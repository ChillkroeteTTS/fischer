(defproject fischer :lein-v
  :docker {:image-name "chillkroetetts/fischer"
           :dockerfile "Dockerfile"
           :build-dir  "."}
  :plugins [[com.roomkey/lein-v "6.2.0"]
            [lein-shell "0.5.0"]
            [lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]
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
                 [http-kit "2.2.0"]
                 [compojure "1.6.0"]
                 [clj-http "3.7.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-mock "0.3.1"]
                 [de.otto/goo "1.1.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/sente "1.11.0"]]
  :profiles {:test    {:jvm-opts ["-Dconfig.edn=test-resources/test-config.edn"]}
             :prod    {:jvm-opts ["-Dconfig.edn=config.edn"]}
             :uberjar {:aot :all}
             :dev     {:dependencies  [[reagent "0.7.0"]
                                       [re-frame "0.10.2"]
                                       [org.clojure/clojurescript "1.9.229"]
                                       [binaryage/devtools "0.9.4"]
                                       [figwheel-sidecar "0.5.14"]
                                       [com.cemerick/piggieback "0.2.2"]]
                       :source-paths  ["src/clj"]
                       :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                       ;; need to add the compliled assets to the :clean-targets
                       :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                         :target-path]}}
  :test-paths ["test/clj"]
  :aliases {"config" ["run" "-m" "outpace.config.generate"]}
  :release-tasks [["vcs" "assert-committed"]
                  ["v" "update"]                            ;; compute new version & tag it
                  ["clean"]
                  ["uberjar"]
                  ["vcs" "push"]
                  ["shell" "./docker_release.sh"]]
  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/cljs"]

                :figwheel     {:on-jsload "fischerboot.core/on-js-reload"
                               :open-urls ["http://localhost:3449/index.html"]}

                :compiler     {:main                 fischerboot.core
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/fischerboot.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true
                               :preloads             [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/fischerboot.js.min"
                               :main          fischerboot.core
                               :optimizations :advanced
                               :pretty-print  false}}]}
  :figwheel {:css-dirs ["resources/public/css"]}
  )

