(ns wowman-comrades.utils
  (:require
   [goog.string]
   [cuerdas.core :as str])
  (:import 
   goog.string))

(defn in?
  [v coll]
  ((complement not) (some #{v} coll)))

;; advanced compilation doesn't like this and I couldn't figure out why :(
;; cuerdas has loads of good stuff though: https://funcool.github.io/cuerdas/latest/
;;(defn format
;;  [string & args]
;;  (apply goog.string/format string (map str args)))

(def format str/format)

(defn -log
  [level & msg-list]
  (let [levels {:debug js/console.log
                :info js/console.info
                :warn js/console.warn
                :error js/console.error
                :spy js/console.info}
        func (get levels level)
        ;;lede (format "[%s]" (name level))
        lede (str "[" (name level) "]")
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
