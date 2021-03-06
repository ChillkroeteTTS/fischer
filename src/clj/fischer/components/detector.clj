(ns fischer.components.detector
  (:require [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [fischer.anomaly-detection :as ad]
            [fischer.utils :as utils]
            [fischer.time-series-provider :as p]
            [outpace.config :refer [defconfig!]]
            [fischer.reporter.prediction-reporter :as r]
            [fischer.reporter.console-reporter :as cr]
            [fischer.utils :as u]
            [fischer.model :as m]))

(defn- profile->features [ts-provider profile->key->props]
  (let [profiles->X-trans (p/prediction-data ts-provider)
        features (fn [[profile key->props]]
                   [profile (first (utils/extract-bare-features (get profiles->X-trans profile)
                                                                key->props))])]
    (map features profile->key->props)))

(defn profile->score [model profile->models profile->features]
  (let [score (fn [[profile features]]
                [profile (first (m/scores model [features] (get profile->models profile)))])]
    (map score
         profile->features)))

(defn profile->prediction [model profile->epsylon profile->score]
  (let [prediction (fn [[profile score]] [profile (first (m/predict model [score] (get profile->epsylon profile)))])]
    (into {} (map prediction profile->score))))

(defn report-predictions! [reporters profile->prediction]
  (dorun (map #(r/report % profile->prediction) reporters)))

(defn detect [ts-provider model reporters trained-profiles-atom]
  (let [sly-profile-map (fn [key] (reduce-kv #(assoc %1 %2 (get %3 key)) {} @trained-profiles-atom))
        profile->models (sly-profile-map :models)
        profile->key->props (sly-profile-map :key->props)
        profile->epsylon (sly-profile-map :epsylon)
        profile->score (->> (profile->features ts-provider profile->key->props)
                            (profile->score model profile->models))
        profile->prediction (profile->prediction model profile->epsylon profile->score)]
    (report-predictions! reporters (into {} (map (fn [[profile s]] [profile {:e (get profile->epsylon profile) :s s :p (get profile->prediction profile)}]) profile->score)))))

(defconfig! repeat-in-ms)

(defrecord Detector [ts-provider model reporters model-trainer scheduled-fn]
  cp/Lifecycle
  (start [self]
    (let [trained-profiles (get-in self [:model-trainer :trained-profiles])]
      (log/info "Register Prom Detector")
      (assoc self :scheduled-fn (at/every repeat-in-ms (partial u/exc-logger (partial detect
                                                                                      ts-provider
                                                                                      model
                                                                                      reporters
                                                                                      trained-profiles))
                                          (at/mk-pool)
                                          :desc "Prom-Detector-Checker"))
      ))
  (stop [self]
    (log/info "Stopping Prom Detector")
    (at/kill scheduled-fn)
    self))


(defn new-detector [provider model reporters]
  (map->Detector {:ts-provider provider
                  :model       model
                  :reporters   reporters}))