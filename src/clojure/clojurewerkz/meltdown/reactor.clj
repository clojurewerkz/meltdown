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
  (:require [clojurewerkz.meltdown.consumers :as mc])
  (:import [reactor.core R Reactor]
           [reactor.fn Selector Consumer Event]
           clojure.lang.IFn))


(defn ^Reactor create
  []
  (R/create))

(defn on
  ([^IFn f]
     (R/on (mc/from-fn f)))
  ([^Reactor reactor ^IFn f]
     (.on reactor (mc/from-fn f)))
  ([^Reactor reactor ^Selector selector ^IFn f]
     (.on reactor selector (mc/from-fn f))))

(defn notify
  ([event]
     (R/notify event))
  ([^Reactor reactor key payload]
     (.notify reactor ^Object key (Event. payload))))
