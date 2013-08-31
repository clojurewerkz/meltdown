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

(ns clojurewerkz.meltdown.stream-graph
  (:require [clojurewerkz.meltdown.streams :as ms]))

(def create ms/create)
(def accept ms/accept)

(defmacro graph
  "Creates a stream processing graph"
  ([channel] channel)
  ([channel & downstreams]
     `(let [~'upstream ~channel]
        ~@downstreams
        ~'upstream)))

(defmacro detach
  "Detaches given functions from the graph, to be attached later on.
   Mostly used to break down functions into smaller pieces."
  [body]
  `(fn [u#]
     (let [~'upstream u#]
       ~body)))

(defmacro attach
  "Attaches parts back to the graph as if they were declared as a part of graph from the
   beginning."
  [detached] `(~detached ~'upstream))

(defmacro map*
  "Like clojure.core/map but for graph computations"
  ([f] `(ms/map* ~f ~'upstream))
  ([f & downstreams]
     `(let [~'upstream (ms/map* ~f ~'upstream)]
        ~@downstreams
        ~'upstream)))

(defmacro filter*
  "Like clojure.core/filter but for graph computations"
  ([f] `(ms/filter* ~f ~'upstream))
  ([f & downstreams]
     `(let [~'upstream (ms/filter* ~f ~'upstream)]
        ~@downstreams
        ~'upstream)))

(defmacro batch*
  ([f] `(ms/batch* ~f ~'upstream))
  ([f & downstreams]
     `(let [~'upstream (ms/batch* ~f ~'upstream)]
        ~@downstreams
        ~'upstream)))

(defmacro reduce*
  "Like clojure.core/reduce but for graph computations"
  ([f default-value] `(ms/reduce* ~f ~default-value ~'upstream))
  ([f default-value & downstreams]
     `(let [~'upstream (ms/reduce* ~f ~default-value ~'upstream)]
        ~@downstreams
        ~'upstream)))

(defmacro consume
  [f] `(ms/consume ~'upstream ~f))
