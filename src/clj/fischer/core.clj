(ns fischer.core
  (:require [fischer.anomaly-detection :as ad]
            [fischer.components.detector :as d]
            [fischer.components.gaussian-ad-trainer :as gadt]
            [clj-time.core :as t]
            [com.stuartsierra.component :as cp]
            [clojure.tools.logging :as log]
            [fischer.models.gaussian-model :as gauss]
            [fischer.models.multivariate-gaussian-model :as mgauss]
            [fischer.prometheus-provider :as prom]
            [fischer.components.frontend :as fe]
            [outpace.config :refer [defconfig! defconfig]]
            [clojure.spec.alpha :as s]
            [fischer.reporter.console-reporter :as cr]
            [fischer.reporter.file-reporter :as fr]
            [fischer.reporter.prometheus-reporter :as promr]
            [fischer.reporter.buffered-channel-reporter :as bcr]
            [clojure.core.async :as async])
  (:gen-class))
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

(defn fischer-system [provider model profiles reporters handler-atom pred-buffer-ch]
  (cp/system-map :model-trainer (cp/using (gadt/new-gaussian-ad-trainer provider model profiles) [])
                 :detector (cp/using (d/new-detector provider model reporters) [:model-trainer])
                 :frontend (cp/using (fe/new-frontend handler-atom pred-buffer-ch) [:model-trainer])))

(def prom-provider (prom/->PrometheusProvider host port profiles))
(def gaussian-model (mgauss/->MultivariateGaussianModel))
(def profile->predictions-buffer (atom {}))

(defn reporters [reporter-confs handler-atom pred-buffer-ch]
  (let [configurable-reporters (mapv (fn [reporter-conf]
                                       (case (:type reporter-conf)
                                         :file (fr/->FileReporter (:file-path reporter-conf))
                                         :console (cr/->ConsoleReporter)
                                         :prometheus (promr/new-prometheus-reporter! handler-atom (:path reporter-conf))))
                                     reporter-confs)]
    (conj configurable-reporters (bcr/->BufferedChannelReporter pred-buffer-ch profile->predictions-buffer))))

(defn -main [& argv]
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (log/error ex "Uncaught exception on" (.getName thread)))))
  (let [validation (validate-config profiles reporters-configs)
        handler-atom (atom [])
        pred-buffer-ch (async/chan)
        reporters (reporters reporters-configs handler-atom pred-buffer-ch)]
    (if (:valid? validation)
      (cp/start (fischer-system prom-provider gaussian-model profiles reporters handler-atom pred-buffer-ch))
      (do
        (log/error "Your config is not in the expected format. Details:\n" (:explanation validation))
        (when (not debug) (System/exit 1))))))