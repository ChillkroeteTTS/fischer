(ns conan.reporter.prediction-reporter)

(defprotocol PredictionReporter
  (report [_ profile->prediction]))