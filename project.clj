(defproject clojurewerkz/meltdown "1.0.0-SNAPSHOT"
  :description "Clojure interface to Reactor, an event-driven programming toolkit for the JVM"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojurewerkz/support "0.15.0"]
                 [reactor/reactor-core "1.0.0.BUILD-SNAPSHOT"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :master {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :dev {:resource-paths ["test/resources"]
                   :plugins [[codox "0.6.4"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases {"all" ["with-profile" "dev:dev,1.4:dev,1.6:dev,master"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail}}
                 "springsource-snapshots" {:url "http://repo.springsource.org/snapshot"
                                           :snapshots true
                                           :releases {:checksum :fail :update :always}}}
  :javac-options      ["-target" "1.6" "-source" "1.6"]
  :jvm-opts           ["-Dfile.encoding=utf-8"]
  :source-paths       ["src/clojure"]
  :java-source-paths  ["src/java"])
