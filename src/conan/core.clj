(ns conan.core
  (:require [conan.anomaly-detection :as ad]
            [conan.components.prometheus-detector :as pd]
            [conan.components.gaussian-ad-trainer :as gadt]
            [de.otto.tesla.system :as system]
            [clj-time.core :as t]
            [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.scheduler :as sched]
            [overtone.at-at :as at]
            [conan.utils :as utils]
            [conan.prometheus-provider :as prom]
            [outpace.config :refer [defconfig!]]))

(defconfig! host)
(defconfig! port)
(defconfig! prometheus-provider-config)

(def prom-provider (prom/->PrometheusProvider host port prometheus-provider-config))

(defn conan-system [conf provider]
  (cp/system-map :model-trainer (cp/using (gadt/new-gaussian-ad-trainer provider) [])
                 :detector (cp/using (pd/new-prometheus-detector provider) [:model-trainer])))

(defn -main [& argv]
  (cp/start (conan-system {} prom-provider)))