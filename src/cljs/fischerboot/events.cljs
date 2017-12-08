(ns fischerboot.events
  (:require [re-frame.core :as rf]))

(def initial_state {:fischer-ws {:server "localhost" :port 8080 :open? false}})

(rf/reg-event-db
  :initialize
  (fn [_ _]
    initial_state))

(rf/reg-event-db
  :ws-state-change
  (fn [db [_ open?]]
    (update-in db [:fischer-ws :open?] (constantly open?))))
