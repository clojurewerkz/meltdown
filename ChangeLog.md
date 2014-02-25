## Changes between 1.0.0-beta4 and 1.0.0-beta5

### Environment Reuse

Previously Meltdown instantiated a new `Environment` per
`clojurewerkz.meltdown.reactor/create` invocation without
a provided environment. This lead to excessive thread creation
which could eventually exhaust system resources.

Meltdown `1.0.0-beta5` will reuse the same environment for
all created reactors unless its asked to use a specific
`Environment` instance.


### Environment Functions

`clojurewerkz.meltdown.env/environment` is a function that returns
a shared environment. To create a completely new environment from
scratch, use `clojurewerkz.meltdown.env/create`.

`clojurewerkz.meltdown.env/shutdown` shuts down environments and
all associated dispatchers.


### `clojurewerkz.meltdown.fn/->filter`

`clojurewerkz.meltdown.fn/->filter` is a new function that reifies
Reactor filters from Clojure functions.


## Changes between 1.0.0-beta3 and 1.0.0-beta4

### Moved Functions

`clojurewerkz.meltdown.streams/fn->function` and
`clojurewerkz.meltdown.streams/fn->predicate` are removed, use
`clojurewerkz.meltdown.fn/->function` and
`clojurewerkz.meltdown.fn/->predicate` instead.

### Streams Flushing

Stream operations are now lazier in Reactor. To enforce stream
sources to be drained, use `clojurewerkz.meltdown.streams/flush`
which accepts a stream or deferred.

### Reactor Update

Reactor is updated to `1.1.0.M1` which has multiple breaking API
changes.


## Changes between 1.0.0-beta2 and 1.0.0-beta3

### Reactor Update

Reactor is updated to `1.0.0.RELEASE`.


## Changes between 1.0.0-beta1 and 1.0.0-beta2

### Reactor Update

Reactor is updated to `1.0.0.RC1`.

### Error handling improvements

You can add listeners for `Exceptions` that are occuring inside of your processing pipeline by
subscribing to events based on the `class` of exception. For example, in order to subscribe
to all Exceptions, you can:

```clj
(mr/on-error r Exception (fn [event]
                           (println event)))
```

In order to subscribe to only `RuntimeExceptions`:

```clj
(mr/on-error r RuntimeException (fn [event]
                                  (println event)))
```

## Changes between 1.0.0-alpha3 and 1.0.0-beta1

### Reactor Update

Reactor is updated to `1.0.0.M3`.


### Added dispatcher option to reactor and stream composition

When creating reactor, it's now possible to plug in a custom dispatcher or configure an underlying
dispatcher in a way that's most suitable for your application, for example:

```clj
(ns my-app.core
  (:import [reactor.event.dispatch RingBufferDispatcher]
           [com.lmax.disruptor.dsl ProducerType]
           [com.lmax.disruptor YieldingWaitStrategy]))

;; Creates a RingBuffer Dispatcher, with a custom queue size of 4096
(def reactor (mr/create :dispatcher (RingBufferDispatcher. "dispatcher-name"
                                                            4096
                                                            ProducerType/MULTI
                                                            (YieldingWaitStrategy.))))
```

## Changes between 1.0.0-alpha2 and 1.0.0-alpha3

### Environment option is added to stream creation

It is only possible to specify type of dispatcher when there's an Environment attached to reactor.
Option was previously missing.



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
