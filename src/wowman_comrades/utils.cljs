(ns wowman-comrades.utils)

(defn in?
  [v coll]
  (not (empty? (some #{v} coll))))

(defn debug
  [msg]
  (js/console.debug "[:debug]" msg))

(defn info
  [msg]
  (js/console.info "[:info]" (if (string? msg) msg (pr-str msg)))
  nil)

(defn warn
  [msg]
  (js/console.warn "[:warn]" msg)
  nil)

(defn error
  [msg]
  (js/console.error "[:error]" msg)
  nil)

(defn format
  [string & args]
  (apply goog.string/format string args))

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
