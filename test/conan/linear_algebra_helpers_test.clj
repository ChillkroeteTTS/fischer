(ns conan.linear-algebra-helpers-test
  (:require [clojure.test :refer :all]
            [conan.linear-algebra-helpers :as la]))

(deftest v-op-test
  (is (= [3 7 11 15 19]
         (la/v-op + [1 3 5 7 9] [2 4 6 8 10])))
  (is (= [-4 0]
         (la/v-op - [-2 3] [2 3]))))

(deftest matmul-test
  (is (= [[1 2]
          [6 8]]
         (la/matmul
           [[1 0]
            [0 2]]
           [[1 2]
            [3 4]]))))

(deftest mat-el-wise-op-test
  (is (= [[1 2 3] [4 5 6]]
         (la/mat-el-wise-op [[1 2 3] [4 5 6]] +))))

(deftest mat+
  (is (= [[3 3] [3 3]]
         (la/mat+ [[1 2] [3 4]] [[2 1] [0 -1]]))))