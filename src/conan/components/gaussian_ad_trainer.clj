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

(defrecord GaussianAnomalyDetectionTrainer [ts-provider key->props mus-and-sigmas]
  cp/Lifecycle
  (start [self]
    (let [X-trans (second (first (p/training-data ts-provider))) ;; TODO: so far just a hack for one profile
          key->props (key->properties X-trans)
          key->props-atom (atom key->props)
          mus-and-sigmas-atom (some-> X-trans
                                      (utils/extract-bare-features key->props)
                                      (write-to-plate!)
                                      (ad/train)
                                      (atom))]
      (log/info "Register Gaussian anomaly detection trainer")
      (assoc self
        :key->props key->props-atom
        :mus-and-sigmas mus-and-sigmas-atom)))
  (stop [self]
    (log/info "Stopping Gaussian anomaly detection trainer")
    self))


(defn new-gaussian-ad-trainer [provider]
  (map->GaussianAnomalyDetectionTrainer {:ts-provider provider}))
