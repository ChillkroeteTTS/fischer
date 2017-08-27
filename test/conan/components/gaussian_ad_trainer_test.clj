(ns conan.components.gaussian-ad-trainer-test
  (:require [clojure.test :refer :all]
            [conan.components.gaussian-ad-trainer :as gadt]
            [conan.time-series-provider :as p]
            [conan.anomaly-detection :as ad]
            [com.stuartsierra.component :as cp]))


(deftest build-index-map-test
  (is (= {:key1 {:idx 0}
          :key2 {:idx 1}}
         (#'gadt/build-index-map {:key1 [1 2 3 4]
                                  :key2 [5 6 7 8]}))))

(deftest key->properties-test
  (is (= {{}           {:idx          0
                        :feature-vals [1 2 3]}
          {:dummy nil} {:idx          1
                        :feature-vals [10 15.5 30]}}
         (#'gadt/key->properties {{}           [1 2 3]
                                  {:dummy nil} [10 15.5 30]}))))

(deftest engineered-features-test
  (testing "it removes each feature which has less values than the feature with the most values, so all features have same amount"
    (is (= {{}      [1 2 3]
            {:b :b} [4 5 6]}
           (#'gadt/engineered-features {{}      [1 2 3]
                                        {:a :a} [1 2]
                                        {:b :b} [4 5 6]})))))

(deftest fill-time-series-gaps
  (testing "it interpolates gaps(nils) in the time series if they appear only once"
    (is (= {{:__name__ :metric1} [1 2 3 4]
            {:__name__ :metric2} [nil 2 3 4]
            {:__name__ :metric3} [1 2.0 3 4]
            {:__name__ :metric4} [1 2 3.0 4]
            {:__name__ :metric5} [1 2 3 nil]}
           (#'gadt/fill-time-series-gaps {{:__name__ :metric1} [1 2 3 4]
                                          {:__name__ :metric2} [nil 2 3 4]
                                          {:__name__ :metric3} [1 nil 3 4]
                                          {:__name__ :metric4} [1 2 nil 4]
                                          {:__name__ :metric5} [1 2 3 nil]})))))

(defn deatomized-trained-profile [rs] (into {} (map (fn [[k v]] [k @v]) rs)))
(defn atomized-trained-profile [rs] (into {} (map (fn [[k v]] [k (atom v)]) rs)))

(deftest trained-profile-test
  (testing "it delivers a trained anomaly detection model with metadata about which features where used for training"
    (let [training-data {{:__name__ "metric1"} [50.0 51.0 49.0]
                         {:__name__ "metric2"} [4.0 5.0 3.0]}]
      (with-redefs (ad/train (constantly [{:mu    50.0
                                           :sigma 0.6}
                                          {:mu    4.0
                                           :sigma 0.6}]))
        (is (= {:key->props     {{:__name__ "metric1"} {:idx          0
                                                       :feature-vals [50.0 51.0 49.0]}
                                 {:__name__ "metric2"} {:idx          1
                                                       :feature-vals [4.0 5.0 3.0]}}
                :mus-and-sigmas [{:mu    50.0
                                  :sigma 0.6}
                                 {:mu    4.0
                                  :sigma 0.6}]}
               (deatomized-trained-profile (#'gadt/trained-profile training-data))))))))

(defrecord TestNilProvider []
  p/TimeSeriesProvider
  (prediction-data [_] {:profile1 :data1
                        :profile2 :data2})
  (training-data [_] {:profile1 :data1
                      :profile2 :data2}))

(deftest GaussianAnomalyDetectionTrainer-test
  (let [train-rs-1 {:key->props     {{:__name__ "metric1"} {:idx 0 :feature-vals [50.0 51.0 49.0]}}
                    :mus-and-sigmas [{:mu 50.0 :sigma 0.6}]}
        train-rs-2 {:key->props     {{:__name__ "metric1"} {:idx 0 :feature-vals [40.0 41.0 39.0]}}
                    :mus-and-sigmas [{:mu 40.0 :sigma 0.6}]}]
    (testing "it tests the startup of the component with 2 profiles"
      (with-redefs [gadt/trained-profile #(get {:data1 (atomized-trained-profile train-rs-1)
                                                :data2 (atomized-trained-profile train-rs-2)} %)]
        (let [cp (cp/start (gadt/map->GaussianAnomalyDetectionTrainer {:ts-provider (->TestNilProvider)}))
              trained-models (into {} (map (fn [[profile trained-profile]] [profile (deatomized-trained-profile trained-profile)]) (:trained-profiles cp)))]
          (is (= {:profile1 train-rs-1
                  :profile2 train-rs-2}
                 trained-models)))))))