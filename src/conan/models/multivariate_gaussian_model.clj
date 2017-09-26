(ns conan.models.multivariate-gaussian-model
  (:require [conan.model :as m]
            [conan.anomaly-detection :as ad]))

(defrecord MultivariateGaussianModel []
  m/Model
  (train [_ feature-vectors] (ad/multivariate-mu-and-sigma feature-vectors))
  (scores [_ feature-vectors mu-and-sigma] (ad/multivariate-scores feature-vectors mu-and-sigma))
  (predict [_ scores epsylon] (ad/predict scores epsylon)))
