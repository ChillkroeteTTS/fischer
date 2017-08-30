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
            [outpace.config :refer [defconfig!]]
            [clojure.spec.alpha :as s]))
(s/def ::profile (s/keys :req-un [::step-size ::days-back ::queries ::epsylon]))
(s/def ::profiles (s/map-of (constantly true) ::profile))

(defconfig! host)
(defconfig! port)
(defconfig! profiles)

(def prom-provider (prom/->PrometheusProvider host port profiles))

(defn conan-system [conf provider]
  (if (s/valid? ::profiles profiles)
    (cp/system-map :model-trainer (cp/using (gadt/new-gaussian-ad-trainer provider profiles) [])
                   :detector (cp/using (d/new-detector provider) [:model-trainer]))
    (do (log/error "Your profile config is not in the expected format. Details:\n" (s/explain ::profiles profiles))
        (System/exit 1))))

(defn -main [& argv]
  (cp/start (conan-system {} prom-provider)))