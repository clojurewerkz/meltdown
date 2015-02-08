;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns clojurewerkz.meltdown.streams
  (:refer-clojure :exclude [flush])
  (:require [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.fn        :as mfn]
            [clojurewerkz.meltdown.env       :as me]
            [clojurewerkz.meltdown.types     :refer :all])
  (:import
           [reactor.core.composable.spec    Streams]
           [reactor.core.composable         Stream Deferred]
           [reactor.core.composable.spec    DeferredStreamSpec]
           [reactor.event.dispatch          Dispatcher]
           [reactor.core                    Environment]
           [reactor.function.support        Tap]
           [reactor.tuple                   Tuple2]
           [clojure.lang                    IFn]))


(def dispatcher-types
  {:event-loop  "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})


(defn ^Deferred create
  "Creates a stream processing channel"
  [& {:keys [dispatcher-type ^Dispatcher dispatcher values batch-size ^Environment env]}]
  (let [^DeferredStreamSpec spec (if values
                                   (Streams/defer values)
                                   (Streams/defer))]
    (if env
      (.env spec env)
      (.env spec (me/environment)))
    (if dispatcher-type
      (.dispatcher spec ^String (dispatcher-type dispatcher-types))
      (.synchronousDispatcher spec))
    (when dispatcher
      (.dispatcher spec dispatcher))
    (when batch-size
      (.batchSize spec batch-size))
    (.get spec)))

(defn accept
  [^Deferred deferred value]
  (.accept deferred value))

(defn ^Tap tap
  [^Stream stream]
  (.tap stream))

(defn- ^Stream maybe-compose
  [deferred-or-stream]
  (if (instance? Deferred deferred-or-stream)
    (let [deferred ^Deferred deferred-or-stream]
      (.compose deferred))
    deferred-or-stream))

(defn ^Stream flush
  [deferred-or-stream]
  (.flush ^Stream (maybe-compose deferred-or-stream)))

(defn map*
  "Defines a map function, that will apply `f` to all events going through it."
  [^IFn f deferred-or-stream]
  (let [stream (maybe-compose deferred-or-stream)]
    (.map stream (mfn/->function f))))

(defn filter*
  "Defines a filter function, that will apply predicate `f` to all events going through it
   and will stream only those for which predicate returned truthy value."
  [^IFn f deferred-or-stream]
  (let [stream (maybe-compose deferred-or-stream)]
    (.filter stream (mfn/->predicate f))))

(defn batch*
  "Defines a batch function that will accumulate values until it's reaches count of `i`, and
   streams the resulting array of values further down."
  [i deferred-or-stream]
  (map* #(into [] %)
        (.collect (maybe-compose deferred-or-stream) i)))

(defn reduce*
  "Defines an aggregator funciton that will apply previous aggregator value and new incoming event
   to function `f`, receives `default-value` with which aggregator is initialized."
  ([^IFn f default-value deferred-or-stream]
     (.reduce (maybe-compose deferred-or-stream) (mfn/->function
                                                   (fn [^Tuple2 tuple]
                                                     (let [value (.getT1 tuple)
                                                           acc (or (.getT2 tuple) default-value)]
                                                       (f acc value))))))
  ([f deferred-or-stream]
     (reduce* f nil deferred-or-stream)))

(defn consume
  "Defines a consumer for stream."
  [^Stream stream ^IFn f]
  (.consume stream
            (mc/from-fn-raw f)))

(defn custom-stream
  [^IFn f deferred-or-stream]
  (let [downstream (create)]
    (consume (maybe-compose deferred-or-stream)
             #(f % downstream))
    (.compose downstream)))

(defn mappend*
  ([monoid deferred-or-stream]
     (mappend* monoid (constantly true) deferred-or-stream))
  ([monoid condition deferred-or-stream]
     (let [state (atom (mempty monoid))]
       (custom-stream
        (fn [event downstream]
          (let [new-v (swap! state mappend event)]
            (when (condition new-v)
              (accept downstream new-v)
              (reset! state (mempty monoid)))))
        deferred-or-stream))))

(defn fmap*
  [f deferred-or-stream]
  (let [stream (maybe-compose deferred-or-stream)]
    (.map stream (mfn/->function #(fmap % f)))))

(defn fold*
  [f deferred-or-stream]
  (let [stream (maybe-compose deferred-or-stream)]
    (.map stream (mfn/->function #(fold % f)))))
