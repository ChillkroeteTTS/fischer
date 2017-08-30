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
      (let [prediction-atom (atom [])
            test-reporter (->TestReporter prediction-atom)
            prediction {:profile1 {:p false :s 0.1 :e 0.2}}]
        (d/detect nil [test-reporter] trained-profiles)
        (d/detect nil [test-reporter] trained-profiles)
        (is (= [prediction prediction] @prediction-atom))))))

(def predictions {:p1 {:p true :s 0.1 :e 0.02}})

(deftest report-predictions!-test
  (testing "it passes the predictions to one reporter"
    (let [pred-atom (atom [])]
      (d/report-predictions! [(->TestReporter pred-atom)] predictions)
      (is (= [predictions]
             @pred-atom))))
  (testing "it passes the predictions to two reporters"
    (let [pred-atom1 (atom [])
          pred-atom2 (atom [])]
      (d/report-predictions! [(->TestReporter pred-atom1) (->TestReporter pred-atom2)] predictions)
      (is (= [predictions]
             @pred-atom1))
      (is (= [predictions]
             @pred-atom2)))))
