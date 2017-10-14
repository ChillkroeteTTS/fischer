(ns conan.anomaly-detection-test
  (:require [clojure.test :refer :all]
            [conan.core :refer :all]
            [conan.anomaly-detection :as ad]
            [clojure.core.matrix :as mat]
            [conan.utils :as utils]
            [incanter.stats :as stats]))

(defn close? [tolerance x y]
  (< (- x y) tolerance))

(deftest covariance-test
  (testing "returns the same matrix as incanter would"
    (let [feature-vectors [[4 80] [6 140] [10 220]]
          mu (mat/div (reduce mat/add feature-vectors) (count feature-vectors))]
      (is (= [[6.222222222222221
               142.2222222222222]
              [142.2222222222222
               3288.888888888889]]
             (ad/covariance feature-vectors mu))))))

(deftest multivariate-gaussian-test
  (let [mu+sigma {:mu    [50.0 100.0]
                  :sigma (mat/matrix [[858.5
                                       1720.947387513257]
                                      [1720.9473875132567
                                       3450.690734381145]])}]
    (is (= [0.0057804442239349435 0.0] (#'ad/multivariate-scores [[50 100]
                                                                  [100 50]] mu+sigma)))))

(deftest mu-and-sigma-test
  (testing "it trains a mu and a sigma for a single feature"
    (let [feature-values [50 49 51]
          res (#'ad/mu-and-sigma feature-values)]
      (prn res)
      (is (number? (:mu res)))
      (is (number? (:sigma res)))
      (is (= 50
             (:mu res)))
      (is (close? 0.001 (double (/ 2 3)) (:sigma res)))
      )))

(deftest train-test
  (testing "it trains for each feature a mu and a sigma"
    (let [features [[50 5]
                    [51 3]]
          res (ad/train features)]
      (prn res)
      (is (= 2
             (count res)))
      (is (= (/ (+ 50 51) 2)
             (:mu (first res)))))))

(def mus-and-sigmas [{:mu 101/2, :sigma 0.25} {:mu 4, :sigma 1.0}])
(deftest predict-one-test
  (testing "it predicts non anonamleous features as such"
    (is (not (< (#'ad/predict-one [50 4]
                  mus-and-sigmas) 0.02))))
  (testing "it predicts anomaleous features as such"
    (is (< (#'ad/predict-one [30 4]
             mus-and-sigmas) 0.02))
    (is (< (#'ad/predict-one [50 8]
             mus-and-sigmas) 0.02))))

(deftest scores-test
  (testing "it predicts multiple features"
    (is (= [0.08615711720739454
            0.0]
           (ad/scores [[50 4] [30 4]]
                      mus-and-sigmas)))))

(deftest predict-test
  (testing "it predicts multiple features"
    (is (= [false true]
           (ad/predict [0.08615711720739454
                        0.0]
                       0.02)))))

(deftest normalization-fn-test
  (testing "it normalizes a vector of values"
    (is (= [0.0 0.3 0.5 0.7 1.0]
           (ad/normalize [0 3 5 7 10]))))
  (testing "it should not accept a vector with < 2 elements"
    (is (thrown? AssertionError (ad/normalize [5])))))