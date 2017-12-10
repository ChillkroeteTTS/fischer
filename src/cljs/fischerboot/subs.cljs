(ns fischerboot.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :fischer-ws
  (fn [db _]
    (:fischer-ws db)))

(rf/reg-sub
  :profile->predictions
  (fn [db _]
    (:profile->predictions db)))
