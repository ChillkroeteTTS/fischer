(ns conan.components.frontend-test
  (:require [clojure.test :refer :all]
            [conan.components.frontend :as fe]
            [clj-http.client :as client]
            [com.stuartsierra.component :as cp]
            [ring.mock.request :as mock]
            [clojure.data.json :as json])
  (:import (java.net ConnectException)))

(deftest test-frontent-component
  (testing "it starts and stops of the server"
    (let [fe-cmp (cp/start (fe/->Frontend {}))
          server (:server fe-cmp)]
      (try
        (is (= 200
               (:status (client/get "http://127.0.0.1:8080"))))
        (cp/stop fe-cmp)
        (is (thrown? ConnectException (client/get "http://127.0.0.1:8080")))
        (finally
          (.stop server))))))

(deftest info-endpoint
  (testing "it returns a json containing infos about the trained models"
    (let [model-data {:profile1 {:my "model-data"}}
          model-trainer {:trained-profiles (atom model-data)}
          response ((fe/routes model-trainer) (mock/request :get "/"))]
      (is (= model-data
             (json/read-str (:body response) :key-fn keyword))))))
