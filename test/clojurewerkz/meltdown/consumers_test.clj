(ns clojurewerkz.meltdown.consumers-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer [$]]
            [clojurewerkz.meltdown.consumers :as mc]))

(deftest test-consumer-count
  (let [no-op (fn [_])
        r     (mr/create)
        n     7]
    (dotimes [i n]
      (mr/on r ($ (format "key.%d" i)) no-op))
    ;; registrations for a class selector and object selector w/ anonymous
    ;; keys are added by default. MK.
    (is (>= (mc/consumer-count r) n))))
