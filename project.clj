(defproject fischer :lein-v
  :docker {:image-name "chillkroetetts/fischer"
           :dockerfile "Dockerfile"
           :build-dir  "."}
  :plugins [[com.roomkey/lein-v "6.2.0"]
            [lein-shell "0.5.0"]]
  :description "A clojure anomaly detection service"
  :main fischer.core
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [incanter "1.9.1" :exclusions [org.clojure/clojure]]
                 [net.mikera/core.matrix "0.61.0"]
                 [clj-http "3.6.1"]
                 [clojure-future-spec "1.9.0-alpha17"]
                 [org.clojure/data.json "0.2.6"]
                 [com.outpace/config "0.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure-android/tools.logging "0.3.2"]
                 [overtone/at-at "1.2.0"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [compojure "1.6.0"]
                 [clj-http "3.7.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-mock "0.3.1"]
                 [de.otto/goo "1.1.0"]]
  :profiles {:test    {:jvm-opts ["-Dconfig.edn=test-resources/test-config.edn"]}
             :prod    {:jvm-opts ["-Dconfig.edn=config.edn"]}
             :uberjar {:aot :all}}
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :release-tasks [["vcs" "assert-committed"]
                  ["v" "update"] ;; compute new version & tag it
                  ["clean"]
                  ["uberjar"]
                  ["vcs" "push"]
                  ["shell" "./docker_release.sh"]])
