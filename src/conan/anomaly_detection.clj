(ns conan.anomaly-detection
  (:require [incanter.stats :as stats]
            [clojure.spec.alpha :as s]
            [clojure.spec.alpha :as s]
            [conan.utils :as utils]))
(s/def ::vector (s/coll-of number?))
(s/def ::matrix (s/coll-of ::vector))
(s/def ::feature-vector ::vector)
(s/def ::feature-vectors ::matrix)
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

(defn- v-op [op v1 v2]
  {:pre [(s/valid? ::feature-vector v1) (s/valid? ::feature-vector v2)] :post [(s/valid? ::feature-vector %)]}
  (let [el-pairs (partition 2 (interleave v1 v2))]
    (map #(reduce (fn [agg pair] (op agg pair)) %) el-pairs)))

(defn- mat-el-wise-op [X op]
  {:pre [(s/valid? ::matrix X)] :post [(s/valid? ::matrix %)]}
  (map (fn [xri] (map #(op %) xri)) X))

(defn- matmul [X Y]
  {:pre [(s/valid? ::matrix Y) (s/valid? ::matrix X)] :post [s/valid? ::matrix]}
  (let [Y-trans (utils/transpose Y)]
    (map (fn [rxi] (map (fn [cyi] (reduce #(+ %1 %2)
                                          (v-op * rxi cyi)))
                        Y-trans))
         X)))

(defn- covariance-part-mat [x mu]
  {:pre [(s/valid? ::feature-vector x) (s/valid? ::multiv-mu mu)] :post [(s/valid? ::matrix %)]}
  (let [x-mu (v-op - x mu)]
    (matmul (utils/transpose [x-mu]) [x-mu])))

(defn- mat+ [X Y]
  {:pre [(s/valid? ::matrix Y) (s/valid? ::matrix X)] :post [(s/valid? ::matrix %)]}
   (let [Xr-Yr-pairs (partition 2 (interleave X Y))]
     (map (fn [xr-yr-pair] (v-op + (first xr-yr-pair) (second xr-yr-pair))) Xr-Yr-pairs)))

(defn- multivariate-mu-and-sigma [feature-vectors]
  {:pre [(s/valid? ::matrix feature-vectors)]}
  (let [m (count feature-vectors)
        mu (map #(/ % m) (reduce #(v-op + %1 %2) feature-vectors))
        cov-part-mats (map #(covariance-part-mat % mu) feature-vectors)
        _ (prn cov-part-mats)
        _ (prn (reduce #(mat+ %1 %2) cov-part-mats))]
    {:mu    mu
     :sigma (mat-el-wise-op (reduce #(mat+ %1 %2) cov-part-mats) #(double (/ % m)))}))

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