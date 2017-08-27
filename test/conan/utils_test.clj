(ns conan.utils-test
  (:require [clojure.test :refer :all]
            [conan.utils :as utils]
            [clojure.java.io :as io]))


(deftest scores->file-test
  (let [file-path "test-resources/testscores"]
    (utils/scores->file file-path [0.2 0.43 0.5])
    (is (= "---\n0.2\n0.43\n0.5\n"
           (slurp file-path)))
    (io/delete-file file-path)))

(deftest sorted-keyset-test
  (is (= [[{:key1 :key1} :val1]
          [{:key2 :key2} :val2]]
         (utils/sorted-kv-list {{:key1 :key1} 0 {:key2 :key2} 1}
                               {{:key1 :key1} :val1 {:key2 :key2} :val2}))))

(def X-trans-map {:key1 [1 2 3]
                 :key2 [4 5 6]})
(def key->props {:key1 {:idx 0}
                 :key2 {:idx 1}})
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
           (utils/extract-bare-features (into (sorted-map) (reverse X-trans-map)) key->props)))))