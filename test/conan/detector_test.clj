(ns conan.detector-test
  (:require [clojure.test :refer :all]
            [conan.components.detector :as d]
            [clojure.java.io :as io]
            [conan.anomaly-detection :as ad]
            [conan.time-series-provider :as p]))

(def trained-profiles {:profile1 (atom {:key->props     {}
                                        :mus-and-sigmas [{:mu 101/2, :sigma 0.25} {:mu 4, :sigma 1.0}]
                                        :epsylon        0.2})})
;; TODO: EPSYLON should be part of profile configuration

(deftest detect-test
  (testing "it detects anomalies on each call"
    (with-redefs [p/prediction-data (constantly nil)
                  ad/scores (constantly [0.1])
                  ad/predict (constantly [false])]
      (let [score-atom (atom [])
            prediction-atom (atom [])
            sc-log-fn (fn [[profile score]] (swap! score-atom conj {profile score}))
            pr-log-fn (fn [[profile prediction]] (swap! prediction-atom conj {profile prediction}))]
        (d/detect nil sc-log-fn pr-log-fn trained-profiles)
        (d/detect nil sc-log-fn pr-log-fn trained-profiles)
        (is (= [{:profile1 0.1} {:profile1 0.1}] @score-atom))
        (is (= [{:profile1 false} {:profile1 false}] @prediction-atom))))))
