(ns conan.components.gaussian-ad-trainer
  (:require [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [conan.anomaly-detection :as ad]
            [conan.utils :as utils]
            [clojure.string :as s]
            [outpace.config :refer [defconfig!]]
            [conan.time-series-provider :as p]))

(defconfig! training-data-to-disk)

(defn write-to-plate! [data]
  (when training-data-to-disk
    (let [csv (s/join "\n" (map (partial s/join ",") data))]
      (spit "trainData" csv)))
  data)

(defn- build-index-map [X-trans-map]
  (into {} (map-indexed (fn [idx [keymap values]] [keymap {:idx idx}]) X-trans-map)))

(defn- key->properties [X-trans-map]
  (let [key->index (build-index-map X-trans-map)]
    (->> (into {} (map (fn [[k v]] [k {:feature-vals v}]) X-trans-map))
         (merge-with merge key->index))))

(defn- engineered-features [X-trans]
  (let [no-values-per-feature (map (fn [[k-map vals]] (count vals)) X-trans)
        max-values (apply max no-values-per-feature)
        keep-complete-features (fn [[k-map vals]] (= (count vals) max-values))
        engineered (->> X-trans
                        (filter keep-complete-features)
                        (into {}))]
    (log/info (format "After engineering, %s from %s original features remain" (count engineered) (count X-trans)))
    engineered))

(defn- interpolate-if-nil [triple]
  (if (= nil (nth triple 1))
    [(nth triple 0)
     (double (/ (+ (nth triple 0) (nth triple 2)) 2))
     (nth triple 2)]
    triple))

(defn- fill-time-series-gaps [X-trans]
  (if (>= (apply min (map #(count (second %)) X-trans)) 3)
    (let [fill-gaps-in-series (fn [[k series]] [k (->> (partition 3 1 series)
                                                       (map interpolate-if-nil)
                                                       ((fn [lst] [(first (first lst))
                                                                   (map #(nth % 1) lst)
                                                                   (last (last lst))]))
                                                       (flatten))])]
      (into {} (map fill-gaps-in-series X-trans)))
    X-trans))

(defn- trained-profile [X-trans]
  (let [key->props (key->properties X-trans)
        mus+sigmas (some-> X-trans
                           (fill-time-series-gaps)
                           (utils/extract-bare-features key->props)
                           (write-to-plate!)
                           (ad/train))]
    {:key->props     (atom key->props)
     :mus-and-sigmas (atom mus+sigmas)}))

(defrecord GaussianAnomalyDetectionTrainer [ts-provider key->props mus-and-sigmas]
  cp/Lifecycle
  (start [self]
    (let [profiles+trained-profiles (fn [[profile X-trans]] [profile (trained-profile X-trans)])
          trained-profiles (->> ts-provider
                                (p/training-data)
                                (map profiles+trained-profiles)
                                (into {}))]
      (log/info "Register Gaussian anomaly detection trainer")
      (assoc self
        :trained-profiles trained-profiles)))
  (stop [self]
    (log/info "Stopping Gaussian anomaly detection trainer")
    self))


(defn new-gaussian-ad-trainer [provider]
  (map->GaussianAnomalyDetectionTrainer {:ts-provider provider}))
