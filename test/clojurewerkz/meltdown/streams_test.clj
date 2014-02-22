(ns clojurewerkz.meltdown.streams-test
  (:refer-clojure :exclude [flush])
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.streams :refer :all :as ms]))

(alter-var-root #'*out* (constantly *out*))

(deftest basic-stream-map-test
  (let [channel (create)
        stream  (map* inc channel)
        stream2 (map* #(+ 2 %) stream)
        stream3 (map* #(+ 3 %) stream2)

        res     (atom {})]

    (consume stream (fn [v] (swap! res assoc :first v)))
    (consume stream2 (fn [v] (swap! res assoc :second v)))
    (consume stream3 (fn [v] (swap! res assoc :third v)))

    (accept channel 1)

    (let [d @res]
      (is (= 2 (:first d)))
      (is (= 4 (:second d)))
      (is (= 7 (:third d))))))


(deftest basic-stream-filter-test
  (let [channel (create)
        even    (filter* even? channel)
        odd     (filter* odd? channel)
        res     (atom {})]

    (consume even (fn [v] (swap! res assoc :even v)))
    (consume odd (fn [v] (swap! res assoc :odd v)))

    (accept channel 1)
    (accept channel 2)

    (let [d @res]
      (is (= 1 (:odd d)))
      (is (= 2 (:even d))))))


(deftest batch-test
  (let [channel     (create)
        incrementer (map* inc channel)
        batcher     (batch* 3 incrementer)
        res         (atom nil)]

    (consume batcher (fn [i] (reset! res i)))

    (accept channel 1)
    (is (nil? @res))
    (accept channel 1)
    (accept channel 1)

    (is (= [2 2 2] @res))))


(deftest basic-stream-reduce-test
  (let [channel (create)
        stream  (reduce* #(+ %1 %2) 0 channel)
        res     (atom nil)]

    (consume stream #(reset! res %))

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (ms/flush channel)

    (is (= 6 @res))))

(deftest custom-stream-test
  (let [channel            (create)
        incrementer        (map* inc channel)
        every-fifth-stream (let [counter (atom 0)]
                             (custom-stream
                              (fn [event downstream]
                                (swap! counter inc)
                                (when (= 5 @counter)
                                  (reset! counter 0)
                                  (accept downstream event)))
                              incrementer))
        res                (atom nil)
        consumer           (consume every-fifth-stream #(reset! res %))]

    (accept channel 1)
    (is (= nil @res))
    (accept channel 2)
    (is (= nil @res))
    (accept channel 3)
    (is (= nil @res))
    (accept channel 4)
    (is (= nil @res))
    (accept channel 5)
    (is (= 6 @res))))


(deftest basic-identity-test
  (let [channel (create)
        stream  (map* identity channel)
        res (atom nil)]

    (consume stream (fn [v] (reset! res v)))
    (accept channel [1 2 3])
    (let [d @res]
      (is (= [1 2 3] d)))))
