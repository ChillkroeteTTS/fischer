(ns fischer.prom-test
  (:require [clojure.test :refer :all]
            [fischer.core :refer :all]
            [fischer.prometheus-provider :as prom]
            [clj-time.core :as t]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [fischer.time-series-provider :as p]
            [clojure.string :as s]))

(defn test-prom-response [metric-name vals]
  {"status" nil
   "data"   {"resultType" nil
             "result"     [{"metric" {"__name__" metric-name,
                                      "instance" "slave1",
                                      "job"      "job-A"}
                            "values" [[1.500854401781E9 (nth vals 0)]
                                      [1.500876001781E9 (nth vals 1)]]}
                           {"metric" {"__name__" metric-name,
                                      "instance" "slave2",
                                      "job"      "job-A"}
                            "values" [[1.500854401781E9 (nth vals 2)]
                                      [1.500876001781E9 (nth vals 3)]]}]}})


(deftest prom-request-test
  (testing "it passes the correct params to the request"
    (let [params-a (atom [])]
      (with-redefs [client/get (fn [& params] (swap! params-a concat params) {:body "{}"})]
        (#'prom/prom-request "myhost" 8080 :thisendpoint "sum(rate(mymetric{job='myjob',stage='mystage'}[1m]))")
        (is (= ["myhost:8080/api/v1/thisendpoint"
                {:query-params
                 {"query" "sum(rate(mymetric{job='myjob',stage='mystage'}[1m]))"}}]
               @params-a))))))

(deftest feature->ts+values-test
  (testing "it description"
    (is (= [{:__name__ "http_duration_in_s_count"} [10.0 21.0 115.0 229.0 279.0]]
           (#'prom/feature->ts+values [:__name__]
             :values
             {:metric {:__name__ "http_duration_in_s_count",
                       :app      "morbo.tesla.live.lhotse.ov.otto.de",
                       :instance "mesos-agent-live-763949.lhotse.ov.otto.de:31459",
                       :job      "morbo",
                       :method   ":get",
                       :path     "/tesla-morbo/exactag",
                       :rc       "400",
                       :stage    "live"}
              :values [[1.500854401781E9 "10"]
                       [1.500876001781E9 "21"]
                       [1.500897601781E9 "115"]
                       [1.500919201781E9 "229"]
                       [1.500940801781E9 "279"]]})))))

(deftest profile-results-test
  (testing "it executes all queries and returns a merged X-trans map"
    (let [result-query-B {:data {:result [{:metric {:__name__ :resC}
                                           :values [[0 "7.0"] [1 "8.0"] [2 "9.0"]]}]}}
          result-query-A {:data {:result [{:metric {:__name__ :resA}
                                           :values [[0 "1.0"] [1 "2.0"] [2 "3.0"]]}
                                          {:metric {:__name__ :resB}
                                           :values [[0 "4.0"] [1 "5.0"] [2 "6.0"]]}]}}]
      (with-redefs [prom/prom-data-last-n-days (fn [_ _ query _ _ _] (if (= query "queryA") result-query-A
                                                                                            result-query-B))]
        (is (= {{:__name__ :resA} [1.0 2.0 3.0]
                {:__name__ :resB} [4.0 5.0 6.0]
                {:__name__ :resC} [7.0 8.0 9.0]}
               (#'prom/profile-results nil nil {:step-size "1h"
                                                :days-back 1
                                                :queries   ["queryA" "queryB"]})))))))

(deftest train-data-for-job-test
  (with-redefs [client/get (fn [_ opts] {:body (json/write-str (if (s/includes? (get-in opts [:query-params "query"]) "query1")
                                                                 (test-prom-response "metric1" ["1" "2" "3" "4"])
                                                                 (test-prom-response "metric2" ["5" "6" "7" "8"])))})]
    (let [config {:profile1 {:step-size "1h"
                             :days-back 1
                             :queries   ["query1"]}
                  :profile2 {:step-size "1h"
                             :days-back 2
                             :queries   ["query2"]}}]
      (is (= {:profile1 {{:__name__ "metric1"} [1.0 2.0 3.0 4.0]}
              :profile2 {{:__name__ "metric2"} [5.0 6.0 7.0 8.0]}}
             (p/training-data (prom/->PrometheusProvider nil nil config)))))))
