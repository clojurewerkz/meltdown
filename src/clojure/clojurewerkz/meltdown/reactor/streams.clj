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
                     (mc/from-fn-raw
                      #(notify reactor downstream-key
                               (f %))))
  reactor)

(defn filter*
  [reactor selector downstream-key filter-fn]
  (register-consumer reactor selector
                     (mc/from-fn-raw
                      #(when (filter-fn %)
                         (notify reactor downstream-key %))))
  reactor)

(defn split*
  [reactor selector downstream-key split-fn]
  (let [state (atom [])
        lock  (Object.)]
    (register-consumer reactor selector
                       (mc/from-fn-raw
                        (fn [event]
                          (locking lock
                            (let [st (swap! state conj event)]
                              (when (split-fn st)
                                (notify reactor downstream-key st)
                                (reset! state []))))))))
  reactor)

(defn batch*
  [reactor selector downstream-key i]
  (split* reactor selector downstream-key #(== i (count %))))

(defn reduce*
  [reactor selector downstream-key f initial]
  (let [state (atom initial)]
    (register-consumer reactor selector
                       (mc/from-fn-raw
                        (fn [event]
                          (notify reactor downstream-key (swap! state #(f % event)))))))
  reactor)

(defn consume
  ([reactor selector f]
     (consume reactor selector nil f))
  ([reactor selector downstream-key f]
     (on reactor selector f)
     reactor))

(defmacro anonymous-stream
  [reactor root-key downstream-key & forms]
  (loop [x             []
         parent-key    root-key
         [form & more] forms]
    (if (empty? more)
      (conj x
            `(~(first form) ~reactor ($ ~parent-key) ~downstream-key ~@(next form)))
      (let [current-key (keyword (gensym))
            threaded    `(~(first form) ~reactor ($ ~parent-key) ~current-key ~@(next form))]
        (recur (conj x threaded) current-key more)))))
