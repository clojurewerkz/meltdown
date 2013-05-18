(ns clojurewerkz.meltdown.reactor-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))


(deftest test-basic-delivery-over-non-root-reactor
  (let [latch (CountDownLatch. 1)
        r     (mr/create)
        data  {:event "delivered"}
        res   (atom nil)]
    (mr/on r ($ "events.silly") (fn [event]
                                  (reset! res event)
                                  (.countDown latch)))
    (mr/notify r "events.silly" data)
    (.await latch 5 TimeUnit/SECONDS)
    (let [d @res]
      (is (:id d))
      (is (= {} (:headers d)))
      (is (= "delivered" (get-in d [:data :event]))))))

(deftest test-basic-delivery-over-root-reactor
  (let [latch (CountDownLatch. 1)
        data  {:event "delivered"}
        res   (atom nil)]
    (mr/on ($ "events.silly") (fn [event]
                                (reset! res event)
                                (.countDown latch)))
    (mr/notify "events.silly" data)
    (.await latch 3 TimeUnit/SECONDS)
    (let [d @res]
      (is (:id d))
      (is (= {} (:headers d)))
      (is (= "delivered" (get-in d [:data :event]))))))
