(ns conan.components.gaussian-ad-trainer
  (:require [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [conan.model :as m]
            [conan.utils :as utils]
            [clojure.string :as s]
            [outpace.config :refer [defconfig!]]
            [conan.time-series-provider :as p]
            [overtone.at-at :as at]
            [conan.utils :as u]))

(defconfig! training-data-to-disk)

(defn write-to-plate! [data]
  (when training-data-to-disk
    (let [csv (s/join "\n" (map (partial s/join ",") data))]
      (spit "trainData" csv)))
  data)

(defn- build-index-map [X-trans-map]
  (into {} (map-indexed (fn [idx [keymap values]] [keymap {:idx idx}]) X-trans-map)))

(defn- check-completeness [X-trans-map]
  (let [apply-count-to-vals #(assoc %1 %2 (count %3))
        key->sample-no (reduce-kv apply-count-to-vals {} X-trans-map)
        sample-no->#occurrences (reduce-kv apply-count-to-vals {} (group-by identity (vals key->sample-no)))
        most-common-sample-no (second (last (sort (clojure.set/map-invert sample-no->#occurrences))))]
    (reduce-kv (fn [m k _] (assoc m k {:train-sample-no (get key->sample-no k)
                                    :train-sample-complete? (= most-common-sample-no
                                                               (get key->sample-no k))}))
            {} X-trans-map)))

(defn- key->properties [X-trans-map]
  (let [key->completion-info (check-completeness X-trans-map)
        key->index (build-index-map X-trans-map)]
    (->> (into {} (map (fn [[k v]] [k {:feature-vals v}]) X-trans-map))
         (merge-with merge key->index)
         (merge-with merge key->completion-info))))

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

(defn- fill-time-series-gaps [X-trans-map]
  (if (>= (apply min (map #(count (second %)) X-trans-map)) 3)
    (let [fill-gaps-in-series (fn [[k series]] [k (->> (partition 3 1 series)
                                                       (map interpolate-if-nil)
                                                       ((fn [lst] [(first (first lst))
                                                                   (map #(nth % 1) lst)
                                                                   (last (last lst))]))
                                                       (flatten))])]
      (into {} (map fill-gaps-in-series X-trans-map)))
    X-trans-map))

(defn- add-artificial-variance [X-trans-map]
  (let [zero-to-one (fn [val] (if (= 0 val) 1 val))
        variance-if-necessary (fn [feature-vals]
                                (if (apply = feature-vals)
                                  (map (fn [val] (+ val (rand (zero-to-one (/ val 100))))) feature-vals)
                                  feature-vals))]
    (reduce-kv #(assoc %1 %2 (variance-if-necessary %3)) {} X-trans-map)))

(defn- trained-profile [model config X-trans-map]
  (let [sanitized-X-trans-map (-> (fill-time-series-gaps X-trans-map) (add-artificial-variance))
        key->props (key->properties sanitized-X-trans-map)
        models (-> sanitized-X-trans-map
                       (utils/extract-bare-features key->props)
                       (write-to-plate!)
                       (#(m/train model %)))]
    (prn models)
    {:key->props key->props
     :models     models
     :epsylon    (:epsylon config)}))

(defn- train-models [ts-provider model profiles models-atom]
  (log/info "Train model...")
  (let [profiles+trained-profiles (fn [[profile X-trans]] [profile (trained-profile model (get profiles profile) X-trans)])
        trained-profiles (->> (p/training-data ts-provider)
                              (map profiles+trained-profiles)
                              (into {}))]
    (log/info "... model trained")
    (reset! models-atom trained-profiles)))

(defconfig! model-training-interval-in-ms)

(defrecord GaussianAnomalyDetectionTrainer [ts-provider model profiles trained-profiles]
  cp/Lifecycle
  (start [self]
    (let [models-atom (atom {})]
      (log/info "Register Gaussian anomaly detection model trainer")
      (at/every model-training-interval-in-ms
                (partial u/exc-logger #(train-models ts-provider model profiles models-atom))
                (at/mk-pool)
                :desc "Gaussian anomaly detection model trainer")
      (assoc self
        :trained-profiles models-atom)))
  (stop [self]
    (log/info "Stopping Gaussian anomaly detection trainer")
    self))


(defn new-gaussian-ad-trainer [provider model profiles]
  (map->GaussianAnomalyDetectionTrainer {:ts-provider provider
                                         :model       model
                                         :profiles    profiles}))
