(ns related.core
  (:require [clojure.java.io :as io]
    #_[cheshire.core :as json]
    #_[jsonista.core :as j])
  (:import (com.google.gson Gson)
           (java.lang.reflect Array))
  (:gen-class))

(def ^:const input-file "../posts.json")
(def ^:const output-file "../related_posts_clj.json")

;; TODO: use deftype or class instance(?), faster json lib mapping to array of objects
(deftype Post [^String _id tags title])
(deftype PostRelated [^String _id tags related])


(defn -main []
  (try
    (let [posts             (.fromJson (Gson.)
                                       (slurp (io/file input-file))
                                       (class (make-array Post 0)))

          t1                (System/currentTimeMillis)

          n                 (alength posts)

          tag-map           (loop [i (int 0) res {}]
                              (if (= i n)
                                res
                                (let [post ^Post (Array/get posts i)
                                      res  (reduce (fn [res tag]
                                                     (update res tag (fn [v]
                                                                       (if (some? v)
                                                                         (conj! v i)
                                                                         (transient [i])))))
                                                   res
                                                   (.tags post))]
                                  (recur (inc i) res))))

          tag-map           (->> tag-map
                                 (mapv (fn [[k v]]
                                         [k (persistent! v)]))
                                 (into {}))

          tagged-post-count (Array/newInstance Integer/TYPE n)
          results           (Array/newInstance PostRelated n)

          _                 (loop [post-idx (int 0)]
                              (if (< post-idx n)
                                (let [post ^Post (Array/get posts post-idx)
                                      top5 (Array/newInstance Integer/TYPE 10)]
                                  (java.util.Arrays/fill tagged-post-count 0)
                                  (doseq [tag (.tags post)
                                          idx (tag-map tag)]
                                    (Array/setInt tagged-post-count idx (inc (Array/getInt tagged-post-count idx))))

                                  (Array/setInt tagged-post-count post-idx 0)

                                  (loop [i        (int 0)
                                         min-tags (int 0)]
                                    (if (< i n)
                                      (let [cnt (Array/getInt tagged-post-count i)]
                                        (if (> cnt min-tags)
                                          (let [up (loop [upper-bound (int 6)]
                                                     (if-not (and (>= upper-bound 0)
                                                                  (> cnt (Array/getInt top5 upper-bound)))
                                                       upper-bound
                                                       (recur (- upper-bound 2))))]
                                            (if (< up 6)
                                              (System/arraycopy top5 (+ 2 up) top5 (+ 4 up) (- 6 up)))
                                            (Array/setInt top5 (+ up 2) cnt)
                                            (Array/setInt top5 (+ up 3) i)
                                            (recur (inc i) (Array/getInt top5 8)))
                                          (recur (inc i) min-tags)))))

                                  (Array/set results post-idx
                                             (PostRelated. (._id post) (.tags post)
                                                           (->> (range 1 10 2)
                                                                (mapv #(Array/get posts (Array/getInt top5 %))))))

                                  (recur (inc post-idx)))))

          t2                (System/currentTimeMillis)]

      (println (format "Processing time (w/o IO): %sms" (- t2 t1)))
      (spit (io/file output-file) (.toJson (Gson.) results)))

    (catch Exception e (prn e))))