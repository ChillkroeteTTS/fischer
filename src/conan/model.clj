(ns conan.model)

(defprotocol Model
  (train [_ feature-vectors])
  (scores [_ feature-vectors mu-and-sigma])
  (predict [_ scores epsylon]))