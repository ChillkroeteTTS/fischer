(ns fischerboot.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :fischer-ws
  (fn [db _]
    (:fischer-ws db)))
