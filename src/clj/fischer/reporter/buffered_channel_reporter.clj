(ns fischer.reporter.buffered-channel-reporter
  (:require [fischer.reporter.prediction-reporter :as r]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [outpace.config :refer [defconfig]])
  (:import (clojure.lang PersistentQueue)))

(defconfig max-queue-size 20)

(defn buffered-conj [buffered-queue prediction]
  (if (< (count buffered-queue) max-queue-size)
    (conj buffered-queue prediction)
    (conj (pop buffered-queue) prediction)))

(defn update-buffered-queue [old-queue prediction]
  (let [buffered-queue (or old-queue PersistentQueue/EMPTY)]
    (buffered-conj buffered-queue prediction)))

(defn report-to-channel [ch profile->buffer]
  (when (not (async/put! ch profile->buffer))
    (log/error "Couldn't report predictions because channel is closed.")))

(defrecord BufferedChannelReporter [ch profile->buffer-atom]
  r/PredictionReporter
  (report [self profile->prediction]
    (doseq [[profile prediction] profile->prediction]
      (swap! profile->buffer-atom #(update % profile update-buffered-queue prediction)))
    (report-to-channel ch @profile->buffer-atom)))
