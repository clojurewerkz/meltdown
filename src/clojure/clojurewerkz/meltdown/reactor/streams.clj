(ns clojurewerkz.meltdown.reactor.streams
  (:require [clojurewerkz.meltdown.reactor   :refer :all]
            [clojurewerkz.meltdown.selectors :as ms :refer [$ R]]
            [clojurewerkz.meltdown.consumers :as mc]
            [clojurewerkz.meltdown.events    :as me])
  (:import  [reactor.event.selector Selector Selectors]))

;; TODO: allow selecting default function for building fns
(defn map*
  [reactor selector downstream-key f]
  (register-consumer reactor selector
                     (mc/from-fn
                      #(notify reactor downstream-key
                               (f %))))
  reactor)

(defn filter*
  [reactor selector downstream-key filter-fn]
  (register-consumer reactor selector
                     (mc/from-fn
                      #(when (filter-fn %)
                         (notify reactor downstream-key %))))
  reactor)

(defn batch*
  [reactor selector downstream-key i]
  (let [state (atom [])
        lock  (Object.)]
    (register-consumer reactor selector
                       (mc/from-fn
                        (fn [event]
                          (locking lock
                            (let [st (swap! state conj event)]
                              (when (== i (count st))
                                (notify reactor downstream-key st)
                                (reset! state []))))))))
  reactor)

(defn consume
  [reactor selector f]
  (on reactor selector f)
  reactor)

(defmacro anonymous-stream
  [reactor root-key downstream-key & forms]
  (loop [x             []
         parent-key    root-key
         [form & more] forms]
    (if (empty? more)
      (conj x
            `(~(first form) ~reactor ($ ~parent-key) ~downstream-key ~@(next form)))
      (let [current-key (keyword (gensym))
            ;; current-obj      (get-object current-selector)
            threaded    `(~(first form) ~reactor ($ ~parent-key) ~current-key ~@(next form))]
        (recur (conj x threaded) current-key more))
      )))
