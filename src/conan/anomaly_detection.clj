(ns conan.anomaly-detection
  (:require [incanter.stats :as stats]
            [clojure.spec.alpha :as s]
            [clojure.spec.alpha :as s]
            [conan.utils :as utils]
            [conan.linear-algebra-helpers :as la]))

(s/def ::feature-vector :conan.linear-algebra-helpers/vector)
(s/def ::feature-vectors (s/coll-of ::feature-vector))
(s/def ::mu number?)
(s/def ::multiv-mu (s/coll-of ::mu))
(s/def ::sigma number?)
(s/def ::mu-and-sigmas (s/coll-of (s/keys :req-un [::mu ::sigma])))
(s/def ::multiv-mu-and-sigmas (s/keys :req-un [::mu ::sigma]))
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

(defn- covariance-part-mat [x mu]
  {:pre [(s/valid? ::feature-vector x) (s/valid? ::multiv-mu mu)] :post [(s/valid? :conan.linear-algebra-helpers/matrix %)]}
  (let [x-mu (la/v-op - x mu)]
    (la/matmul (utils/transpose [x-mu]) [x-mu])))


(defn- multivariate-mu-and-sigma [feature-vectors]
  {:pre [(s/valid? :conan.linear-algebra-helpers/matrix feature-vectors)]}
  (let [m (count feature-vectors)
        mu (map #(/ % m) (reduce #(la/v-op + %1 %2) feature-vectors))
        cov-part-mats (map #(covariance-part-mat % mu) feature-vectors)
        _ (prn cov-part-mats)
        _ (prn (reduce #(la/mat+ %1 %2) cov-part-mats))]
    {:mu    mu
     :sigma (la/mat-el-wise-op (reduce #(la/mat+ %1 %2) cov-part-mats) #(double (/ % m)))}))

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
  {:pre  [(s/valid? ::feature-vectors feature-vectors)]
   :post [(s/valid? ::scores %)]}
  (map #(predict-one % mu-and-sigma) feature-vectors))

(defn predict [scores epsylon]
  {:pre  [(s/valid? ::scores scores)]
   :post [(s/valid? ::predictions %)]}
  (map #(< % epsylon) scores))