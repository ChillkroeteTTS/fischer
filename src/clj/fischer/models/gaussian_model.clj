(ns fischer.models.gaussian-model
  (:require [fischer.model :as m]
            [fischer.anomaly-detection :as ad]))

(defrecord GaussianModel []
  m/Model
  (train [_ feature-vectors] (ad/train feature-vectors))
  (scores [_ feature-vectors mu-and-sigma] (ad/scores feature-vectors mu-and-sigma))
  (predict [_ scores epsylon] (ad/predict scores epsylon)))