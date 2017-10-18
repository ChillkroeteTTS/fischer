(ns fischer.utils
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.core.matrix :as mat]))

(defmacro timed [& body]
  `(let [start-time# (System/currentTimeMillis)
         res# ~@body]
     {:took (- (System/currentTimeMillis) start-time#)
      :res  res#}))

(defn transpose [matrix]
  (apply map vector matrix))

(defn filtered-keyset [key->props key->feature]
  (let [complete-props (into {} (filter (fn [[k props]] (:train-sample-complete? props)) key->props))]
    (select-keys key->feature (keys complete-props))))

(defn sorted-kv-list [key->props key->feature]
  (reduce (fn [agg [k v]] (concat agg [[k v]])) []
          (sort-by (fn [[k v]] (:idx (get key->props k))) key->feature)))

(defn extract-bare-features [key->feature key->props]
  (->> key->feature
       (filtered-keyset key->props)
       (sorted-kv-list key->props)
       (map second)
       (mat/transpose)))

(defn exc-logger [fn]
  (try
    (fn)
    (catch Exception e
      (log/error e))))