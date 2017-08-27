(ns conan.time-series-provider)

(defprotocol TimeSeriesProvider
  (training-data [_])
  (prediction-data [_]))