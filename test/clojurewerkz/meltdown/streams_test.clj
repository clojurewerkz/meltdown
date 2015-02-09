(ns clojurewerkz.meltdown.streams-test
  (:refer-clojure :exclude [flush])
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.streams :refer :all :as ms]
            [clojurewerkz.meltdown.env :as me]))

(alter-var-root #'*out* (constantly *out*))

(def env (me/environment))

(deftest basic-stream-map-test
  (let [ch (create :env env)
        stream  (map* inc ch)
        stream2 (map* #(+ 2 %) stream)
        stream3 (map* #(+ 3 %) stream2)

        res     (atom {})]

    (consume stream (fn [v] (swap! res assoc :first v)))
    (consume stream2 (fn [v] (swap! res assoc :second v)))
    (consume stream3 (fn [v] (swap! res assoc :third v)))

    (accept ch 1)

    (let [d @res]
      (is (= 2 (:first d)))
      (is (= 4 (:second d)))
      (is (= 7 (:third d))))))


(deftest basic-stream-filter-test
  (let [ch (create :env env)
        even    (filter* even? ch)
        odd     (filter* odd? ch)
        res     (atom {})]

    (consume even (fn [v] (swap! res assoc :even v)))
    (consume odd (fn [v] (swap! res assoc :odd v)))

    (accept ch 1)
    (accept ch 2)

    (let [d @res]
      (is (= 1 (:odd d)))
      (is (= 2 (:even d))))))


(deftest batch-test
  (let [ch     (create :env env)
        incrementer (map* inc ch)
        batcher     (batch* 3 incrementer)
        res         (atom nil)]

    (consume batcher (fn [i] (reset! res i)))

    (accept ch 1)
    (is (nil? @res))
    (accept ch 1)
    (accept ch 1)

    (is (= [2 2 2] @res))))


(deftest basic-stream-reduce-test
  (let [ch     (create :env env)
        stream (reduce* #(+ %1 %2) 0 ch)
        res    (atom nil)]

    (consume stream #(reset! res %))

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (ms/flush ch)

    (is (= 6 @res))))

(deftest custom-stream-test
  (let [ch                 (create :env env)
        incrementer        (map* inc ch)
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

    (accept ch 1)
    (is (= nil @res))
    (accept ch 2)
    (is (= nil @res))
    (accept ch 3)
    (is (= nil @res))
    (accept ch 4)
    (is (= nil @res))
    (accept ch 5)
    (is (= 6 @res))))


(deftest basic-identity-test
  (let [ch (create :env env)
        stream  (map* identity ch)
        res (atom nil)]

    (consume stream (fn [v] (reset! res v)))
    (accept ch [1 2 3])
    (let [d @res]
      (is (= [1 2 3] d)))))
