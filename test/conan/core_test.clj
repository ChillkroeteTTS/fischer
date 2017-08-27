(ns conan.core-test
  (:require [conan.core :as c]
            [clojure.test :refer :all]
            [conan.time-series-provider :as p]
            [de.otto.tesla.system :as system]
            [conan.utils :as utils]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as cp]))

(defrecord TestProvider []
  p/TimeSeriesProvider
  (prediction-data [_] {:profile1 {{:keymap1 1} [50.0]
                                   {:keymap2 2} [4.0]}})
  (training-data [_] {:profile1 {{:keymap1 1} [50.0 51.0]
                                 {:keymap2 2} [5.0 3.0]}}))

(deftest core-test
  (testing "it detects an anomaly"
    (let [prediction-atom (atom [])
          prediction->atom-fn (fn [pred] (swap! prediction-atom #(conj % pred)))]
      (with-redefs [utils/predictions->console prediction->atom-fn
                    at/every (fn [_ fn _ _ _] (fn))
                    at/stop identity]
        (let [s (cp/start (c/conan-system {} (->TestProvider)))]
          (is (= [false]
                 (first @prediction-atom)))
          (cp/stop s))))))