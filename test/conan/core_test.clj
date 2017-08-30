(ns conan.core-test
  (:require [conan.core :as c]
            [clojure.test :refer :all]
            [conan.time-series-provider :as p]
            [de.otto.tesla.system :as system]
            [conan.utils :as utils]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [conan.reporter.prediction-reporter :as r]))

(defrecord TestProvider []
  p/TimeSeriesProvider
  (prediction-data [_] {:profile1 {{:keymap1 1} [50.0]
                                   {:keymap2 2} [4.0]}})
  (training-data [_] {:profile1 {{:keymap1 1} [50.0 51.0]
                                 {:keymap2 2} [5.0 3.0]}}))

(def profile-conf {:profile1 {:epsylon 0.02
                              :days-back nil
                              :step-size nil
                              :queries nil}})

(defrecord TestReporter [pred-atom]
  r/PredictionReporter
  (report [_ profiles->predictions]
    (dorun (map (fn [[profile pred]] (swap! pred-atom conj {profile pred}))
                profiles->predictions))))

(deftest core-test
  (testing "it detects an anomaly"
    (let [prediction-atom (atom [])
          test-reporter (->TestReporter prediction-atom)]
      (with-redefs [at/every (fn [_ fn _ _ _] (fn))
                    at/stop identity
                    conan.core/profiles profile-conf]
        (let [s (cp/start (c/conan-system (->TestProvider) [test-reporter]))]
          (is (= [{:profile1 {:e 0.02
                              :p false
                              :s 0.08615711720739454}}]
                 @prediction-atom))
          (cp/stop s))))))