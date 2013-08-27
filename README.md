# Meltdown, a Clojure Interface to Reactor

Meltdown is a Clojure interface to [Reactor](https://github.com/reactor), an asynchronous
programming, event passing and stream processing toolkit for the JVM.

It follows the path of
[Romulan](https://github.com/clojurewerkz/romulan), an old
ClojureWerkz project on top of LMAX Disruptor that's not currently
actively being worked on.


## Project Goals

 * Provide a convenient, reasonably idiomatic Clojure API for Reactor
 * Not introduce a lot of overhead
 * Be well documented
 * Be well tested


## Community

Meltdown uses
[Reactor mailing list](https://groups.google.com/group/reactor-framework/). Feel
free to join it and ask any questions you may have.


To subscribe for announcements of releases, important changes and so on,
please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on
Twitter.



## Project Maturity

Meltdown is **very** (literally pre-alpha) young, yet API hasn't changed
for quite a long time already. We won't cut a final release until
underlying Reactor library reaches a stable release. Reactor itself also
evolves rapidly and is at the pre-alpha stage.



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
[clojurewerkz/meltdown "1.0.0-alpha1-SNAPSHOT"]
```

With Maven:

```xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>meltdown</artifactId>
  <version>1.0.0-alpha1-SNAPSHOT</version>
</dependency>
```

## Basic concepts

`Reactor` is a routing middleware, that's used to tie events and
handlers together. Handlers can be attached to and detached from reactor
dynamically. When handler is attached to reactor, selector is
used. Selectors is a way for reactor to know when to call a handler.

First thing you need to get started is to define a reactor:

```clj
(ns my-ns
  (:require [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :as ms :refer [$ R]])

(def reactor (mr/create))
```

You can subscribe to events triggered within reactor by using `on`:

```clj
(mr/on reactor ($ "key") (fn [event]
                           # do work
                             ))
```

`($ "key")` here is a selector. In essence, that means that every time
reactor receives event dispatched with key `"key"`, it will call your
handler.

In order to push events into reactor, you can use `notify`:

```clj
(mr/notify r "key" {:my "payload"})
```

## Selectors

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



## Routing strategies

## Dispatchers

## Request/response pattern

## Stream processing

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

Copyright (C) 2013 Michael S. Klishin, Alex Petrov.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
