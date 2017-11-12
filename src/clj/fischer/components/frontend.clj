(ns fischer.components.frontend
  (:require [com.stuartsierra.component :as cp]
            [ring.middleware.json :as rjson]
            [org.httpkit.server :as server]
            [outpace.config :refer [defconfig!]]
            [ring.util.response :as resp]
            [compojure.core :as cpj]
            [clojure.tools.logging :as log])
  (:import (org.httpkit.server HttpServer)))

(defconfig! frontend-host)
(defconfig! frontend-port)

(defn apply-to-vals [myMap myFun]
  (into {} (map (fn [[k v]] [k (if (map? v) (apply-to-vals v myFun) (myFun v))]) myMap)))

(defn model-trainer-info [model-trainer req]
  (resp/response @(:trained-profiles model-trainer)))

(defn merged-routes [handlers model-trainer]
  (->> (apply cpj/routes (conj handlers
                               (cpj/GET "/models" req (model-trainer-info model-trainer req))
                               (cpj/GET "/ws" req (model-trainer-info model-trainer req))))
       (rjson/wrap-json-response)))

(defrecord Frontend [handlers model-trainer]
  cp/Lifecycle
  (start [self]
    (log/info "start frontend with following pre-registered handler")
    (log/info @handlers)
    (let [shutdown-fn (server/run-server (merged-routes @handlers model-trainer)
                                    {:host frontend-host
                                     :port frontend-port})]
      (assoc self :shutdown-fn shutdown-fn)))
  (stop [self]
    (log/info "stop frontend...")
    ((:shutdown-fn self))
    (dissoc self :shutdown-fn)))

(defn new-frontend [handlers]
  (map->Frontend {:handlers handlers}))