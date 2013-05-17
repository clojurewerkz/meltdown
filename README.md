# Meltdown, a Clojure Interface to Reactor

Meltdown is a Clojure interface to [Reactor](https://github.com/reactor), an asynchronous
programming and event passing toolkit for the JVM.

It follows the path of
[Romulan](https://github.com/clojurewerkz/romulan), an old
ClojureWerkz project on top of LMAX Disruptor that's not currently
actively being worked on.


## Project Goals

 * Provide a convenient, reasonably idiomatic Clojure API for Reactor
 * Don't introduce a lot of overhead
 * Be well documented
 * Be well tested


## Community

Meltdown uses [Reactor mailing list](https://groups.google.com/group/reactor-framework/). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## Project Maturity

Meltdown is **very** (literally pre-alpha) young. Sweeping public API changes
are likely.



## Artifacts

Meltdown artifacts are [released to Clojars](https://clojars.org/clojurewerkz/meltdown). If you are using Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

    [clojurewerkz/meltdown "1.0.0-alpha1-SNAPSHOT"]


With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>meltdown</artifactId>
      <version>1.0.0-alpha1-SNAPSHOT</version>
    </dependency>



## Documentation & Examples

Meltdown is a **very** young project.
Our documentation site is not yet ready, sorry.



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
