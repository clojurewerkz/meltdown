(ns clojurewerkz.meltdown.env
  "Convenience functions for working with Reactor environments"
  (:import reactor.core.Environment))

(defn create
  "Instantiates new environment"
  []
  (Environment.))

(def ^{:doc "Provides access to default (memorized) environment"}
  environment (memoize create))

(defn shutdown
  "Shuts environment down. This shuts down all the reactors
   associated with this environment"
  [^Environment env]
  (.shutdown env))

