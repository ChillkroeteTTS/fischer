(ns fischer.models.multivariate-gaussian-model
  (:require [fischer.model :as m]
            [fischer.anomaly-detection :as ad]))

(defrecord MultivariateGaussianModel []
  m/Model
  (train [_ feature-vectors] (ad/multivariate-train feature-vectors))
  (scores [_ feature-vectors mu-and-sigma] (ad/multivariate-scores feature-vectors mu-and-sigma))
  (predict [_ scores epsylon] (ad/predict scores epsylon)))
