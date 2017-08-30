(defproject conan "0.1.0-SNAPSHOT"
  :description "A clojure anomaly detection service"
  :main conan.core
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [incanter "1.9.1" :exclusions [org.clojure/clojure]]
                 [clj-http "3.6.1"]
                 [clojure-future-spec "1.9.0-alpha17"]
                 [org.clojure/data.json "0.2.6"]
                 [com.outpace/config "0.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure-android/tools.logging "0.3.2"]
                 [overtone/at-at "1.2.0"]]
  :profiles {:test {:jvm-opts ["-Dconfig.edn=test-config.edn"]}
             :prod {:jvm-opts ["-Dconfig.edn=config.edn"]}}
  :aliases {"config" ["run" "-m" "outpace.config.generate"]})
