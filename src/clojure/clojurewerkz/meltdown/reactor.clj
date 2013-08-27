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
  (:require [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.events :as ev]
            [clojurewerkz.meltdown.selectors :as msel])
  (:import [reactor.event.routing EventRouter Linkable ConsumerFilteringEventRouter
            ArgumentConvertingConsumerInvoker]
           [reactor.event.selector Selector]
           [reactor.event.registry Registry CachingRegistry]
           [reactor.filter PassThroughFilter]
           [reactor.core Environment]
           [reactor.function Observable Consumer]
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
  [ev]
  (if (= Event (type ev))
    ev
    (Event. ev)))

(defn on
  "Registers a Clojure function as event handler for a particular kind of events."
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (mc/from-fn f)))
  ([^Reactor reactor ^IFn f]
     (.on reactor (mc/from-fn f))))

(defn notify
  ([^Reactor reactor payload]
     (.notify reactor (Event. payload)))
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key (Event. payload)))
  ([^Reactor reactor key payload ^IFn completion-fn]
     (.notify reactor ^Object key (Event. payload) ^Consumer (mc/from-fn completion-fn))))

(defn- notify-raw
  [^Reactor reactor key payload]
  (.notify reactor ^Object key payload))

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
                         (fn [e]
                           (notify reactor (.getReplyTo e) (f (dissoc (ev/event->map e) :reply-to :id)))))))

;; Router is made for each Reactor, since otherwise reactors _will share_
(defn ^Reactor create
  "Creates a reactor instance"
  [& {:keys [dispatcher-type event-routing-strategy env]}]
  (let [spec (Reactors/reactor)]
    (if env
      (.env spec env)
      (.env spec (environment)))
    (if dispatcher-type
      (.dispatcher spec (dispatcher-type dispatcher-types))
      (.synchronousDispatcher spec))
    (when event-routing-strategy
      (when (= :first event-routing-strategy)
        (.firstEventRouting spec))
      (when (= :round-robin event-routing-strategy)
        (.roundRobinEventRouting spec))
      (when (= :broadcast event-routing-strategy)
        (.broadcastEventRouting spec)))
    (.get spec)))

(defn link
  "Link components together"
  [^Reactor r1 ^Reactor r2]
  (.link r1 r2))

(defn unlink
  "Unlink components"
  [^Reactor r1 ^Reactor r2]
  (.unlink r1 r2))

(defn responds-to?
  [^Reactor reactor key]
  (.respondsToKey reactor key))
