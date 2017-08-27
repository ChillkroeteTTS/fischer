(ns conan.components.gaussian-ad-trainer-test
  (:require [clojure.test :refer :all]
            [conan.components.gaussian-ad-trainer :as gadt]))


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