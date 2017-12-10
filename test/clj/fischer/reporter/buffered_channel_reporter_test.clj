(ns fischer.reporter.buffered-channel-reporter-test
  (:require [clojure.test :refer :all]
            [fischer.reporter.buffered-channel-reporter :as bcr]
            [clojure.core.async :as async]
            [fischer.reporter.prediction-reporter :as r])
  (:import (clojure.lang PersistentQueue)))

(deftest add-prediction-test
  (testing "puts an element in the buffered atom"
    (let [out (async/chan)
          buffer-atom (atom {})
          bcr (bcr/->BufferedChannelReporter out buffer-atom)
          pred {:p true :e 0.2 :s 0.1}]
      (r/report bcr {:profile1 pred})
      (is (= {:profile1 (conj PersistentQueue/EMPTY pred)}
             @buffer-atom))))

  (testing "pops oldest entries from buffer if full"
    (with-redefs [bcr/max-queue-size 2]
      (let [out (async/chan)
            buffer-atom (atom {:profile1 (conj PersistentQueue/EMPTY 1 2)})
            bcr (bcr/->BufferedChannelReporter out buffer-atom)
            pred 3]
        (r/report bcr {:profile1 pred})
        (is (= {:profile1 (conj PersistentQueue/EMPTY 2 3)}
               @buffer-atom))))))

(deftest channel-reporting-test
  "prediction buffer is send over out channel"
  (let [out (async/chan)
        bcr (bcr/->BufferedChannelReporter out (atom {}))
        pred {:p true :e 0.2 :s 0.1}]
    (r/report bcr {:profile1 pred})
    (is (= {:profile1 (conj PersistentQueue/EMPTY pred)}
           (async/<!! out))))
  )