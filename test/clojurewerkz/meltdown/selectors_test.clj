(ns clojurewerkz.meltdown.selectors-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

(deftest test-predicate-selector
  (let [r     (mr/create)
        latch (CountDownLatch. 100)
        xs    (atom [])
        sel   (ms/predicate even?)]
    (mr/on r sel (fn [evt]
                   (swap! xs conj (:data evt))
                   (.countDown latch)))
    (doseq [i (range 100 200)]
      (mr/notify r i i))
    (.await latch 1 TimeUnit/SECONDS)
    (is (= (vec (sort @xs))
           (vec (range 100 200 2))))))

(deftest test-match-all-selector
  (let [r     (mr/create)
        latch (CountDownLatch. 100)
        xs    (atom [])
        sel   (ms/match-all)
        rng   (range 100 200)]
    (mr/on r sel (fn [evt]
                   (swap! xs conj (:data evt))
                   (.countDown latch)))
    (doseq [i rng]
      (mr/notify r i i))
    (.await latch 1 TimeUnit/SECONDS)
    (is (= (vec (sort @xs))
           (vec rng)))))
