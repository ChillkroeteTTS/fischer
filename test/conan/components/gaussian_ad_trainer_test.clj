(ns conan.components.gaussian-ad-trainer-test
  (:require [clojure.test :refer :all]
            [conan.components.gaussian-ad-trainer :as gadt]
            [conan.time-series-provider :as p]
            [conan.model :as m]
            [conan.anomaly-detection :as ad]
            [com.stuartsierra.component :as cp]
            [overtone.at-at :as at]))


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

(deftest trained-profile-test
  (testing "it delivers a trained anomaly detection model with metadata about which features where used for training"
    (let [training-data {{:__name__ "metric1"} [50.0 51.0 49.0]
                         {:__name__ "metric2"} [4.0 5.0 3.0]}
          profile-config {:epsylon 0.5}]
      (with-redefs [m/train (constantly [{:mu    50.0
                                          :sigma 0.6}
                                         {:mu    4.0
                                          :sigma 0.6}])]
        (is (= {:key->props {{:__name__ "metric1"} {:idx          0
                                                    :feature-vals [50.0 51.0 49.0]}
                             {:__name__ "metric2"} {:idx          1
                                                    :feature-vals [4.0 5.0 3.0]}}
                :models     [{:mu    50.0
                              :sigma 0.6}
                             {:mu    4.0
                              :sigma 0.6}]
                :epsylon    0.5}
               @(#'gadt/trained-profile nil profile-config training-data)))))))

(def train-rs-1 [{:mu 50.0 :sigma 0.6}])
(def train-rs-2 [{:mu 40.0 :sigma 0.6}])
(def expected-rs {:profile1 {:key->props {{:__name__ "metric1"} {:idx 0, :feature-vals [1]}}
                             :epsylon    0.02
                             :models     train-rs-1}
                  :profile2 {:key->props {{:__name__ "metric2"} {:idx 0, :feature-vals [2]}}
                             :epsylon    0.04
                             :models     train-rs-2}})

(def dummy-data {:profile1 {{:__name__ "metric1"} [1]} :profile2 {{:__name__ "metric2"} [2]}})
(defrecord TestNilProvider []
  p/TimeSeriesProvider
  (prediction-data [_] dummy-data)
  (training-data [_] dummy-data))

(defrecord TestModel []
  m/Model
  (train [_ data]
    (get {1 train-rs-1 2 train-rs-2} (first (first data))))
  (scores [_ _ _] nil)
  (predict [_ _ _] nil))

(defn deatomized-trained-profile [rs] (into {} (map (fn [[k v]] [k @v]) rs)))
(deftest GaussianAnomalyDetectionTrainer-test
  (testing "it tests if the training data is used to train and save an ad model"
    (with-redefs [at/every (fn [_ fn _ _ _] (fn))]
      (let [cp (cp/start (gadt/map->GaussianAnomalyDetectionTrainer {:profiles    {:profile1 {:epsylon 0.02}
                                                                                   :profile2 {:epsylon 0.04}}
                                                                     :ts-provider (->TestNilProvider)
                                                                     :model       (->TestModel)}))]
        (is (= expected-rs
               (deatomized-trained-profile (:trained-profiles cp))))))))