;; Copyright (c) 2013 The ClojureWerkz team and contributors.
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

(ns clojurewerkz.meltdown.reactor
  "Provides key reactor and message passing operations:

    * Reactor instantiation
    * Registration (subscription) for events
    * Event notifications"
  (:require [clojurewerkz.meltdown.consumers :as mc])
  (:import [reactor.core.spec Reactors]
           [reactor.core Reactor Environment]
           [reactor.event.dispatch ThreadPoolExecutorDispatcher Dispatcher RingBufferDispatcher]
           [com.lmax.disruptor.dsl ProducerType]
           [com.lmax.disruptor YieldingWaitStrategy]
           [reactor.event.selector Selector]
           [reactor.function Consumer]
           [reactor.event Event]
           clojure.lang.IFn
           clojure.lang.IPersistentMap))

(defn environment
  []
  (Environment.))

(def dispatcher-types
  {:event-loop "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})

(defn ^Reactor create
  "Creates a reactor instance"
  [& {:keys [dispatcher-type dispatcher env]}]
  (let [reactor (Reactors/reactor)]
    (if env
      (.env reactor env)
      (.env reactor (environment)))
    (when dispatcher
      (.dispatcher reactor dispatcher))
    (when dispatcher-type
      (.dispatcher reactor (dispatcher-type dispatcher-types)))
    (.get reactor)))

(defn on
  "Registers a Clojure function as event handler for a particular kind of events."
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (mc/from-fn f)))
  ([^Reactor reactor ^IFn f]
     (.on reactor (mc/from-fn f))))

;; TODO: error handlers

(defn notify
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key (Event. payload)))
  ([^Reactor reactor key payload ^IFn completion-fn]
     (.notify reactor ^Object key (Event. payload) ^Consumer (mc/from-fn completion-fn))))

(defn responds-to?
  #_ ([key]
     (.respondsToKey reactor key))
  ([^Reactor reactor key]
     (.respondsToKey reactor key)))
