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
         ~'latch latch#]
     ~@body
     (.await latch# 1 TimeUnit/SECONDS)
     (is (= 0 (.getCount latch#)))))

(deftest test-basic-delivery-over-non-root-reactor
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
    (let [r              (mr/create)
          key            "hello"
          selector       ($ key)
          [reply-to-selector reply-to-key] ($)
          res            (atom nil)]

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
