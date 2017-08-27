(ns conan.anomaly-detection
  (:require [incanter.stats :as stats]
            [clojure.spec.alpha :as s]
            [clojure.spec.alpha :as s]))
(s/def ::feature-vector (s/coll-of number?))
(s/def ::feature-vectors (s/coll-of ::feature-vector))
(s/def ::mu number?)
(s/def ::sigma number?)
(s/def ::mu-and-sigmas (s/coll-of (s/keys :req-un [::mu ::sigma])))
(s/def ::scores (s/coll-of number?))
(s/def ::predictions (s/coll-of (fn [v] (or (= true v) (= false v)))))
(s/def ::normalized-value (s/double-in :min 0.0 :max 1.0 :NaN? false :infinite? false))

(defn normalize-fn [min max value]
  (double (/ (- value min) (- max min))))

(defn normalize
  [values]
  {:pre  [(s/valid? (s/coll-of number? :min-count 2) values)]
   :post [(s/valid? (s/coll-of ::normalized-value) %)]}
  (let [min (apply min values)
        max (apply max values)
        range (- max min)]
    (map #(normalize-fn min max %) values)))

(defn- mu-and-sigma [val-per-feature]
  "val-per-feature is a vector of size m with one feature representation per line"
  (let [m (count val-per-feature)
        mu (/ (reduce + val-per-feature) m)]
    {:mu    mu
     :sigma (/ (reduce + (map #(Math/pow (- % mu) 2)
                              val-per-feature))
               m)}))

(defn- gaussian-feature-distribution [[x {:keys [mu sigma]}]]
  (stats/pdf-normal x :mean mu :sd sigma))

(defn- predict-one [feature-vector mu-and-sigma]
  {:pre [(s/valid? ::feature-vector feature-vector)
         (s/valid? ::mu-and-sigmas mu-and-sigma)]}
  (let [features-w-mu-a-s (partition 2 (interleave feature-vector mu-and-sigma))
        feature-distributions (map gaussian-feature-distribution features-w-mu-a-s)
        v (reduce * feature-distributions)]
    (reduce * feature-distributions)))

(defn train [feature-vectors]
  {:pre  [(s/valid? ::feature-vectors feature-vectors)]
   :post [(s/valid? ::mu-and-sigmas %)]}
  (map mu-and-sigma (apply map vector feature-vectors)))

(defn scores [feature-vectors mu-and-sigma]
  {:pre [(s/valid? ::feature-vectors feature-vectors)]
   :post [(s/valid? ::scores %)]}
  (map #(predict-one % mu-and-sigma) feature-vectors))

(defn predict [scores epsylon]
  {:pre  [(s/valid? ::scores scores)]
   :post [(s/valid? ::predictions %)]}
  (map #(< % epsylon) scores))