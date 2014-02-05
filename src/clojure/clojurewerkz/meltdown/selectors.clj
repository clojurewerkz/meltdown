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

(ns clojurewerkz.meltdown.selectors
  (:import [reactor.event.selector Selector Selectors]
           reactor.function.Fn))

(defn ^Selector $
  ([^String sel]
     (Selectors/$ sel))
  ([]
     (let [sel (Selectors/$)]
       [sel (.getObject sel)])))

(defn ^Selector R
  [^String regex]
  (Selectors/R regex))

(defn ^Selector T
  "Creates a `Selector` based on the given class type that matches
   objects whose type is assignable according to `Class.isAssignableFrom(Class)`"
  [c]
  (Selectors/T c))
