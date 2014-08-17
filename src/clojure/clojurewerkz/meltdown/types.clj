(ns clojurewerkz.meltdown.types)

(defprotocol Foldable
  (fold [coll f]))

(defprotocol Functor
  (fmap [coll f]))

(defprotocol Monoid
  (mempty [_])
  (mappend [old new]))

(defn mconcat
  [m coll]
  (reduce mappend (mempty m) coll))

;;
;; Monoid
;;

(extend-protocol Monoid
  clojure.lang.PersistentVector
  (mempty [_] [])
  (mappend [old new] (conj old new))

  clojure.lang.IPersistentMap
  (mempty [_] {})
  (mappend [old [k v]] (update-in old [k]
                                  #(if (nil? %)
                                     [v]
                                     (conj % v)))))

;;
;; Functor
;;

(extend-protocol Functor
  clojure.lang.PersistentVector
  (fmap [coll f]
    (map f coll))

  clojure.lang.IPersistentMap
  (fmap [m f]
    (zipmap (keys m)
            (map #(map f %) (vals m)))))

;;
;; Foldable
;;

(extend-protocol Foldable
  clojure.lang.PersistentVector
  (fold [coll f]
    (reduce f coll))

  clojure.lang.IPersistentMap
  (fold [m f]
    (zipmap (keys m)
            (->> m vals (map #(reduce f %))))))
