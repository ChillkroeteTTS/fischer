(ns conan.anomaly-detection-test
  (:require [clojure.test :refer :all]
            [conan.core :refer :all]
            [conan.anomaly-detection :as ad]))

(defn close? [tolerance x y]
  (< (- x y) tolerance))

(deftest v-op-test
  (is (= [3 7 11 15 19]
         (#'ad/v-op + [1 3 5 7 9] [2 4 6 8 10])))
  (is (= [-4 0]
         (#'ad/v-op - [-2 3] [2 3]))))

(deftest matmul-test
  (is (= [[1 2]
          [6 8]]
         (#'ad/matmul
           [[1 0]
            [0 2]]
           [[1 2]
            [3 4]]))))

(deftest mat-el-wise-op-test
  (is (= [[1 2 3] [4 5 6]]
         (#'ad/mat-el-wise-op [[1 2 3] [4 5 6]] +))))

(deftest mat+
  (is (= [[3 3] [3 3]]
         (#'ad/mat+ [[1 2] [3 4]] [[2 1] [0 -1]]))))

(deftest multivariate-mu-and-sigma-test
  (is (= {:mu    [50 5]
          :sigma [[(double (/ 2 3)) (double (/ 1 3))]
                  [(double (/ 1 3)) (double (/ 2 3))]]}
         (#'ad/multivariate-mu-and-sigma [[50 4]
                                          [49 5]
                                          [51 6]]))))

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