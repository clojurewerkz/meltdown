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

(ns clojurewerkz.meltdown.fn
  "Interfaces Clojure functions to Reactor's Function interface and
   such"
  (:import [reactor.function Function Predicate]
           clojure.lang.IFn))

(defn ^Function ->function
  "Reifies a Clojure function to a reactor.fn.Function
   instance Reactor can work with"
  [IFn f]
  (reify Function
    (apply [this arg]
      (f arg))))

(defn ^Predicate ->predicate
  "Instantiates a reactor consumer from a Clojure
   function"
  [^IFn f]
  (proxy [Predicate] []
    (test [a]
      (f a))))
