(ns conan.core
  (:require [conan.anomaly-detection :as ad]
            [conan.components.detector :as d]
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
                 :detector (cp/using (d/new-detector provider) [:model-trainer])))

(defn -main [& argv]
  (cp/start (conan-system {} prom-provider)))