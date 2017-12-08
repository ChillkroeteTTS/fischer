(ns fischerboot.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [fischerboot.events :as events]
            [fischerboot.subs :as subs]
            [fischerboot.views :as views]
            [fischerboot.ws :as ws]))

(enable-console-print!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn ^:export run []
  (rf/dispatch-sync [:initialize])
  (add-watch ws/chsk-state :state-change (fn [k r o n] (prn n) (rf/dispatch [:ws-state-change (:open? n)])))
  (reagent/render [views/ui]
                  (js/document.getElementById "app")))

(run)
