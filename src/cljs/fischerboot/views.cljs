(ns fischerboot.views
  (:require [re-frame.core :as rf]))


(defn hello [server]
  (let [server @(rf/subscribe [:fischer-ws])]
        [:p
         (str "Hello! You can find fischer under " server)]))

(defn ui []
  [hello])