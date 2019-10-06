(ns wowman-comrades.utils)

(defn in?
  [v coll]
  ((complement not) (some #{v} coll)))

(defn format
  [string & args]
  (apply goog.string/format string (map str args)))

(defn -log
  [level & msg-list]
  (let [levels {:debug js/console.debug
                :info js/console.info
                :warn js/console.warn
                :error js/console.error
                :spy js/console.info}
        lede (format "[%s]" (name level))]
    (apply (get levels level) lede (mapv pr-str msg-list)))
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
