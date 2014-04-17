(defproject clojurewerkz/meltdown "1.0.0-beta10"
  :description "Clojure interface to Reactor, an event-driven programming toolkit for the JVM"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.projectreactor/reactor-core "1.1.0.M3"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :master {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
             :dev {:resource-paths ["test/resources"]
                   :dependencies   [[com.lmax/disruptor "3.2.1"]]
                   :plugins [[codox "0.6.6"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases {"all" ["with-profile" "dev:dev,1.4:dev,1.5:dev,master"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail}}
                 "springsource-milestone" {:url "http://repo.springsource.org/libs-milestone"
                                           :releases {:checksum :fail :update :always}}
                 "springsource-snapshots" {:url "http://repo.springsource.org/libs-snapshot"
                                           :snapshots true
                                           :releases {:checksum :fail :update :always}}
                 "springsource-releases" {:url "http://repo.springsource.org/libs-release"
                                          :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :javac-options      ["-target" "1.6" "-source" "1.6"]
  :jvm-opts           ["-Dfile.encoding=utf-8"]
  :source-paths       ["src/clojure"]
  :java-source-paths  ["src/java"]
  :global-vars {*warn-on-reflection* true}
  :test-selectors     {:default     (fn [m] (not (:performance m)))
                       :performance :performance
                       :focus       :focus
                       :all         (constantly true)})
