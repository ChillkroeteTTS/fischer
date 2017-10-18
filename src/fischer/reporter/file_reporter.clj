(ns fischer.reporter.file-reporter
  (:require [fischer.reporter.prediction-reporter :as r]
            [clj-time.core :as t]))

(defrecord FileReporter [file-path]
  r/PredictionReporter
  (report [self profile->prediction]
    (with-open [w (clojure.java.io/writer file-path :append true)]
      (.write w (str "-------- " (str (t/now)) "--------\n"))
      (doseq [[profile prediction] profile->prediction]
        (.write w (str profile ": " (:s prediction) " --(" (:e prediction) ")--> " (:p prediction) "\n"))))))