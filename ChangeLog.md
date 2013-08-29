## Changes between 1.0.0-alpha1 and 1.0.0-alpha2

### Building declarative graphs

New namespace, `clojurewerkz.meltdown.stream-graph`, was added for building graphs in a declarative
manner

```clj
(ns my-stream-graphs-ns
  (:use clojurewerkz.meltdown.stream-graph))

(def res (atom nil))

(def channel (graph (create)
                    (map* inc
                          (reduce* #(+ %1 %2) 0
                                   (consume #(reset! res %))))))

(accept channel 1)
(accept channel 2)
(accept channel 3)

@res
;; => 9
```

In order to attach and detach graph parts, you can use `attach` and `detach` functions from same
namespace:

```clj
(let [even-sum (atom nil)
      odd-sum (atom nil)
      even-summarizer (detach
                       (filter* even?
                                (reduce* #(+ %1 %2) 0
                                         (consume #(reset! even-sum %)))))

      odd-summarizer (detach
                      (filter* odd?
                               (reduce* #(+ %1 %2) 0
                                        (consume #(reset! odd-sum %)))))
      summarizer #(+ %1 %2)
      channel (graph (create)
                     (map* inc
                           (attach even-summarizer)
                           (attach odd-summarizer)))]

  (accept channel 1)
  (accept channel 2)
  (accept channel 3)
  (accept channel 4)

  @even-sum
  ;; => 6

  @odd-sum
  ;; => 8
  )
```


### Custom streams

Added an ability to create custom streams, whenever `map*`, `reduce*`, `filter*` and `batch*` are not
enough. For that, you can use `clojurewerkz.meltdown.streams/custom-stream`. For example, you'd like
to create a stream that will only dispatch every 5th value further. For state, you can use
let-over-lamda:

```clj
(defn every-fifth-stream
  "Defines a stream that will receive all events from upstream and dispatch
   every fifth event further down"
  [upstream]
  (let [counter (atom 0)]
    (custom-stream
     (fn [event downstream]
       (swap! counter inc)
       (when (= 5 @counter)
         (reset! counter 0)
         (accept downstream event)))
     upstream)))
```

You can use custom streams same way as you usually use internal ones:

```clj
(def channel (create))

(def result (atom nil))

(def incrementer (map* inc channel)
(def inst-every-fifth-stream (every-fifth-stream incrememter))
(consume inst-every-fifth-stream #(reset! res %))

(accept channel 1) ;; @res is still `nil`
(accept channel 2) ;; @res is still `nil`
(accept channel 3) ;; @res is still `nil`
(accept channel 4) ;; @res is still `nil`
(accept channel 5)
@res ;; => 6
```



## Initial Release: 1.0.0-alpha1

Initial release

Supported features:

  * Reactor operations, such as `notify`, `on`, `send`, `receive`
  * Reactor configuration options, such as `dispatcher` and `routing-strategy`
  * Selectors, `$` and `regexp` ones
  * Support for raw operations on reactor to avoid overhead of wrapping and deserialization
    on Meltdown side
  * Stream & deferred operations such as `map*`, `reduce*`, `filter*` and `batch*`
