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

(ns clojurewerkz.meltdown.events
  (:import [reactor.event Event Event$Headers]
           [clojure.lang IPersistentMap]))

(defprotocol EventToMap
  (event->map [e]))

(extend-protocol EventToMap
  Event
  (event->map [^Event event]
    {:data     (.getData event)
     :reply-to (.getReplyTo event)
     :headers  (into {} (.getHeaders event))
     :id       (.getId event)})

  IPersistentMap
  (event>map [e]
    e))

(defn pev
  [& {:keys [data reply-to ^IPersistentMap headers]}]
  (let [e (Event. (Event$Headers. headers) data)]
    (when reply-to
      (.setReplyTo e reply-to))
    e))