(ns clojurewerkz.meltdown.throughput-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc])
  (:import [java.util.concurrent CountDownLatch TimeUnit]))

(defn register-consumers-and-warm-cache
  [reactor objects consumer]
  (doseq [o objects]
    (mr/on reactor ($ o) consumer))

  ;; pre-select everything to ensure it's in the cache
  (doseq [o objects]
    (.select (.getConsumerRegistry reactor) o)))

(defn gen-objects
  ([]
     (gen-objects [] 0))
  ([c i]
     (lazy-cat c (gen-objects [(str "test" i)] (inc i)))))

(defn throughput-test
  [reactor]
  (let [selectors  250
        iterations 7500
        test-runs  3
        objects    (vec (take selectors (gen-objects)))
        latch      (CountDownLatch. (* test-runs selectors iterations))
        consumer   (fn [event] (.countDown latch))]
    (time
     (register-consumers-and-warm-cache reactor objects consumer))
    (dotimes [tr test-runs]
      (let [start (System/currentTimeMillis)]
        (dotimes [i (* selectors iterations)]
          (mr/notify reactor (get objects (mod i selectors)) "Hello World!"))
        (let [end (System/currentTimeMillis)
              elapsed (- end start)]
          (println
           (str
            (-> reactor
                (.getDispatcher)
                (.getClass)
                (.getSimpleName))
            "throughput (" elapsed "ms): " (Math/round (float (/ (* selectors iterations) (/ elapsed 1000)))))))))))

(deftest ^:performance dispatcher-throughput-test
  (testing "Event Loop"
    (throughput-test (mr/create :dispatcher-type :event-loop)))
  (testing "Thread Pool Executor"
    (throughput-test (mr/create :dispatcher-type :thread-pool)))
  (testing "Ring Buffer"
    (throughput-test (mr/create :dispatcher-type :ring-buffer))))
