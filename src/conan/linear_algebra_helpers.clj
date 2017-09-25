(ns conan.linear-algebra-helpers
  (:require [clojure.spec.alpha :as s]
            [conan.utils :as utils]))

(s/def ::vector (s/coll-of number?))
(s/def ::matrix (s/coll-of ::vector))

(defn v-op [op v1 v2]
  {:pre [(s/valid? ::vector v1) (s/valid? ::vector v2)] :post [(s/valid? ::vector %)]}
  (let [el-pairs (partition 2 (interleave v1 v2))]
    (map #(reduce (fn [agg pair] (op agg pair)) %) el-pairs)))

(defn mat-el-wise-op [X op]
  {:pre [(s/valid? ::matrix X)] :post [(s/valid? ::matrix %)]}
  (map (fn [xri] (map #(op %) xri)) X))

(defn matmul [X Y]
  {:pre [(s/valid? ::matrix Y) (s/valid? ::matrix X)] :post [s/valid? ::matrix]}
  (let [Y-trans (utils/transpose Y)]
    (map (fn [rxi] (map (fn [cyi] (reduce #(+ %1 %2)
                                          (v-op * rxi cyi)))
                        Y-trans))
         X)))


(defn mat+ [X Y]
  {:pre [(s/valid? ::matrix Y) (s/valid? ::matrix X)] :post [(s/valid? ::matrix %)]}
  (let [Xr-Yr-pairs (partition 2 (interleave X Y))]
    (map (fn [xr-yr-pair] (v-op + (first xr-yr-pair) (second xr-yr-pair))) Xr-Yr-pairs)))