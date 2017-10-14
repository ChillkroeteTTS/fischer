(ns conan.anomaly-detection
  (:require [incanter.stats :as stats]
            [clojure.core.matrix :as mat]
            [clojure.spec.alpha :as s]
            [clojure.spec.alpha :as s]
            [conan.utils :as utils]))

(s/def ::feature-vector mat/vec?)
(s/def ::feature-vectors (s/coll-of ::feature-vector))
(s/def ::mu number?)
(s/def ::multiv-mu (s/coll-of ::mu))
(s/def ::sigma number?)
(s/def ::multiv-sigma mat/matrix?)
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

(defn multivariate-train [feature-vectors]
  {:pre [(s/valid? ::feature-vectors feature-vectors)]}
  (let [m (count feature-vectors)
        mu (map stats/mean (utils/transpose feature-vectors))]
    {:mu    mu
     :sigma (stats/covariance feature-vectors)}))

(defn- gaussian-feature-distribution [[x {:keys [mu sigma]}]]
  (stats/pdf-normal x :mean mu :sd sigma))

(defn- multivariate-predict-one [feature-vector {:keys [mu sigma]}]
  (let [p (count feature-vector)
        denominator (Math/sqrt (* (Math/pow (* 2 Math/PI) p) (mat/det sigma)))
        x-mu (mat/sub feature-vector mu)
        exp-term (Math/exp (* -1/2 (mat/mmul (mat/transpose x-mu) (mat/inverse sigma) x-mu)))]
    (/ exp-term denominator))) ; TODO: Calc determinant once per prediction cycle


(defn- predict-one [feature-vector mu-and-sigma]
  {:pre [(s/valid? ::feature-vector feature-vector)
         (s/valid? ::mu-and-sigmas mu-and-sigma)]}
  (let [features-w-mu-a-s (partition 2 (interleave feature-vector mu-and-sigma))
        feature-distributions (map gaussian-feature-distribution features-w-mu-a-s)
        v (reduce * feature-distributions)]
    (reduce * feature-distributions)))

(defn covariance [feature-vectors mu]
  (let [sum-term (fn [feature-vector]
                   (let [x-mu (mat/sub feature-vector mu)
                         x-mu-as-matrix [x-mu]]
                     (mat/mmul (mat/transpose x-mu-as-matrix) x-mu-as-matrix)))
        m (count feature-vectors)]
    (mat/div (reduce mat/add (map sum-term feature-vectors))
             m)))

(defn multivariate-train [feature-vectors]
  {:pre [(s/valid? ::feature-vectors feature-vectors)]}
  (let [m (count feature-vectors)
        mu (mat/div (reduce mat/add feature-vectors)
                    m)]
    {:mu    mu
     :sigma (covariance feature-vectors mu)}))

(defn train [feature-vectors]
  {:pre  [(s/valid? ::feature-vectors feature-vectors)]
   :post [(s/valid? ::mu-and-sigmas %)]}
  (map mu-and-sigma (apply map vector feature-vectors)))

(defn multivariate-scores [feature-vectors mu-and-sigma]
  {:pre  [(s/valid? ::feature-vectors feature-vectors)
          (s/valid? ::multiv-mu (:mu mu-and-sigma))
          (s/valid? ::multiv-sigma (:sigma mu-and-sigma))]
   :post [(s/valid? ::scores %)]}
  (map #(multivariate-predict-one % mu-and-sigma) feature-vectors))

(defn scores [feature-vectors mu-and-sigma]
  {:pre  [(s/valid? ::feature-vectors feature-vectors)]
   :post [(s/valid? ::scores %)]}
  (map #(predict-one % mu-and-sigma) feature-vectors))

(defn predict [scores epsylon]
  {:pre  [(s/valid? ::scores scores)]
   :post [(s/valid? ::predictions %)]}
  (map #(< % epsylon) scores))