(ns clojurewerkz.meltdown.reactor-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$ R]]
            [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.events :as me])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

(defmacro with-latch
  [countdown-from & body]
  `(let [latch# (CountDownLatch. ~countdown-from)
         ;; intentionally unhygienic, expected by @body
         ~'latch latch#]
     ~@body
     (is (.await latch# 2 TimeUnit/SECONDS))
     (is (= 0 (.getCount latch#)))))

(deftest test-basic-delivery
  (with-latch 1
    (let [r     (mr/create)
          key   "events.silly"
          data  {:event "delivered"}
          res   (atom nil)]
      (mr/on r ($ key) (fn [event]
                         (reset! res event)
                         (.countDown latch)))
      (mr/notify r key data)
      (.await latch 1 TimeUnit/SECONDS)
      (let [d @res]
        (is (= key (:key d)))
        (is (:id d))
        (is (= {} (:headers d)))
        (is (= "delivered" (get-in d [:data :event])))))))

(deftest test-fanout-delivery
  (with-latch 2
    (let [r     (mr/create)
          key   "events.silly"
          data  {:event "delivered"}
          res   (atom [])
          f     (fn [event]
                  (swap! res conj event)
                  (.countDown latch))]
      (mr/on r ($ key) f)
      (mr/on r ($ key) f)
      (mr/notify r key data)
      (.await latch 1 TimeUnit/SECONDS)
      (let [xs @res
            d  (first xs)]
        (is (= (first xs) (second xs)))
        (is (:id d))
        (is (= {} (:headers d)))
        (is (= "delivered" (get-in d [:data :event])))))))

(deftest test-regex-delivery
  (with-latch 3
    (let [r     (mr/create)
          data  {:event "delivered"}
          res   (atom nil)]
      (mr/on r (R "events.*") (fn [event]
                                (reset! res event)
                                (.countDown latch)))

      (mr/notify r "events.one" data)
      (mr/notify r "events.two" data)
      (mr/notify r "events.three" data)

      (.await latch 1 TimeUnit/SECONDS)
      (let [d @res]
        (is (:id d))
        (is (= {} (:headers d)))
        (is (= "delivered" (get-in d [:data :event])))))))


(deftest test-request-response
  (with-latch 2
    (let [r                                (mr/create)
          key                              "hello"
          selector                         ($ key)
          [reply-to-selector reply-to-key] ($)
          res                              (atom nil)]

      (mr/receive-event r selector (fn [_]
                                     (.countDown latch)
                                     "response"))

      (mr/send-event r "hello" "data" (fn [event]
                                        (reset! res event)
                                        (.countDown latch)))

      (.await latch 1 TimeUnit/SECONDS)
      (let [d @res]
        (is (:id d))
        (is (= {} (:headers d)))
        (is (= "response" (get-in d [:data])))))))

(deftest routing-strategies
  (testing "First routing strategy"
    (with-latch 1
      (let [r       (mr/create :event-routing-strategy :first)
            res     (atom nil)
            handler (fn [event] (.countDown latch))]
        (mr/on r ($ "key") handler)
        (mr/on r ($ "key") handler)
        (mr/on r ($ "key") handler)
        (mr/notify r "key" {})

        (.await latch 1 TimeUnit/SECONDS))))

  (testing "Broadcast routing strategy"
    (with-latch 6
      (let [r       (mr/create :event-routing-strategy :broadcast)
            res     (atom nil)
            handler (fn [event] (.countDown latch))]
        (mr/on r ($ "key") handler)
        (mr/on r ($ "key") handler)
        (mr/on r ($ "key") handler)
        (mr/notify r "key" {})
        (.await latch 2 TimeUnit/SECONDS))))

  (testing "Round Robin routing strategy"
    (with-latch 6
      (let [r       (mr/create :event-routing-strategy :round-robin)
            res     (atom nil)
            handler (fn [event] (.countDown latch))]
        (mr/on r ($ "key") (fn [event] (.countDown latch)))
        (mr/on r ($ "key") (fn [event] (dotimes [i 2] (.countDown latch))))
        (mr/on r ($ "key") (fn [event] (dotimes [i 3] (.countDown latch))))

        (mr/notify r "key" {})
        (mr/notify r "key" {})
        (mr/notify r "key" {})
        (.await latch 2 TimeUnit/SECONDS)))))

(deftest test-responds-to
  (let [r     (mr/create)]
    (mr/on r ($ "key") (fn [_] ))

    (is (mr/responds-to? r "key"))
    (is (not (mr/responds-to? r "other")))))

(deftest test-error-listener-reactors
  (with-latch 1
    (let [key "events.silly"
          r   (mr/create)
          e   (atom nil)]
      (mr/on r ($ key) (fn [event]
                         (throw (RuntimeException. "Red Alert!"))))

      (mr/on-error r Exception (fn [event]
                                 (.countDown latch)
                                 (reset! e (:data event))))

      (mr/notify r key {})
      (Thread/sleep 100)

      (is (instance? RuntimeException @e)))))
