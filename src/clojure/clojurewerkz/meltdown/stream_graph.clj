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
  [body] `(fn [u#]
            (let [~'upstream u#]
              ~body)))

(defmacro attach
  "Attaches parts back to the graph as if they were declared as a part of graph from the
   beginning."
  [detached] `(~detached ~'upstream))

(defmacro map*
  "Like clojure.core/map but for graph computations"
  ([f] `(ms/map* ~f ~'upstream))
  ([f & downstreams] `(let [~'upstream (ms/map* ~f ~'upstream)]
                        ~@downstreams
                        ~'upstream)))

(defmacro filter*
  "Like clojure.core/filter but for graph computations"
  ([f] `(ms/filter* ~f ~'upstream))
  ([f & downstreams] `(let [~'upstream (ms/filter* ~f ~'upstream)]
                        ~@downstreams
                        ~'upstream)))

(defmacro batch*
  ([f] `(ms/batch* ~f ~'upstream))
  ([f & downstreams] `(let [~'upstream (ms/batch* ~f ~'upstream)]
                        ~@downstreams
                        ~'upstream)))

(defmacro reduce*
  "Like clojure.core/reduce but for graph computations"
  ([f default-value] `(ms/reduce* ~f ~default-value ~'upstream))
  ([f default-value & downstreams] `(let [~'upstream (ms/reduce* ~f ~default-value ~'upstream)]
                                      ~@downstreams
                                      ~'upstream)))

(defmacro consume
  [f] `(ms/consume ~'upstream ~f))
