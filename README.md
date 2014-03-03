# Meltdown, a Clojure Interface to Reactor

Meltdown is a Clojure interface to [Reactor](https://github.com/reactor), an asynchronous
programming, event passing and stream processing [toolkit for the JVM](http://spring.io/blog/2013/11/12/it-can-t-just-be-big-data-it-has-to-be-fast-data-reactor-1-0-goes-ga).

It follows the path of
[Romulan](https://github.com/clojurewerkz/romulan), an old
ClojureWerkz project on top of [LMAX Disruptor](http://lmax-exchange.github.io/disruptor/) that's
been abandoned.


## Project Goals

 * Provide a convenient, reasonably idiomatic Clojure API for Reactor
 * Not introduce a lot of overhead
 * Be well documented
 * Be well tested

In addition to providing a core Reactor API for Clojure, Meltdown provides a DSL
for stream processing.


## Project Maturity

Meltdown is **young**, although the API hasn't changed
in a while. However, as Reactor itself is young, breaking
API changes are not out of the question.



## Artifacts

Meltdown artifacts are
[released to Clojars](https://clojars.org/clojurewerkz/meltdown). If you
are using Maven, add the following repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

```clj
[clojurewerkz/meltdown "1.0.0-beta5"]
```

With Maven:

```xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>meltdown</artifactId>
  <version>1.0.0-beta5</version>
</dependency>
```

## Community

Meltdown uses
[Reactor mailing list](https://groups.google.com/group/reactor-framework/). Feel
free to join it and ask any questions you may have.


To subscribe for announcements of releases, important changes and so on,
please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on
Twitter.


## Documentation

### Basic concepts

`Reactor` is an event-driven programming toolkit for the JVM which
offers multiple features. At its core, however, it is a message
passing library, that's used to tie event publishers and consumers
together. Handlers can be attached to and detached from a reactor
dynamically. When handler is attached to reactor, selector is
used. Selectors is a way for reactor to know when to call a handler.

To start using Meltdown, first define a reactor
using `clojurewerkz.meltdown.reactor/create`:

```clj
(require '[clojurewerkz.meltdown.reactor :as mr])

(let [reactor (mr/create)]
  )
```

You can subscribe to events triggered within reactor by using
`clojurewerkz.meltdown.reactor/on`:

```clj
(require '[clojurewerkz.meltdown.reactor :as mr :refer [$]])

(mr/on reactor ($ "key") (fn [event]
                           # do work
                             ))
```

`($ "key")` here is a **selector**. In essence, that means that every time
reactor receives event dispatched with the key `"key"`, it will call your
handler.

In order to push events into reactor, use `clojurewerkz.meltdown.reactor/notify`:

```clj
(require '[clojurewerkz.meltdown.reactor :as mr])

;; you can pass any data structures/objects over
;; reactors
(mr/notify reactor "key" {:my "payload"})
```

### Selectors

There're two types of selectors supported: exact match and regular
expression. Exact match should be used for cases when you want handler
to respond to the single key:

```clj
(mr/on reactor ($ "key") (fn [event] (do-one-thing event)))
(mr/on reactor ($ "key") (fn [event] (do-other-thing event)))
(mr/on reactor ($ "key") (fn [event] (do-something-else event)))

(mr/notify reactor "key" {:my :payload}) ;; will fire all three handlers
(mr/notify reactor "other" {:other :payload}) ;; will fire none
```

That means that all three handlers will receive a payload.

Regular expression selectors are used whenever you want to match one or
many event keys based on some pattern, for example:

```clj
(mr/on reactor (R "USA.*") (fn [event] (usa-handler event)))
(mr/on reactor (R "Europe.*") (fn [event] (europe-handler event)))
(mr/on reactor (R "Europe.Sw*") (fn [event] (sw-handler event)))

(mr/notify reactor "USA.EastCoast" {:teh :payload}) ;; will fire USA.*
(mr/notify reactor "Europe.Germany" {:das :payload}) ;; will fire Europe.*
(mr/notify reactor "Europe.Sweden" {:das :payload}) ;; will Europe.Sw* and Europe.* handlers
(mr/notify reactor "Europe.Switzerland" {:das :payload}) ;; will Europe.Sw* and Europe.* handlers
(mr/notify reactor "Asia.China" {:teh :payload}) ;; will fire none
```

### Routing Strategies

Whenever you have more than a single handler attached for selector, you
can define a routing strategy:

 * `:first` routing strategy will take a first handler for which
   selector matches key
 * `:broadcast` will dispatch event to every handler whose selector
   matches key
 * `:round-robin` on each run, will dispatch event to least recently
   used handler for which selector matches key. For example, if there're
   three handlers, first event will get to first, second - to second,
   third - to third, fourth - to first again and so on.

You can chose a routing strategy that makes most sense for your
application. `:first` is usually used when there should be a guarantee
that a single, first-assigned handler should take care of
event. `:broadcast` makes sense when all handlers should get an event
simultaneously, and perform different actions. And `:round-robin` would
make sense for things like load-balancing, whenever you would like to
keep all workers equally busy, therefore giving the one that just
received an event a chance to take care of it before it gets the next one.

In order to chose a routing strategy, pass one of the mentioned values
to `create` function, for example:

```clj
(mr/create :event-routing-strategy :broadcast)
```

### Dispatchers

There are several types of dispatchers, providing you a toolkit for both
threadpool-style long-running execution to high-throughput task
dispatching.

  * default one, syncronous dispatcher, implementation that dispatches
    events using the calling thread.
  * `:event-loop` dispatcher implementation that dispatches events using
    the single dedicated thread. Together with default syncronous dispatcher,
    very useful in development mode.
  * `:thread-pool` dispatcher implementation that uses
    `ThreadPoolExecution` with an unbounded queue to dispatch
    events. Works best for long-running tasks.
  * `:ring-buffer` dispatcher implementation that uses LMAX Disruptor
    RingBuffer to queue tasks to execute. Known to be most
    high-througput implementation.


In order to create a reactor backed by the dispatched of your
preference, pass `:dispatcher-type` to `create` function, for example:

```clj
(mr/create :dispatcher-type :ring-buffer)
```

### Request/response Pattern

It is possible to receive get a callback from the callee. In order to
implement request-response with callback, use `send-event`
`receive-event` pair of methods.

`receive-event` is used instead of `notify` for performance
reasons. It's an expensive operation to check for `:respond-to` field
for each event. Result of the handler execution will be passed back to
the caller.

For example, if you'd like to send an event with `hello-key` key, execute a
callback function whenever response is received, you can do it that way:

```clj
;; Result of handler execution will be passed back to callback in send-event
(mr/receive-event reactor ($ "hello-key") (fn [_] "response"))

;; Sends "data" to "hello-key" and waits for handler to call back
(mr/send-event reactor "hello-key" "data" (fn [event]
                                            ;; do job with response
                                            ))
```

### Stream Processing

Two main concepts in stream processing are `channel` and `stream`. You
can publish information to `channel`, create arbitrary amount of streams
out of any `channel` or `stream`.

`stream` is a stateless event processor, that allows values that are
going through it to be filtered or changed. It's very easy to build
large processing graphs using this concept, since every mutation returns
another `stream` you can attach consumers to.

In order to compose streams, you can use `clojurewerkz.meltdown.streams/map*`, `filter*`, `reduce*`
and `batch*` functions. They have signatures similar to the ones you're
used to have in clojure. Applying `map*` with `inc` function on the
channel will create a new `stream` that contains all the values
incremented by one.

#### Stream Flushing

Stream processing in Reactor is (mostly) lazy. To begin feeding a channel
the values, you need to flush it with `clojurewerkz.meltdown.streams/flush`
first.

#### `map*` operation

For example, let's create a channel where we push integers, and two
streams with attached consumers that would calculate incremented and
decremented values for incoming ints:

```clj
(require '[clojurewerkz.meltdown.streams :as ms :refer [create consume accept map*]])

(let [channel (create)
      incremented-values (map* inc channel)
      decremented-values (map* dec channel)]
  (consume incremented-values (fn [i] (println "Incremented value: " i)))
  (consume decremented-values (fn [i] (println "Decremented value: " i)))
  (accept channel 1)
  (accept channel 2)
  (accept channel 3)
  (ms/flush channel))
;; => Incremented value:  2
;; => Decremented value:  0
;; => Incremented value:  3
;; => Decremented value:  1
;; => Incremented value:  4
;; => Decremented value:  2
```

You can also apply `map*` to streams that were already "mapped", reduced
filtered or batched:

```clj
(require '[clojurewerkz.meltdown.streams :as ms :refer [create consume accept map*]])

(let [channel (create)
      incremented-values (map* inc channel)
      squared-values (map* (fn [i] (* i i)) incremented-values)]
  (consume squared-values (fn [i] (println "Incremented and squared value: " i)))
  (accept channel 1)
  (accept channel 2)
  (ms/flush channel))
;; => Incremented and squared value:  4
;; => Incremented and squared value:  9
```

#### `filter*` operation

`filter*` would filter values that go through it and only pass ones for
which predicate matches further:

```clj
(require '[clojurewerkz.meltdown.streams :as ms :refer [create consume accept filter*]])

(let [channel (create)
      even-values (filter* even? channel)
      odd-values  (filter* odd? channel)]
  (consume even-values (fn [i] (println "Got an even value: " i)))
  (consume odd-values (fn [i] (println "Got an odd value: " i)))
  (accept channel 1)
  (accept channel 2)
  (accept channel 3)
  (accept channel 4)
  (ms/flush channel))
;; => Got an odd value:  1
;; => Got an even value:  2
;; => Got an odd value:  3
;; => Got an even value:  4
```

#### `reduce*` operation

`reduce*` works pretty much same way as `reduce` in clojure works,
except for it gets values from the stream and holds last accumulator
value:

```clj
(require '[clojurewerkz.meltdown.streams :as ms :refer [create consume accept reduce*]])

(let [channel (create)
      res (atom nil)
      sum (reduce* #(+ %1 %2) 0 channel)]
  (consume sum #(reset! res %))
  (accept channel 1)
  (accept channel 2)
  (accept channel 3)
  (ms/flush channel)
  @res)
;; => 6
```

#### `batch*` operation

For buffered operations, for example, when you'd like to have several
values batched together, and only bundled values are of interest for
you, you can use `batch*` that only emits values when buffer capacity is
reached and buffer is flushed.

#### Custom streams

If these four given operations are not enough for you and you'd like to
create a custom stream, it's quite easy as welll. For that, there's a
`custom-stream` operation available. For example, you'd like to create
a stream that will only dispatch every 5th value further. For state,
you can use let-over-lamda:

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
(let [channel (create)
      res (atom nil)
      incrementer (map* inc channel)
      inst-every-fifth-stream (every-fifth-stream incrementer)]
  (consume inst-every-fifth-stream #(reset! res %))
  (accept channel 1)
  (println @res)
  (accept channel 2)
  (println @res)
  (accept channel 3)
  (println @res)
  (accept channel 4)
  (println @res)
  (accept channel 5)
  @res)
;; => nil
;; => nil
;; => nil
;; => nil
;; => 6
```

### Building declarative graphs

You can also build processing graphs in a declarative manner. For example,
let's create a graph that will increment all incoming values, then aggregate
a sum of them and put that sum to atom.

For these examples, you should use `clojurewerkz.meltdown.stream-graph` namespace
instead of usual `streams` one.

```clj
(ns my-stream-graphs-ns
  (:use clojurewerkz.meltdown.stream-graph))

(let [res (atom nil)
      channel (graph (create)
                     (map* inc
                         (reduce* #(+ %1 %2) 0
                                    (consume #(reset! res %)))))]
  (accept channel 1)
  (accept channel 2)
  (accept channel 3)
  @res)
;; => 9
```

#### Attaching and detaching graph parts

If you see that your graph is too deeply nested, and you'd like to split it
in parts, you can use `attach` and `detach` functions. For example, here's
a graph that will increment each incoming value and calculate sums of
even and odd values separately:

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

## Error handling

Whenever an Exception occurs inside of your processing pipeline,
downstreams won't receive any events. In order to debug your
pipeline and see what exactly happened, you should subscribe
to the `Exception` events by using `on-error`.

Most generic case would be:

```clj
(mr/on-error r Exception (fn [event]
                           (println event)))
```

So whenever __any__ `Exception` (of any type) occurs inside your
processing pipeline, your handler will be executed. You can also use
subclasses:

```clj
(mr/on-error r RuntimeException (fn [event]
                                  (println event)))
```

In this case your handler will be only triggered when type of an occured
exception is `RuntimeException` or one of the derived classes.

You can have more than one handler per `Exception` type.

## Practical applications

You can use `reactor` for all kinds of events in your systems. Event can
represent an error, alarm or some other piece of information another
part of system should be aware of. Reactor calls do not block, therefore
should be used in cases when you'd like to fire-and-forget the event.

Using reactive approach has it's advantages, for example, if you're
responding to TCP socket connection, you can dispatch an event to
reactor, continue listening and be certain that a worker will pick it up
and take care of it.

You should be aware of the fact that if your handlers never finish (for
example, there's an endless loop inside one of handlers), you'll
eventually run out of available handlers, and won't be able to proceed.

## Performance

Throughput tests are included. They're formed in same exact way
Reactor's throughput tests are done, therefore you can compare outputs
directly. Overhead is insignificant.

## Supported Clojure Versions

Meltdown is built from the ground up for Clojure 1.4+.


## Continuous Integration Status

[![Continuous Integration status](https://secure.travis-ci.org/clojurewerkz/meltdown.png)](http://travis-ci.org/clojurewerkz/meltdown)



## Meltdown Is a ClojureWerkz Project

Meltdown is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with
 * [Monger](http://clojuremongodb.info)
 * [Langohr](https://github.com/michaelklishin/langohr)
 * [Elastisch](https://github.com/clojurewerkz/elastisch)
 * [Titanium](http://titanium.clojurewerkz.org)
 * [Welle](http://clojureriak.info)
 * [Neocons](http://clojureneo4j.info)
 * [Quartzite](https://github.com/michaelklishin/quartzite) and several others.


## Development

Meltdown uses [Leiningen
2](http://leiningen.org). Make
sure you have it installed and then run tests against supported
Clojure versions using

    lein all test

Then create a branch and make your changes on it. Once you are done
with your changes and all tests pass, submit a pull request on GitHub.



## License

Copyright (C) 2013-2014 Michael S. Klishin, Alex Petrov.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/clojurewerkz/meltdown/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

