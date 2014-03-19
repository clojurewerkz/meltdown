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

(ns clojurewerkz.meltdown.consumers
  "Operations on consumers and registrations"
  (:require [clojurewerkz.meltdown.events :as ev])
  (:import [reactor.function Consumer]
           reactor.core.Reactor
           [reactor.event.registry Registration Registry]
           [clojurewerkz.meltdown IFnConsumer IFnTransformingConsumer]
           clojure.lang.IFn))

(defn ^Consumer from-fn
  "Instantiates a transforming Reactor consumer from a Clojure
   function. The consumer will automatically convert Reactor
   events to Clojure maps."
  [^IFn f]
  (IFnTransformingConsumer. f ev/event->map))

(defn ^Consumer from-fn-raw
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (IFnConsumer. f))

(defn ^boolean paused?
  [^Registration reg]
  (.isPaused reg))

(defn ^Registration pause
  [^Registration reg]
  (.pause reg))

(defn ^Registration resume
  [^Registration reg]
  (.resume reg))


(defn ^boolean cancelled?
  [^Registration reg]
  (.isCancelled reg))

(defn ^Registration cancel
  [^Registration reg]
  (.cancel reg))

(defn ^boolean cancel-after-use?
  [^Registration reg]
  (.isCancelAfterUse reg))

(defn ^Registration cancel-after-use
  [^Registration reg]
  (.cancelAfterUse reg))

(defn ^Registry consumer-registry-of
  [^Reactor r]
  (.getConsumerRegistry r))

(defn consumers-on
  [^Reactor r]
  (let [^Registry xs (consumer-registry-of r)]
    (remove nil? (into [] xs))))

(defn ^long consumer-count
  [^Reactor r]
  (let [^Registry xs (consumer-registry-of r)]
    (count (remove nil? (into [] xs)))))
