(ns fischerboot.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [fischerboot.events :as events]
            [fischerboot.subs :as subs]
            [fischerboot.views :as views]))

(enable-console-print!)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn ^:export run []
  (rf/dispatch-sync [:initialize])
  (reagent/render [views/ui]
                  (js/document.getElementById "app")))

(run)
