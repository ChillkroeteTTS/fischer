(ns fischerboot.ws-reframe-interop
  (:require [re-frame.core :as rf]))

(defn handle-push-event [id payload]
  (case id
    :fischer/predictions-changed (rf/dispatch [:predictions-change payload])))
