(ns conan.components.detector
  (:require [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.scheduler :as sched]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [conan.anomaly-detection :as ad]
            [conan.utils :as utils]
            [conan.time-series-provider :as p]
            [outpace.config :refer [defconfig!]]))

(defn- profile->features [ts-provider profile->key->props]
  (let [profiles->X-trans (p/prediction-data ts-provider)
        features (fn [[profile key->props]]
                   [profile (utils/extract-bare-features (get profiles->X-trans profile)
                                                         key->props)])]
    (map features profile->key->props)))

(defn profile->score [profile->mus-and-sigmas profile->features]
  (let [score (fn [[profile features]]
                [profile (first (ad/scores features (get profile->mus-and-sigmas profile)))])]
    (map score
         profile->features)))

(defn profile->prediction [epsylon profile->score]
  (let [prediction (fn [[profile score]] [profile (first (ad/predict [score] epsylon))])]
    (map prediction profile->score)))

(defn detect [ts-provider score-logging-fn prediction-logging-fn trained-profiles epsylon]
  (let [run-all (fn [fn coll] (doseq [e coll] (fn e)) coll)
        profile->mus-and-sigmas (into {} (map (fn [[k v]] [k @(:mus-and-sigmas v)]) trained-profiles))
        profile->key->props (into {} (map (fn [[k v]] [k @(:key->props v)]) trained-profiles))]
    ;;(log/info "Start detection with " @mus+sigmas-atom)
    (->> (profile->features ts-provider profile->key->props)
         (profile->score profile->mus-and-sigmas)
         (#(run-all score-logging-fn %))
         (profile->prediction epsylon)
         (#(run-all prediction-logging-fn %)))))

(defn exc-logger [fn]
  (try
    (fn)
    (catch Exception e
      (log/error e))))

(defconfig! repeat-in-ms)
(defconfig! epsylon)

(defrecord Detector [ts-provider model-trainer scheduled-fn]
  cp/Lifecycle
  (start [self]
    (let [trained-profiles (get-in self [:model-trainer :trained-profiles])
          key->props-atom (:key->props (second (first trained-profiles)))
          mus+sigma-atom (:mus-and-sigmas (second (first trained-profiles)))]
      (log/info "Register Prom Detector")
      (assoc self :scheduled-fn (at/every repeat-in-ms (partial exc-logger (partial detect
                                                                                    ts-provider
                                                                                    #(utils/score->file "./scores" %)
                                                                                    utils/prediction->console
                                                                                    trained-profiles
                                                                                    epsylon))
                                          (at/mk-pool)
                                          :desc "Prom-Detector-Checker"))
      ))
  (stop [self]
    (log/info "Stopping Prom Detector")
    (at/kill scheduled-fn)
    self))


(defn new-detector [provider]
  (map->Detector {:ts-provider provider}))