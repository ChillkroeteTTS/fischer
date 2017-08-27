(ns conan.utils
  (:require [clojure.spec.alpha :as s]
            [conan.anomaly-detection :as ad]))

(defmacro timed [& body]
  `(let [start-time# (System/currentTimeMillis)
         res# ~@body]
     {:took (- (System/currentTimeMillis) start-time#)
      :res  res#}))

(defn transpose [matrix]
  (apply map vector matrix))

(defn scores->file [file-path scores]
  (with-open [w (clojure.java.io/writer file-path :append true)]
    (.write w "---\n")
    (doseq [score scores] (.write w (str score "\n")))))

(defn predictions->console [predictions]
  (prn predictions))

(defn filtered-keyset [key->props key->feature]
  (select-keys key->feature (keys key->props)))

(defn sorted-kv-list [key->props key->feature]
  (reduce (fn [agg [k v]] (concat agg [[k v]])) []
          (sort-by (fn [[k v]] (:idx (get key->props k))) key->feature)))

(defn extract-bare-features [key->feature key->props]
  (->> key->feature
       (filtered-keyset key->props)
       (sorted-kv-list key->props)
       (map second)
       (transpose)))