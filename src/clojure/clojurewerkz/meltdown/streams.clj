(ns clojurewerkz.meltdown.streams
  (:import [reactor.core.composable.spec Streams]
           [reactor.core.composable Stream Deferred]
           [reactor.function Function Predicate]
           [reactor.tuple Tuple2]
           clojure.lang.IFn)
  (:require [clojurewerkz.meltdown.consumers :as mc]))

(def dispatcher-types
  {:event-loop "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})

(defn ^Function fn->function
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (reify Function
    (apply [this a]
      (f a))))

(defn ^Predicate fn->predicate
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (proxy [Predicate] []
    (test [a]
      (f a))))

(defn accept
  [^Stream stream value]
  (.accept stream value))

(defn- maybe-compose
  [deferred-or-stream]
  (if (instance? Deferred deferred-or-stream)
    (.compose deferred-or-stream)
    deferred-or-stream))

(defn map*
  [f deferred-or-stream]
  (.map (maybe-compose deferred-or-stream) (fn->function f)))

(defn filter*
  [f deferred-or-stream]
  (.filter (maybe-compose deferred-or-stream) (fn->predicate f)))

(defn batch*
  [i deferred-or-stream]
  (map* #(into [] %)
        (.collect (.batch (maybe-compose deferred-or-stream) i))))

(defn reduce*
  [f deferred-or-stream]
  (.reduce (maybe-compose deferred-or-stream) (fn->function
                                               (fn [^Tuple2 tuple]
                                                 (let [value (.getT1 tuple)
                                                       acc (.getT2 tuple)]
                                                   (f acc value))))))

(defn consume
  [^Stream stream f]
  (.consume stream
            (mc/from-fn-raw f)))

(defn ^Deferred create
  [& {:keys [dispatcher-type values batch-size]}]
  (let [spec (Streams/defer)]
    (if dispatcher-type
      (.dispatcher spec (dispatcher-type dispatcher-types))
      (.synchronousDispatcher spec))
    (when values
      (.each spec values))
    (when batch-size
      (.batchSize spec batch-size))
    (.get spec)))
