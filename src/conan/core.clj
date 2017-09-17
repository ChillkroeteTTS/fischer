(ns conan.core
  (:require [conan.anomaly-detection :as ad]
            [conan.components.detector :as d]
            [conan.components.gaussian-ad-trainer :as gadt]
            [clj-time.core :as t]
            [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [conan.models.GaussianModel :as gauss]
            [conan.prometheus-provider :as prom]
            [conan.components.frontend :as fe]
            [outpace.config :refer [defconfig! defconfig]]
            [clojure.spec.alpha :as s]
            [conan.reporter.console-reporter :as cr]
            [conan.reporter.file-reporter :as fr]))
(s/def ::profile (s/keys :req-un [::step-size ::days-back ::queries ::epsylon]))
(s/def ::profiles (s/map-of (constantly true) ::profile))
(s/def ::reporters (s/coll-of (s/keys :req-un [::type])))

(defconfig debug false)
(defconfig! host)
(defconfig! port)
(defconfig! profiles)
(defconfig! reporters-configs)

(defn validate-config [profiles reporters]
  (let [valid? (and (s/valid? ::profiles profiles) (s/valid? ::reporters reporters))]
    {:valid?      valid?
     :explanation (str (s/explain-str ::profiles profiles) "\n"
                       (s/explain-str ::reporters reporters))}))

(defn reporters [reporter-confs]
  (map (fn [reporter-conf]
         (case (:type reporter-conf)
           :file (fr/->FileReporter (:file-path reporter-conf))
           :console (cr/->ConsoleReporter)))
       reporter-confs))

(defn conan-system [provider model profiles reporters-configs]
  (let [validation (validate-config profiles reporters-configs)]
    (if (:valid? validation)
      (cp/system-map :model-trainer (cp/using (gadt/new-gaussian-ad-trainer provider model profiles) [])
                     :detector (cp/using (d/new-detector provider model (reporters reporters-configs)) [:model-trainer])
                     :frontend (cp/using (fe/new-frontend) [:model-trainer]))
      (do
        (log/error "Your config is not in the expected format. Details:\n" (:explanation validation))
        (when (not debug) (System/exit 1))))))

(def prom-provider (prom/->PrometheusProvider host port profiles))
(def gaussian-model (gauss/->GaussianModel))

(defn -main [& argv]
  (cp/start (conan-system prom-provider gaussian-model profiles reporters-configs)))