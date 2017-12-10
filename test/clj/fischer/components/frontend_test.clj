(ns fischer.components.frontend-test
  (:require [clojure.test :refer :all]
            [fischer.components.frontend :as fe]
            [clj-http.client :as client]
            [com.stuartsierra.component :as cp]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [compojure.core :as cpj]
            [ring.util.response :as resp]
            [clojure.core.async :as async]
            [clojure.string :as s])
  (:import (java.net ConnectException)
           (clojure.lang PersistentQueue)))

(deftest test-frontent-component
  (testing "it starts and stops of the server"
    (let [handler (atom [(cpj/GET "/" rsp (resp/response ""))])
          fe-cmp (cp/start (fe/->Frontend handler {} nil))
          shutdown-fn (:shutdown-fn fe-cmp)]
      (try
        (is (= 200
               (:status (client/get "http://127.0.0.1:8080"))))
        (cp/stop fe-cmp)
        (is (thrown? ConnectException (client/get "http://127.0.0.1:8080")))
        (finally
          (shutdown-fn)))))
  (testing "a prediction is pushed to the all ws clients"
    (let [sended-msgs (atom [])]
      (with-redefs [fe/chsk-send! #(swap! sended-msgs conj %2)
                    fe/connected-uids (atom {:any [:uid1]})]
        (let [handler (atom [])
              pred-buffer-ch (async/chan)
              fe-cmp (cp/start (fe/->Frontend handler {} pred-buffer-ch))
              shutdown-fn (:shutdown-fn fe-cmp)
              payload {:profile1 (conj PersistentQueue/EMPTY {:p true :e 0.2 :s 0.1})}]
          (try
            (async/>!! pred-buffer-ch payload)
            (Thread/sleep 10)
            (is (= [[:fischer/predictions-changed payload]] @sended-msgs))
            (finally
              (shutdown-fn))))))))

(deftest ws-test
  (testing "the ws endpoint works"
    (with-redefs [fe/handle_event (fn [[event data]] (s/upper-case (:test-str data)))]
      (let [response (atom "")
            msg {:event     [:test/test-event {:test-str "test"}]
                 :id        1
                 :?data     nil
                 :send-fn   nil
                 :?reply-fn #(reset! response %)
                 :uid       1
                 :ring-req  {}
                 :client-id 1}]
        (async/>!! fe/ch-chsk msg)
        (Thread/sleep 10)
        (is (= "TEST" @response))))))

(deftest info-endpoint
  (testing "it returns a json containing infos about the trained models"
    (let [model-data {:profile1 {:my "model-data"}}
          model-trainer {:trained-profiles (atom model-data)}
          response ((fe/merged-routes [] model-trainer) (mock/request :get "/models"))]
      (is (= model-data
             (json/read-str (:body response) :key-fn keyword))))))
