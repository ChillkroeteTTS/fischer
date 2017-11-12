(ns fischerboot.events
  (:require [re-frame.core :as rf]))

(def initial_state {:fischer-ws {:server "localhost" :port 8080}})

(rf/reg-event-db
  :initialize
  (fn [_ _]
    initial_state))
