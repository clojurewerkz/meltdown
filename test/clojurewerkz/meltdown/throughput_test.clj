(ns clojurewerkz.meltdown.reactor-test
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
  (let [selectors 250
        iterations 1000
        test-runs 3
        objects (take (* selectors iterations) (gen-objects))
        latch (CountDownLatch. (* selectors iterations))
        start (System/currentTimeMillis)]
    (dotimes [tr test-runs]
      (dotimes [i (* selectors iterations)]
        (mr/notify reactor (get objects (mod i selectors)))))
    (let [end (System/currentTimeMillis)
          elapsed (- end start)]
      (println
       (str
        (-> reactor
            (.getDispatcher)
            (.getClass)
            (.getSimpleName))
        " dispatched total of " (count objects) " messages, "
        "throughput (" elapsed "ms): " (Math/round (float (/ (* selectors iterations) (/ elapsed 1000)))))))))

(deftest dispatcher-throughput-test
  (testing "Event Loop"
    (throughput-test (mr/create :dispatcher-type :event-loop)))
  (testing "Thread Pool Executor"
    (throughput-test (mr/create :dispatcher-type :thread-pool)))
  (testing "Ring Buffer"
    (throughput-test (mr/create :dispatcher-type :ring-buffer))))
