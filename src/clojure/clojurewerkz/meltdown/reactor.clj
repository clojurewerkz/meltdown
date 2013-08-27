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
           java.lang.Throwable))

(defn environment
  []
  (Environment.))

(def dispatcher-types
  {:event-loop "eventLoop"
   :thread-pool "threadPoolExecutor"
   :ring-buffer "ringBuffer"})

(defn ^EventRouter make-router
  []
  (ConsumerFilteringEventRouter. (PassThroughFilter.) (ArgumentConvertingConsumerInvoker. nil)))

(defn make-sync-dispatcher
  []
  (SynchronousDispatcher.))

(defn make-caching-registory
  []
  (CachingRegistry.))

(defprotocol IReactor
  (on [reactor ^Selector selector ^IFn consumer] [reactor ^IFn consumer])
  (notify [_ key event on-complete] [_ key event] [_ event])
  (send-event [self key e callback])
  (receive-event [self selector f] "Same as notify, except it checks for reply-to and sends a resulting callback. Used for optimization, since checking for
   reply-to is a (relatively) expensive operation"))

(defn dispatch
  [^Dispatcher d ^Object key ^Event event ^Registry registry
   ^Consumer error-consumer ^EventRouter router ^Consumer completition-consumer]
  (.dispatch d key event registry error-consumer router  completition-consumer))

(defn- error-handler*
  [r]
  (mc/from-fn
   (fn [^Throwable t]
     (dispatch (.dispatcher r) (.getClass t) (Event/wrap t) (.consumer-registry r) nil (.event-router r) nil))))

(def ^:private error-handler (memoize error-handler*))

(defn maybe-wrap-event
  [ev]
  (if (= Event (type ev))
    ev
    (Event. ev)))

(deftype Reactor [^Dispatcher dispatcher event-router ^Registry consumer-registry]

  IReactor
  (on [_ selector consumer]
    (.register consumer-registry selector (mc/from-fn consumer)))

  (on [_ consumer]
    (.register consumer-registry (mc/from-fn consumer)))

  (notify [this key event on-complete]
    (assert (not (nil? key)) "Key can't be nil")
    (assert (not (nil? key)) "Event can't be nil")

    (.dispatch dispatcher key (maybe-wrap-event event) consumer-registry (error-handler this) event-router on-complete))

  (notify [this key event]
    (notify this key event nil))

  (notify [_ event]

    )

  (send-event [self key event callback]
    (let [e (maybe-wrap-event event)
          [reply-to-selector reply-to-key] (msel/$)]
      (.setReplyTo e reply-to-key)
      (on self reply-to-selector
          (fn [response]
            (callback response)
            (.unregister consumer-registry reply-to-key)))
      (notify self key e)))

  (receive-event [self selector f]
    (on self selector (fn [event]
                        (notify self (:reply-to event) (f event))))))


;; Router is made for each Reactor, since otherwise reactors _will share_
(defn create
  ""
  []
  (Reactor. (make-sync-dispatcher) (make-router) (make-caching-registory))
  )
