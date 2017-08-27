(ns conan.prometheus-provider
  (:require [clj-time.core :as time]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as cs]
            [conan.anomaly-detection :as ad]
            [conan.utils :as u]
            [clojure.string :as s]
            [conan.time-series-provider :as p]
            [com.stuartsierra.component :as cp]))

(defonce day-formatter (f/formatter "yyyy-MM-dd"))
(defn base [host port]
  (format "%s:%s" host port))

(defn- prom-request
  ([host port endpoint query]
   (prom-request host port endpoint query {}))
  ([host port endpoint query add-query-params]
   (let [params (assoc add-query-params "query" query)
         resp (client/get (str (base host port) "/api/v1/" (name endpoint))
                          {:query-params params})]
     (json/read-str (:body resp) :key-fn keyword))))

(defn- prom-data-now [host port query]
  (prom-request host port :query query))

(defn- prom-data-last-n-days [host port query date last-n-days step-size]
  (let [date-str (str (f/unparse day-formatter date) "T00:00:01.781Z")
        prev-day-str (str (f/unparse day-formatter (t/minus date (t/days last-n-days))) "T00:00:01.781Z")]
    (prom-request host port :query_range query {"start" prev-day-str
                                                "end"   date-str
                                                "step"  step-size})))

(defn- feature->ts+values [key-list value-key result]
  (let [keymap (select-keys (:metric result) key-list)
        vals (->> (get result value-key)
                  ((fn [res] (if (= :value value-key) [res] res)))
                  (map second)
                  (map (fn [val] (case val
                                   ("+Inf" "NaN") nil
                                   (Float/parseFloat val)))))]
    [keymap vals]))

(defn- featurize-prom-response [resp value-key]
  (->> (get-in resp [:data :result])
       (map #(feature->ts+values [:__name__ :method :path :rc :stage] value-key %))
       (reduce (fn [aggmap [k v]] (if (contains? aggmap k) (update aggmap k #(concat %1 v)) (conj aggmap [k v])))
               {})))

(defn- pmap-merge-reduce [fn collection]
  (reduce merge (pmap fn collection)))

(defn- prediction-data-results [host port {:keys [queries]}]
  (pmap-merge-reduce #(some-> (prom-data-now host port %)
                              (featurize-prom-response :value))
                     queries))

(defn- profile-results [host port {:keys [queries days-back step-size]}]
  (let [query-fn (fn [query] (some-> (prom-data-last-n-days host port query (t/now) days-back step-size)
                                     (featurize-prom-response :values)))
        merged-query-results (pmap-merge-reduce query-fn queries)]
    merged-query-results))


(defrecord PrometheusProvider [host port config]
  p/TimeSeriesProvider
  (training-data [_]
    (into {} (map (fn [[profile-key config]] [profile-key (profile-results host port config)]) config)))
  (prediction-data [_]
    (into {} (map (fn [[profile-key config]] [profile-key (prediction-data-results host port config)]) config))))

