(ns conan.detector-test
  (:require [clojure.test :refer :all]
            [conan.components.detector :as d]
            [clojure.java.io :as io]
            [conan.anomaly-detection :as ad]
            [conan.time-series-provider :as p]
            [conan.reporter.prediction-reporter :as r]))

(defrecord TestReporter [pred-atom]
  r/PredictionReporter
  (report [_ profiles->predictions]
    (dorun (map (fn [[profile pred]] (swap! pred-atom conj {profile pred}))
                profiles->predictions))))

(def trained-profiles {:profile1 (atom {:key->props     {}
                                        :mus-and-sigmas [{:mu 101/2, :sigma 0.25} {:mu 4, :sigma 1.0}]
                                        :epsylon        0.2})})

(deftest detect-test
  (testing "it detects anomalies on each call"
    (with-redefs [p/prediction-data (constantly nil)
                  ad/scores (constantly [0.1])
                  ad/predict (constantly [false])]
      (let [score-atom (atom [])
            prediction-atom (atom [])
            test-reporter (->TestReporter prediction-atom)
            sc-log-fn (fn [[profile score]] (swap! score-atom conj {profile score}))]
        (d/detect nil sc-log-fn [test-reporter] trained-profiles)
        (d/detect nil sc-log-fn [test-reporter] trained-profiles)
        (is (= [{:profile1 0.1} {:profile1 0.1}] @score-atom))
        (is (= [{:profile1 false} {:profile1 false}] @prediction-atom))))))

(deftest report-predictions!-test
  (testing "it passes the predictions to one reporter"
    (let [pred-atom (atom [])]
      (d/report-predictions! [(->TestReporter pred-atom)] {:p1 true})
      (is (= [{:p1 true}]
             @pred-atom))))
  (testing "it passes the predictions to two reporters"
    (let [pred-atom1 (atom [])
          pred-atom2 (atom [])]
      (d/report-predictions! [(->TestReporter pred-atom1) (->TestReporter pred-atom2)] {:p1 true})
      (is (= [{:p1 true}]
             @pred-atom1))
      (is (= [{:p1 true}]
             @pred-atom2)))))
