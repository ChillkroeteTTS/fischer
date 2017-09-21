(ns conan.components.frontend
  (:require [com.stuartsierra.component :as cp]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as rjson]
            [outpace.config :refer [defconfig!]]
            [ring.util.response :as resp]
            [compojure.core :as cpj]
            [clojure.tools.logging :as log])
  (:import (org.eclipse.jetty.server Server)))

(defconfig! frontend-host)
(defconfig! frontend-port)

(defn model-trainer-info [model-trainer req]
  (resp/response @(:trained-profiles model-trainer)))

(defn merged-routes [handlers model-trainer]
  (->> (apply cpj/routes (conj handlers (cpj/GET "/models" req (model-trainer-info model-trainer req))))
       (rjson/wrap-json-response)))

(defrecord Frontend [handlers model-trainer]
  cp/Lifecycle
  (start [self]
    (log/info "start frontend with following pre-registered handler")
    (log/info @handlers)
    (let [server (jetty/run-jetty (merged-routes @handlers model-trainer)
                                  {:host  frontend-host
                                   :port  frontend-port
                                   :join? false})]
      (assoc self :server server)))
  (stop [self]
    (log/info "stop frontend...")
    (.stop ^Server (:server self))
    (dissoc self :server)))

(defn new-frontend [handlers]
  (map->Frontend {:handlers handlers}))