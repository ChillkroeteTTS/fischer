(ns conan.components.prometheus-detector
  (:require [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.scheduler :as sched]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [conan.anomaly-detection :as ad]
            [conan.utils :as utils]
            [conan.time-series-provider :as p]
            [outpace.config :refer [defconfig!]]))

(defn- profile->features [ts-provider trained-profiles]
  (let [profiles->X-trans (p/prediction-data ts-provider)
        features (fn [[profile trained-profile]]
                   [profile (utils/extract-bare-features (get profiles->X-trans profile)
                                                         @(:key->props trained-profile))])]
    (map features trained-profiles)))

(defn profile->score [profile->trained-profile profile->features]
  (let [score (fn [[profile features]]
                           [profile (first (ad/scores features @(:mus-and-sigmas (get profile->trained-profile profile))))])]
    (map score
         profile->features)))

(defn profile->prediction [epsylon profile->score]
  (let [prediction (fn [[profile score]] [profile (first (ad/predict [score] epsylon))])]
    (map prediction profile->score)))

(defn detect [ts-provider score-logging-fn prediction-logging-fn trained-profiles epsylon]
  ;;(log/info "Start detection with " @mus+sigmas-atom)
  (let [profile->features (profile->features ts-provider trained-profiles)
        profile->score (profile->score trained-profiles profile->features)
        profile->prediction (profile->prediction epsylon profile->score)]

    (doseq [profile+score profile->score]
      (score-logging-fn profile+score))
    (doseq [profile+prediction profile->prediction]
      (prediction-logging-fn profile+prediction))))

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
    (let [trained-profiles (get-in self [:model-trainer :trained-profiles])
          key->props-atom (:key->props (second (first trained-profiles)))
          mus+sigma-atom (:mus-and-sigmas (second (first trained-profiles)))]
      (log/info "Register Prom Detector")
      (assoc self :scheduled-fn (at/every repeat-in-ms (partial exc-logger (partial detect
                                                                                    ts-provider
                                                                                    #(utils/scores->file "./scores" %)
                                                                                    utils/predictions->console
                                                                                    trained-profiles
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