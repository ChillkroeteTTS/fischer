(ns fischer.reporter.file-reporter-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [fischer.reporter.file-reporter :as fr]
            [fischer.reporter.prediction-reporter :as r]
            [clj-time.core :as t]))

(def test-file-path "test-resources/predictions")

(deftest file-reporter-test
  (testing "it writes the predictions to a file"
    (with-redefs [t/now (constantly (t/date-time 2017 01 01 00 00 00))]
      (r/report (fr/->FileReporter test-file-path) {:profile1 {:e 0.02 :s 0.324 :p false}})
      (is (= "-------- 2017-01-01T00:00:00.000Z--------\n:profile1: 0.324 --(0.02)--> false\n"
             (slurp test-file-path)))
      (io/delete-file test-file-path))))
