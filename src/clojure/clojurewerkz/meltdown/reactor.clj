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
  (:import [reactor.core R Reactor]
           [reactor.fn Selector Consumer Event]
           clojure.lang.IFn))


(defn ^Reactor create
  "Creates a reactor instance"
  []
  (R/create))

(defn on
  "Registers a Clojure function as event handler for a particular kind of events.

   1-arity will register a handler for all events on the root (default) reactor.
   2-arity takes a selector and a handler and will use the root reactor.
   3-arity takes a reactor instance, a selector and a handler."
  ([^IFn f]
     (R/on (mc/from-fn f)))
  ([^Selector selector ^IFn f]
     (R/on selector (mc/from-fn f)))
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (mc/from-fn f))))

(defn on-any
  "Registers a Clojure function as event handler for all events
   using default selector."
  ([^IFn f]
     (R/on (mc/from-fn f)))
  ([^Reactor reactor ^IFn f]
     (.on reactor (mc/from-fn f))))

;; TODO: error handlers

(defn notify
  ([payload]
     (R/notify (Event. payload)))
  ([key payload]
     (R/notify ^Object key (Event. payload)))
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key (Event. payload)))
  ([^Reactor reactor key payload ^IFn completion-fn]
     (.notify reactor ^Object key (Event. payload) ^Consumer (mc/from-fn completion-fn))))
