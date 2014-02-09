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

(ns clojurewerkz.meltdown.reactor
  "Provides key reactor and message passing operations:

    * Reactor instantiation
    * Registration (subscription) for events
    * Event notifications"
  (:require [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.events :as ev]
            [clojurewerkz.meltdown.selectors :as msel])
  (:import [reactor.event.routing EventRouter ConsumerFilteringEventRouter
            ArgumentConvertingConsumerInvoker]
           [reactor.event.selector Selector Selectors]
           [reactor.event.registry Registry CachingRegistry]
           [reactor.filter PassThroughFilter]
           [reactor.core Environment]
           [reactor.function Consumer]
           [reactor.event.dispatch Dispatcher SynchronousDispatcher]
           [clojure.lang IFn]
           [reactor.event Event]
           java.lang.Throwable
           [reactor.core Reactor]
           [reactor.core.spec Reactors]))

(defn environment
  []
  (Environment.))

(def dispatcher-types
  {:event-loop "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})

(defn maybe-wrap-event
  "Coerces given into to an Event instance"
  [ev]
  (if (instance? Event ev)
    ev
    (Event. ev)))

(defn on
  "Registers a Clojure function as event handler for a particular kind of events."
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (mc/from-fn f)))
  ([^Reactor reactor ^IFn f]
     (.on reactor (mc/from-fn f))))

(defn on-error
  "Registers a Clojure function as event handler for a class of Exception."
  [^Reactor reactor exception-type ^IFn f]
  (.on reactor (msel/T exception-type) (mc/from-fn f)))

(defn register-consumer
  "Registers a Clojure function as event handler for a particular kind of events."
  ([^Reactor reactor ^Selector selector ^Consumer consumer]
     (.on reactor selector consumer))
  ([^Reactor reactor ^Consumer consumer]
     (.on reactor consumer)))

(defn notify
  "Broadcasts a event instantiated from provided payload (data structure)"
  ([^Reactor reactor payload]
     (.notify reactor (Event. payload)))
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key ^Event (Event. payload) nil))
  ([^Reactor reactor key payload ^IFn completion-fn]
     (.notify reactor ^Object key (Event. payload) ^Consumer (mc/from-fn completion-fn))))

(defn notify-raw
  [^Reactor reactor ^Object key ^Event payload]
  (.notify reactor ^Object key ^Event payload nil))

(defn send-event
  [^Reactor reactor key event callback]
  (let [e (Event. event)
        [reply-to-selector reply-to-key] (msel/$)]
    (.setReplyTo e reply-to-key)
    (on reactor reply-to-selector
        (fn [response]
          (callback response)
          (.unregister (.getConsumerRegistry reactor) reply-to-key)))
    (notify-raw reactor key e)))

(defn receive-event
  "Same as notify, except it checks for reply-to and sends a resulting callback. Used for optimization, since checking for
   reply-to is a (relatively) expensive operation"
  [^Reactor reactor selector ^IFn f]
  (.on reactor selector (mc/from-fn-raw
                         (fn [^Event e]
                           (notify reactor (.getReplyTo e) (f (dissoc (ev/event->map e) :reply-to :id)))))))

(defn ^Reactor create
  "Creates a reactor instance.

   A new router is instantiated for every reactor,
   otherwise reactors will needlessly share state"
  [& {:keys [^DeferredStreamSpec dispatcher-type event-routing-strategy ^Environment env ^Dispatcher dispatcher]}]
  (let [spec (Reactors/reactor)]
    (if env
      (.env spec env)
      (.env spec (environment)))
    (if dispatcher
      (.dispatcher spec dispatcher)
      (if dispatcher-type
        (.dispatcher spec ^String (dispatcher-type dispatcher-types))
        (.synchronousDispatcher spec)))
    (when event-routing-strategy
      (when (= :first event-routing-strategy)
        (.firstEventRouting spec))
      (when (= :round-robin event-routing-strategy)
        (.roundRobinEventRouting spec))
      (when (= :broadcast event-routing-strategy)
        (.broadcastEventRouting spec)))
    (.get spec)))

(defn responds-to?
  "Returns true if provded reactor has consumers registered for a particular key"
  [^Reactor reactor key]
  (.respondsToKey reactor key))
