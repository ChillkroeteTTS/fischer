(ns fischer.detector-test
  (:require [clojure.test :refer :all]
            [fischer.components.detector :as d]
            [clojure.java.io :as io]
            [fischer.anomaly-detection :as ad]
            [fischer.time-series-provider :as p]
            [fischer.reporter.prediction-reporter :as r]
            [fischer.model :as m]))

(defrecord TestReporter [pred-atom]
  r/PredictionReporter
  (report [_ profiles->predictions]
    (dorun (map (fn [[profile pred]] (swap! pred-atom conj {profile pred}))
                profiles->predictions))))

(defrecord TestProvider []
  p/TimeSeriesProvider
  (training-data [_] nil)
  (prediction-data [_] {:profile1 {{:keymap1 1} [1] {:keymap2 2} [2]}}))

(def trained-profiles (atom {:profile1 {:key->props {{:keymap1 1} {:idx 0} {:keymap2 2} {:idx 1}}
                                        :models     [{:mu 101/2, :sigma 0.25} {:mu 4, :sigma 1.0}]
                                        :epsylon    0.2}}))

(deftest detect-test
  (testing "it writes the prediction in the prediction atom"
    (with-redefs [p/prediction-data (constantly nil)
                  m/scores (constantly [0.1])
                  m/predict (constantly [false])]
      (let [prediction-atom (atom [])
            test-reporter (->TestReporter prediction-atom)
            prediction {:profile1 {:p false :s 0.1 :e 0.2}}]
        (d/detect (->TestProvider) nil [test-reporter] trained-profiles)
        (d/detect (->TestProvider) nil [test-reporter] trained-profiles)
        (is (= [prediction prediction] @prediction-atom))))))

(deftest profile->features-test
  (testing "it extracts a profile->feature vector map from a provider")
  (is (= [[:profile1 [1 2]]]
         (#'d/profile->features (->TestProvider) {:profile1 {{:keymap1 1} {:idx 0 :train-sample-complete? true} {:keymap2 2} {:idx 1 :train-sample-complete? true}}}))))

(deftest profile->score-test
  (with-redefs [m/scores (constantly [0.42])]
    (is (= [[:profile1 0.42]]
           (d/profile->score nil {:profile1 nil} {:profile1 nil})))))

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
