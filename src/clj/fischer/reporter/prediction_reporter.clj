(ns fischer.reporter.prediction-reporter)

(defprotocol PredictionReporter
  (report [_ profile->prediction]))