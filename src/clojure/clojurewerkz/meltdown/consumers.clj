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

(ns clojurewerkz.meltdown.consumers
  "Operations on consumers and registrations"
  (:require [clojurewerkz.meltdown.events :as ev])
  (:import [reactor.function Consumer]
           [reactor.event.registry Registration]
           clojure.lang.IFn))

(defn ^Consumer from-fn
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (reify Consumer
    (accept [this event]
      (f (ev/event->map event)))))


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

(defn ^boolean cancell-after-use?
  [^Registration reg]
  (.isCancellAfterUse reg))

(defn ^Registration cancel-after-use
  [^Registration reg]
  (.cancelAfterUse reg))
