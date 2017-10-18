(ns fischer.core-test
  (:require [fischer.core :as c]
            [clojure.test :refer :all]
            [fischer.time-series-provider :as p]
            [fischer.utils :as utils]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]
            [fischer.reporter.prediction-reporter :as r]
            [fischer.models.gaussian-model :as gauss]))

(defrecord TestProvider []
  p/TimeSeriesProvider
  (prediction-data [_] {:normal-profile    {{:keymap1 1} [50.0]
                                            {:keymap2 2} [4.0]}
                        :anomalous-profile {{:keymap1 1} [85.0]
                                            {:keymap2 2} [15.0]}})
  (training-data [_] {:normal-profile    {{:keymap1 1} [50.0 51.0]
                                          {:keymap2 2} [5.0 3.0]}
                      :anomalous-profile {{:keymap1 1} [50.0 51.0]
                                          {:keymap2 2} [5.0 3.0]}}))

(def std-conf {:epsylon 0.02 :days-back nil :step-size nil :queries nil})
(def profile-conf {:normal-profile std-conf :anomalous-profile std-conf})

(defrecord TestReporter [pred-atom]
  r/PredictionReporter
  (report [_ profiles->predictions]
    (swap! pred-atom conj profiles->predictions)))

(deftest core-test
  (testing "it detects an anomaly"
    (let [prediction-atom (atom [])
          test-reporter (->TestReporter prediction-atom)]
      (with-redefs [at/every (fn [_ fn _ _ _] (fn))
                    at/stop identity
                    c/reporters (constantly [test-reporter])]
        (let [s (cp/start (c/fischer-system (->TestProvider) (gauss/->GaussianModel) profile-conf []))]
          (is (= [{:normal-profile    {:e 0.02
                                       :p false
                                       :s 0.08615711720739454}
                   :anomalous-profile {:e 0.02
                                       :p true
                                       :s 0.0}}]
                 @prediction-atom))
          (cp/stop s))))))

(deftest reporters-test
  (testing "it creates a file reporter"
    (is (= [fischer.reporter.file_reporter.FileReporter]
           (map type (c/reporters [{:type :file :file-path nil}] nil)))))
  (testing "it creates a console reporter"
    (is (= [fischer.reporter.console_reporter.ConsoleReporter]
           (map type (c/reporters [{:type :console}] nil)))))
  (testing "it creates multiple reporter"
    (is (= [fischer.reporter.file_reporter.FileReporter
           fischer.reporter.console_reporter.ConsoleReporter]
           (map type (c/reporters [{:type :file :file-path nil} {:type :console}] nil))))))