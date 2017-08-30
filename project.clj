(defproject conan "0.1.0-SNAPSHOT"
  :description "A clojure anomaly detection service"
  :main conan.core
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [incanter "1.9.1" :exclusions [org.clojure/clojure]]
                 [clj-http "3.6.1"]
                 [clojure-future-spec "1.9.0-alpha17"]
                 [de.otto/tesla-microservice "0.11.3"]
                 [de.otto.tesla/basic-logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 [com.outpace/config "0.10.0"]]
  :aliases {"config" ["run" "-m" "outpace.config.generate"]})