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

(defn apply-all
  [data fn-list]
  (if (empty? fn-list)
    data
    (let [f (first fn-list)]
      (apply-all (f data) (rest fn-list)))))

;; https://stackoverflow.com/questions/24553524/how-to-drop-the-nth-item-in-a-collection-in-clojure
(defn drop-nth [n coll]
  (concat 
    (take n coll)
    (drop (inc n) coll)))

(defn keywordify
  "'Source Available?' => :source-available?, 'f/oss?' => :f-oss?"
  [text]
  (if (empty? text)
    nil
    (-> text
        clojure.string/lower-case
        (clojure.string/replace #"[^a-zA-Z0-9- ?]" " ")
        (clojure.string/replace #"[ _/]+" "-")
        keyword)))

(defn kv-map
  [f coll]
  (into {} (map (fn [[k v]]
                  (f k v)) coll)))

(defn kv-filter
  [f coll]
  (into {} (filter (fn [[k v]]
                     (f k v))
                   coll)))
