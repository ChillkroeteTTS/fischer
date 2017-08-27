(ns conan.components.prometheus-detector
  (:require [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.scheduler :as sched]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [conan.anomaly-detection :as ad]
            [conan.utils :as utils]
            [conan.time-series-provider :as p]
            [outpace.config :refer [defconfig!]]))

(defn detect [ts-provider score-logging-fn prediction-logging-fn mus+sigmas-atom key->props-atom epsylon]
  (log/info "Start detection with " @mus+sigmas-atom)
  (let [features (utils/extract-bare-features (second (first (p/prediction-data ts-provider))) ;; TODO: multi profile fix
                                              @key->props-atom)
        scores (ad/scores features @mus+sigmas-atom)
        predictions (ad/predict scores epsylon)]
    (score-logging-fn scores)
    (prediction-logging-fn predictions)))

(defn exc-logger [fn]
  (try
    (fn)
    (catch Exception e
      (log/error e))))

(defconfig! repeat-in-ms)
(defconfig! epsylon)

(defrecord PrometheusDetector [ts-provider model-trainer scheduled-fn]
  cp/Lifecycle
  (start [self]
    (let [key->props-atom (get-in self [:model-trainer :key->props])
          mus+sigma-atom (get-in self [:model-trainer :mus-and-sigmas])]
      (log/info "Register Prom Detector")
      (assoc self :scheduled-fn (at/every repeat-in-ms (partial exc-logger (partial detect
                                                                                    ts-provider
                                                                                    #(utils/scores->file "./scores" %)
                                                                                    utils/predictions->console
                                                                                    mus+sigma-atom
                                                                                    key->props-atom
                                                                                    epsylon))
                                          (at/mk-pool)
                                          :desc "Prom-Detector-Checker"))
      ))
  (stop [self]
    (log/info "Stopping Prom Detector")
    (at/kill scheduled-fn)
    self))


(defn new-prometheus-detector [provider]
  (map->PrometheusDetector {:ts-provider provider}))