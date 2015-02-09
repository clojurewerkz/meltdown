(ns clojurewerkz.meltdown.reactor.streams-test
  (:require [clojurewerkz.meltdown.reactor         :as mr]
            [clojurewerkz.meltdown.selectors       :as ms :refer [$ R]]
            [clojurewerkz.meltdown.reactor.streams :refer :all]
            [clojure.test                          :refer :all]))

(alter-var-root #'*out* (constantly *out*))

(defmacro with-latch
  [countdown-from & body]
  `(let [latch# (CountDownLatch. ~countdown-from)
         ;; intentionally unhygienic, expected by @body
         ~'latch latch#]
     ~@body
     (is (.await latch# 2 TimeUnit/SECONDS))
     (is (= 0 (.getCount latch#)))))

(deftest test-anonymous-stream
  (let [reactor (mr/create)
        stream  (anonymous-stream reactor :upstream :downstream
                                  (map* #(inc (.getData %)))
                                  (map* #(inc (.getData %)))
                                  (map* #(inc (.getData %))))
        res     (atom nil)]

    (consume reactor ($ :downstream) #(reset! res (:data %)))

    (mr/notify reactor :upstream 1)

    (is (= @res 4))))
