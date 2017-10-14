(ns conan.utils-test
  (:require [clojure.test :refer :all]
            [conan.utils :as utils]
            [clojure.java.io :as io]))

(deftest sorted-keyset-test
  (is (= [[{:key1 :key1} :val1]
          [{:key2 :key2} :val2]]
         (utils/sorted-kv-list {{:key1 :key1} 0 {:key2 :key2} 1}
                               {{:key1 :key1} :val1 {:key2 :key2} :val2}))))

(def X-trans-map {:key1 [1 2 3]
                  :key2 [4 5 6]
                  :key3 [7 8 9]})
(def key->props {:key1 {:idx 0 :train-sample-complete? true}
                 :key2 {:idx 1 :train-sample-complete? true}
                 :key3 {:idx 3 :train-sample-complete? false}})
(deftest extract-bare-features-test
  (testing "if it extracts simple features"
    (is (= [[1 4]
            [2 5]
            [3 6]]
           (utils/extract-bare-features X-trans-map key->props))))

  (testing "if it extracts features in the correct order"
    (is (= [[1 4]
            [2 5]
            [3 6]]
           (utils/extract-bare-features (into (sorted-map) (reverse X-trans-map)) key->props))))

  (testing "if it removes features with incomplete training sample"
    (is (= [[1 4]
            [2 5]
            [3 6]]
           (utils/extract-bare-features (into (sorted-map) (reverse X-trans-map)) key->props)))))