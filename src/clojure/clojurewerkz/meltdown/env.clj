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

