(ns clojurewerkz.meltdown.throughput-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor   :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.events    :as me])
  (:import [java.util.concurrent CountDownLatch TimeUnit]
           [reactor.event.dispatch RingBufferDispatcher]
           [reactor.event Event]
           [reactor.core Reactor]
           [com.lmax.disruptor.dsl ProducerType]
           [com.lmax.disruptor YieldingWaitStrategy]))

(defn register-consumers-and-warm-cache
  [^Reactor reactor objects consumer]
  (doseq [o objects]
    (mr/register-consumer reactor ($ o) consumer))

  ;; pre-select everything to ensure it's in the cache
  (doseq [o objects]
    (.select (.getConsumerRegistry reactor) o)))

(defn gen-objects
  ([]
     (gen-objects [] 0))
  ([c i]
     (lazy-cat c (gen-objects [(str "test" i)] (inc i)))))

(declare ^CountDownLatch latch)
(defn throughput-test
  [^Reactor reactor]
  (let [selectors  250
        iterations 7500
        test-runs  3
        _          (def latch (CountDownLatch. (* selectors iterations)))
        objects    (vec (take selectors (gen-objects)))
        consumer   (mc/from-fn-raw (fn [_] (.countDown ^CountDownLatch latch)))
        hello      (me/ev :data "Hello World!")]
    (time
     (register-consumers-and-warm-cache reactor objects consumer))
    (dotimes [tr test-runs]
      (let [start (System/currentTimeMillis)]
        (dotimes [i iterations]
          (doseq [o objects]
            (mr/notify-raw ^Reactor reactor o ^Event hello)))
        (.await latch)
        (let [end (System/currentTimeMillis)
              elapsed (- end start)]
          (println
           (str
            (-> reactor
                (.getDispatcher)
                (.getClass)
                (.getSimpleName))
            " throughput (" elapsed "ms): " (Math/round (float (/ (* selectors iterations) (/ elapsed 1000))))
            "/sec")))
        (def latch (CountDownLatch. (* selectors iterations)))))
    (.shutdown (.getDispatcher reactor))))

(deftest ^:performance dispatcher-throughput-test
  (testing "Event Loop"
    (throughput-test (mr/create :dispatcher-type :event-loop)))
  (testing "Thread Pool Executor"
    (throughput-test (mr/create :dispatcher-type :thread-pool)))
  (testing "Ring Buffer"
    (throughput-test (mr/create :dispatcher-type :ring-buffer)))
  (testing "Ring Buffer"
    (throughput-test (mr/create :dispatcher (RingBufferDispatcher.
                                             "dispatcher-name"
                                             (int 4096)
                                             nil
                                             ProducerType/MULTI
                                             (YieldingWaitStrategy.))))))
