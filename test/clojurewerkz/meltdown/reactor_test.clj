(ns clojurewerkz.meltdown.reactor-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))


(deftest test-basic-delivery-over-non-root-reactor
  (let [latch (CountDownLatch. 1)
        r     (mr/create)
        key   "events.silly"
        data  {:event "delivered"}
        res   (atom nil)]
    (mr/on r ($ key) (fn [event]
                       (reset! res event)
                       (.countDown latch)))
    (is (mr/responds-to? r key))
    (mr/notify r key data)
    (.await latch 5 TimeUnit/SECONDS)
    (let [d @res]
      (is (:id d))
      (is (= {} (:headers d)))
      (is (= "delivered" (get-in d [:data :event]))))))

(deftest test-basic-delivery-over-root-reactor
  (let [latch (CountDownLatch. 1)
        key   "events.silly"
        data  {:event "delivered"}
        res   (atom nil)]
    (mr/on ($ key) (fn [event]
                     (reset! res event)
                     (.countDown latch)))
    #_ (is (mr/responds-to? key))
    (mr/notify key data)
    (.await latch 3 TimeUnit/SECONDS)
    (let [d @res]
      (is (:id d))
      (is (= {} (:headers d)))
      (is (= "delivered" (get-in d [:data :event]))))))

;; TODO
#_ (deftest test-basic-delivery-using-default-selector
     (let [latch (CountDownLatch. 1)
           r     (mr/create)
           data  {:event "delivered"}
           res   (atom nil)]
       (mr/on-any r (fn [event]
                      (println "hello")
                      (reset! res event)
                      (.countDown latch)))
       (mr/notify r "events.silly" data)
       (.await latch 3 TimeUnit/SECONDS)
       (let [d @res]
         (is (:id d))
         (is (= {} (:headers d)))
         (is (= "delivered" (get-in d [:data :event]))))))

(deftest test-pause-on-registrations
  (let [r   (mr/create)
        key "events.silly"
        at  (atom nil)
        reg (mr/on r ($ key) (fn [event]
                               (reset! at event)))]
    (is (mr/responds-to? r key))
    (mc/pause reg)
    (is (mr/responds-to? r key))
    (mr/notify r key "value")
    (is (mc/paused? reg))
    (mr/notify r key "value")
    (is (nil? @at))))

(deftest test-resume-on-registrations
  (let [r   (mr/create)
        key "events.silly"
        at  (atom nil)
        reg (mr/on r ($ key) (fn [event]
                               (reset! at event)))]
    (is (mr/responds-to? r key))
    (mc/pause reg)
    (is (mr/responds-to? r key))
    (mr/notify r key "value")
    (is (mc/paused? reg))
    (mc/resume reg)
    (is (mr/responds-to? r key))
    (mr/notify r key "value")
    (is (not (mc/paused? reg)))
    (mr/notify r key "value")
    (is @at)))

(deftest test-cancel-on-registrations
  (let [r   (mr/create)
        key "events.silly"
        at  (atom nil)
        reg (mr/on r ($ key) (fn [event]
                               (reset! at event)))]
    (is (mr/responds-to? r key))
    (mc/cancel reg)
    (is (not (mr/responds-to? r key)))
    (mr/notify r key "value")
    (is (mc/cancelled? reg))
    (mr/notify r key "value")
    (is (nil? @at))))
