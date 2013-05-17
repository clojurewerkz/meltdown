(ns clojurewerkz.meltdown.reactor-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))


(deftest test-basic-delivery-over-non-root-reactor
  (let [latch (CountDownLatch. 1)
        r     (mr/create)
        sel   ($ "events.silly")
        key   "events.silly"
        data  {:event "delivered"}
        res   (atom nil)]
    (mr/on r sel (fn [event]
                   (reset! res event)
                   (.countDown latch)))
    (.start (Thread. (fn []
                       (Thread/sleep 100)
                       (mr/notify r key data))))
    (.await latch 5 TimeUnit/SECONDS)
    (let [d @res]
      (is (:id d))
      (is (= {} (:headers d)))
      (is (= "delivered" (get-in d [:data :event]))))))
