(ns conan.prometheus-detector-test
  (:require [clojure.test :refer :all]
            [conan.components.prometheus-detector :as pd]
            [clojure.java.io :as io]
            [conan.anomaly-detection :as ad]
            [conan.time-series-provider :as p]))

(def mus-and-sigmas (atom [{:mu 101/2, :sigma 0.25} {:mu 4, :sigma 1.0}]))

(deftest detect-test
  (testing "it detects anomalies"
    (with-redefs [p/prediction-data (constantly nil)
                  ad/scores (constantly [0.1 0.3])
                  ad/predict (constantly [false true])]
      (let [score-atom (atom [])
            prediction-atom (atom [])
            sc-log-fn #(swap! score-atom concat %)
            pr-log-fn #(swap! prediction-atom concat %)]
        (pd/detect nil sc-log-fn pr-log-fn mus-and-sigmas (atom {}) 0.2)
        (pd/detect nil sc-log-fn pr-log-fn mus-and-sigmas (atom {}) 0.2)
        (is (= [0.1 0.3 0.1 0.3] @score-atom))
        (is (= [false true false true] @prediction-atom))))))
