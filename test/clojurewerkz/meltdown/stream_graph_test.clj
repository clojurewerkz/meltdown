(ns clojurewerkz.meltdown.stream-graph-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.streams :as ms]
            [clojurewerkz.meltdown.stream-graph :refer :all]
            [clojurewerkz.meltdown.env :as me]))

(alter-var-root #'*out* (constantly *out*))

(def env (me/environment))

(deftest basic-stream-map-test
  (let [res (atom {})
        swapper (fn [k] #(swap! res assoc k %))
        summarizer (fn [i] #(+ i %))
        ch (graph (create :env env)
                  (map* inc
                        (consume (swapper :first))
                        (map* (summarizer 2)
                              (consume (swapper :second))
                              (map* (summarizer 3)
                                    (consume (swapper :third))))))]

    (accept ch 1)

    (let [d @res]
      (is (= 2 (:first d)))
      (is (= 4 (:second d)))
      (is (= 7 (:third d))))))

(deftest basic-stream-map-reduce-test
  (let [res (atom nil)
        ch (graph (create :env env)
                  (map* inc
                        (reduce* + 0
                                 (consume (fn [s]
                                            (reset! res s))))))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (ms/flush ch)

    (let [d @res]
      (is (= 9 d)))))

(deftest basic-stream-map-filter-reduce-test
  (let [res (atom nil)
        summarizer #(+ %1 %2)
        ch (graph (create :env env)
                  (map* inc
                        (filter* even?
                                 (reduce* + 0
                                          (consume #(reset! res %))))))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)
    (ms/flush ch)

    (let [d @res]
      (is (= 6 d)))))


(deftest basic-stream-map-filter-reduce-test-2
  (let [res1 (atom nil)
        res2 (atom nil)
        summarizer #(+ %1 %2)
        ch (graph (create :env env)
                  (map* inc
                        (filter* even?
                                 (reduce* + 0
                                          (consume #(reset! res1 %)))))
                  (filter* even?
                           (reduce* * 1
                                    (consume #(reset! res2 %)))))]
    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)
    (ms/flush ch)
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
                                  (reduce* * 1
                                           (consume #(reset! res1 %))))))

        detached2 (detach
                   (filter* odd?
                            (reduce* + 0
                                     (consume #(reset! res2 %)))))
        summarizer #(+ %1 %2)
        ch (graph (create :env env)
                  (attach detached1)
                  (attach detached2))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)
    (ms/flush ch)

    (let [d1 @res1
          d2 @res2]
      (is (= 8 d1))
      (is (= 4 d2)))))


(deftest subdetached-test
  (let [res1 (atom nil)
        res2 (atom nil)
        detached-filter (detach
                         (filter* even?
                                  (reduce* * 2
                                           (consume #(reset! res1 %)))))
        detached1 (detach
                   (map* inc
                         (attach detached-filter)))

        detached2 (detach
                   (filter* odd?
                            (reduce* + 1
                                     (consume #(reset! res2 %)))))
        summarizer #(+ %1 %2)
        ch (graph (create :env env)
                  (attach detached1)
                  (attach detached2))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)
    (ms/flush ch)

    (let [d1 @res1
          d2 @res2]
      (is (= 16 d1))
      (is (= 5 d2)))))


(deftest attached-doseq-trick-test
  (let [res (atom {})
        summarizer +
        ch (graph (create :env env)
                  (dotimes [i 5]
                    (map* #(swap! res assoc i %))))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)

    (let [d @res]
      (dotimes [i 5]
        (is (= 4 (get d i)))))))

(deftest basic-map-batch-test
  (let [res (atom nil)
        swapper (fn [k] #(swap! res assoc k %))
        ch (graph (create :env env)
                  (map* inc
                        (batch* 5
                                (consume #(reset! res %)))))]

    (accept ch 1)
    (accept ch 2)
    (accept ch 3)
    (accept ch 4)
    (accept ch 5)

    (let [d @res]
      (is (= [2 3 4 5 6] d)))))


(deftest mappend-fmap-test
  (testing "mapend with fmap on vector monoid"
    (let [res (atom nil)
          ch (graph (create :env env)
                    (mappend* [] #(= 5 (count %))
                              (fmap* inc
                                     (consume #(reset! res %)))))]

      (accept ch 1)
      (accept ch 2)
      (accept ch 3)
      (accept ch 4)
      (accept ch 5)

      (let [d @res]
        (is (= [2 3 4 5 6] d)))))

  (testing "mapend with fmap on map monoid"
    (let [res (atom nil)
          ch (graph (create :env env)
                    (map* (fn [i] [(if (even? i)
                                    :even
                                    :odd)
                                  i])
                          (mappend* {}
                                    #(= 5 (last (:odd %)))
                                    (fmap* (comp inc inc)
                                           (consume #(reset! res %))))))]

      (accept ch 1)
      (accept ch 2)
      (accept ch 3)
      (accept ch 4)
      (accept ch 5)

      (let [d @res]
        (is (= {:odd [3 5 7], :even [4 6]} d))))))

(deftest mappend-fmap-fold-test
  (testing "mapend and fold on vector monoid"
    (let [res (atom nil)
          ch (graph (create :env env)
                    (mappend* [] #(= 5 (count %))
                              (fold* +
                                     (consume #(reset! res %)))))]

      (accept ch 1)
      (accept ch 2)
      (accept ch 3)
      (accept ch 4)
      (accept ch 5)

      (let [d @res]
        (is (= 15 d)))))

  (testing "mapend and fold on map monoid"
    (let [res (atom nil)
          ch  (graph (create :env env)
                     (map* (fn [i] [(if (even? i)
                                     :even
                                     :odd)
                                   i])
                           (mappend* {}
                                     #(= 5 (last (:odd %)))
                                     (consume println)
                                     (fmap* inc
                                            (fold* +
                                                   (consume #(reset! res %)))))))]

      (accept ch 1)
      (accept ch 2)
      (accept ch 3)
      (accept ch 4)
      (accept ch 5)

      (let [d @res]
        (is (= {:odd 12, :even 8} d))))))
