(ns conan.components.detector
  (:require [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [conan.anomaly-detection :as ad]
            [conan.utils :as utils]
            [conan.time-series-provider :as p]
            [outpace.config :refer [defconfig!]]
            [conan.reporter.prediction-reporter :as r]
            [conan.reporter.console-reporter :as cr]))

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

(defn profile->prediction [profile->epsylon profile->score]
  (let [prediction (fn [[profile score]] [profile (first (ad/predict [score] (get profile->epsylon profile)))])]
    (into {} (map prediction profile->score))))

(defn report-predictions! [reporters profile->prediction]
  (dorun (map #(r/report % profile->prediction) reporters)))

(defn detect [ts-provider reporters trained-profiles]
  (let [profile->mus-and-sigmas (into {} (map (fn [[k v]] [k (:mus-and-sigmas @v)]) trained-profiles))
        profile->key->props (into {} (map (fn [[k v]] [k (:key->props @v)]) trained-profiles))
        profile->epsylon (into {} (map (fn [[k v]] [k (:epsylon @v)]) trained-profiles))
        profile->score (->> (profile->features ts-provider profile->key->props)
                            (profile->score profile->mus-and-sigmas))
        profile->prediction (profile->prediction profile->epsylon profile->score)]
    ;;(log/info "Start detection with " @mus+sigmas-atom)
    (report-predictions! reporters (into {} (map (fn [[profile s]] [profile {:e (get profile->epsylon profile) :s s :p (get profile->prediction profile)}]) profile->score)))))

(defn exc-logger [fn]
  (try
    (fn)
    (catch Exception e
      (log/error e))))

(defconfig! repeat-in-ms)

(defrecord Detector [ts-provider reporters model-trainer scheduled-fn]
  cp/Lifecycle
  (start [self]
    (let [trained-profiles (get-in self [:model-trainer :trained-profiles])]
      (log/info "Register Prom Detector")
      (assoc self :scheduled-fn (at/every repeat-in-ms (partial exc-logger (partial detect
                                                                                    ts-provider
                                                                                    reporters
                                                                                    trained-profiles))
                                          (at/mk-pool)
                                          :desc "Prom-Detector-Checker"))
      ))
  (stop [self]
    (log/info "Stopping Prom Detector")
    (at/kill scheduled-fn)
    self))


(defn new-detector [provider reporters]
  (map->Detector {:ts-provider provider
                  :reporters reporters}))