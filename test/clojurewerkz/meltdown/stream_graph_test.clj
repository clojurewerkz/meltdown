(ns clojurewerkz.meltdown.stream-graph-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.streams :as ms]
            [clojurewerkz.meltdown.stream-graph :refer :all]))

(alter-var-root #'*out* (constantly *out*))

(deftest basic-stream-map-test
  (let [res (atom {})
        swapper (fn [k] #(swap! res assoc k %))
        summarizer (fn [i] #(+ i %))
        channel (graph (create)
                       (map* inc
                             (consume (swapper :first))
                             (map* (summarizer 2)
                                   (consume (swapper :second))
                                   (map* (summarizer 3)
                                         (consume (swapper :third))))))]

    (accept channel 1)

    (let [d @res]
      (is (= 2 (:first d)))
      (is (= 4 (:second d)))
      (is (= 7 (:third d))))))

(deftest basic-stream-map-reduce-test
  (let [res (atom nil)
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (map* inc
                             (reduce* #(+ %1 %2) 0
                                      (consume #(reset! res %)))))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)

    (let [d @res]
      (is (= 9 d)))))

(deftest basic-stream-map-filter-reduce-test
  (let [res (atom nil)
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (map* inc
                             (filter* even?
                                      (reduce* #(+ %1 %2) 0
                                               (consume #(reset! res %))))))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)

    (let [d @res]
      (is (= 6 d)))))


(deftest basic-stream-map-filter-reduce-test
  (let [res1 (atom nil)
        res2 (atom nil)
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (map* inc
                             (filter* even?
                                      (reduce* #(+ %1 %2) 0
                                               (consume #(reset! res1 %)))))
                       (filter* even?
                                (reduce* #(* %1 %2) 1
                                         (consume #(reset! res2 %)))))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)

    (let [d1 @res1
          d2 @res2]
      (is (= 6 d1))
      (is (= 8 d2)))))


(deftest basic-detached-test
  (let [res1 (atom nil)
        res2 (atom nil)
        detached1 (detach
                   (map* inc
                         (filter* even?
                                  (reduce* #(* %1 %2) 1
                                           (consume #(reset! res1 %))))))

        detached2 (detach
                   (filter* odd?
                            (reduce* #(+ %1 %2) 0
                                     (consume #(reset! res2 %)))))
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (attach detached1)
                       (attach detached2))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)

    (let [d1 @res1
          d2 @res2]
      (is (= 8 d1))
      (is (= 4 d2)))))


(deftest subdetached-test
  (let [res1 (atom nil)
        res2 (atom nil)
        detached-filter (detach
                         (filter* even?
                                  (reduce* #(* %1 %2) 2
                                           (consume #(reset! res1 %)))))
        detached1 (detach
                   (map* inc
                         (attach detached-filter)))

        detached2 (detach
                   (filter* odd?
                            (reduce* #(+ %1 %2) 1
                                     (consume #(reset! res2 %)))))
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (attach detached1)
                       (attach detached2))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)

    (let [d1 @res1
          d2 @res2]
      (is (= 16 d1))
      (is (= 5 d2)))))


(deftest attached-doseq-trick-test
  (let [res (atom {})
        summarizer #(+ %1 %2)
        channel (graph (create)
                       (dotimes [i 5]
                         (map* #(swap! res assoc i %))))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)

    (let [d @res]
      (dotimes [i 5]
        (is (= 4 (get d i)))))))

(deftest basic-map-batch-test
  (let [res (atom nil)
        swapper (fn [k] #(swap! res assoc k %))
        channel (graph (create)
                       (map* inc
                             (batch* 5
                                     (consume #(reset! res %)))))]

    (accept channel 1)
    (accept channel 2)
    (accept channel 3)
    (accept channel 4)
    (accept channel 5)

    (let [d @res]
      (is (= [2 3 4 5 6] d)))))
