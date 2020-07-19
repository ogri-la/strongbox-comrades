(ns strongbox-comrades.utils
  (:require
   [cuerdas.core :as str]))

(defn in?
  [v coll]
  ((complement not) (some #{v} coll)))

(def format str/format)

(defn -log
  [level & msg-list]
  (let [levels {:debug js/console.log
                :info js/console.info
                :warn js/console.warn
                :error js/console.error
                :spy js/console.info}
        func (get levels level)
        lede (format "[%s]" (name level))
        args (mapv pr-str msg-list)]
    (when func
      (apply func lede args)))
  nil)

(def debug (partial -log :debug))
(def info (partial -log :info))
(def warn (partial -log :warn))
(def error (partial -log :error))

(defn spy
  [x]
  (-log :spy x)
  x)

(defn kv-map
  [f coll]
  (into {} (map (fn [[k v]]
                  (f k v)) coll)))

(defn kv-filter
  [f coll]
  (into {} (filter (fn [[k v]]
                     (f k v))
                   coll)))

;; https://stackoverflow.com/questions/27130961/clojure-deep-merge-to-ignore-nil-values
(defn deep-merge*
  [& maps]
  (let [f (fn [old new]
             (if (and (map? old) (map? new))
                 (merge-with deep-merge* old new)
                 new))]
    (if (every? map? maps)
      (apply merge-with f maps)
     (last maps))))

(defn deep-merge
  [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
   (apply merge-with deep-merge* maps)))
