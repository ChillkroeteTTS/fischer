(ns conan.reporter.PrometheusReporterTest
  (:require [clojure.test :refer :all]
            [conan.reporter.prometheus-reporter :as promr]
            [conan.reporter.prediction-reporter :as r]
            [de.otto.goo.goo :as goo]
            [ring.mock.request :as mock]
            [clojure.string :as s]))

(deftest PrometheusReporter-test
  (testing "it sets the anomaly detected metric to the correct value"
    (r/report (promr/->PrometheusReporter) {:profile1 {:p 1.0
                                                       :s 0.01
                                                       :e 0.02}
                                            :profile2 {:p 0.0
                                                       :s 0.03
                                                       :e 0.02}})
    (is (= 1.0 (.get ((goo/snapshot) :conan/anomaly-detected {:profile "profile1"}))))
    (is (= 0.0 (.get ((goo/snapshot) :conan/anomaly-detected {:profile "profile2"}))))))

(deftest prom-handler-test
  (r/report (promr/->PrometheusReporter) {:testprofile {:p 1.0 :s 0.01 :e 0.02}})
  (let [response ((promr/prom-handler "/metrics") (mock/request :get "/metrics"))]
    (is (s/includes? (:body response) "testprofile"))))