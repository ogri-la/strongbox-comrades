(ns wowman-comrades.update
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clj-http.client :as http]
   [me.raynes.fs :as fs]
   [clojure.data.json :as json]
   ))

(def cache-dir "/tmp/wowman-comrades/cache")

(defn write-csv!
  [rows output-file]
  (with-open [writer (io/writer output-file)]
    (csv/write-csv writer rows)))

(defn read-csv!
  [csv-file]
  (with-open [reader (io/reader csv-file)]
    (doall
     (csv/read-csv reader))))

(defn to-sorted-vecs
  "given a list of maps, returns a list of those map's values with the given header as the first item"
  [rows header]
  (let [;; turn list of unordered maps into a list of values ordered by given header
        sort-vals (fn [row]
                    (let [cmp (comp #(.indexOf header %) first)]
                      (->> row seq (sort-by cmp) (map second))))]
    (into [header] (mapv sort-vals rows))))

(defn to-maps
  [csv-data]
  (map zipmap
       (->> (first csv-data) ;; First row is the header
            ;;(map keyword) ;; Drop if you want string keys instead
            repeat)
	  (rest csv-data)))

(defn fs-cache-key
  "returns a fs-safe base64 string given a value `x`"
  [x]
  (let [enc (java.util.Base64/getUrlEncoder)]
    (->> x str .getBytes (.encodeToString enc))))

(defn fs-join
  "just like `fs/file`, but returns a string path"
  [& bits]
  (str (apply fs/file bits)))

(defn http-get
  "given a URL, does a HTTP GET when result of previous fetch does not exist.
  stores result in a temporary file on the filesystem"
  [url]
  (prn "fetching" url)
  (let [key (fs-cache-key url)
        key-path (fs-join cache-dir key)]
    (fs/mkdirs cache-dir)
    
    (if (fs/exists? key-path)
      (slurp key-path)
      
      (let [result (:body (http/get url))]
        (spit key-path result)
        result))))

(defn explicitly-set?
  "a value is 'explicitly set' when it's suffixed with an exclamation mark."
  [x]
  (clojure.string/ends-with? (str x) "!"))

(defn strip-explicit-mark
  [x]
  (apply str (drop-last x)))

(defn y-n-m
  "yes, no, maybe."
  [x]
  (cond
    ;; value has been explicitly set, emit without mark
    (explicitly-set? x) (strip-explicit-mark x)
    
    (true? x) "yes"
    (false? x) "no"

    ;; 'yes, with caveats' is the same as 'maybe' is the same as 'no, with caveats'
    :else "yes*"))

(defn dt-y-m-d
  "given a RFC3339 date-timestamp, returns the year, month and day as a triplet"
  [dt-str]
  [(Integer. (subs dt-str 0 4))
   (Integer. (subs dt-str 5 7))
   (Integer. (subs dt-str 8 10))])

(defn third
  [x]
  (nth x 2))

(defn dt-as-int
  "converts a date triple into an integer value representing number of days since the epoch.
  deliberately inprecise for now, I don't want to deal with date objects right now, tyvm"
  [dt-triple]
  ;; given [2020 1 1]
  (+
   (-> dt-triple first (* 365)) ;; 2020 * 365
   (-> dt-triple second dec (* 30)) ;; (1 - 1) * 30
   (third dt-triple))) ;; 1

(def dt-today
  (dt-as-int
   (dt-y-m-d
    (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.)))))

;;

(defn github-hosted?
  "returns true if the given addon manager is hosted on github"
  [row]
  (-> row (get "URL") (.indexOf "github.com") (> -1)))

(defn github-repo-name
  "returns the github `repo-owner/repo-name` string.
  assumes addon manager is hosted on github.
  if separator not found in addon name, it's assumed to be the owner's name and the repository name is identical"
  [row]
  (let [name (get row "Name")]
    (if (> (.indexOf name "/") -1)
      name
      (format "%s/%s" name name))))

(defn github-data
  "if given row is a project hosted on github, fetch it's data and return it"
  [row]
  (when (github-hosted? row)
    (let [;;        https://api.github.com/repos/vargen2/addon-manager
          url (str "https://api.github.com/repos/" (github-repo-name row))
          ]
      (-> url http-get json/read-str))))

(defn maintained?
  "a repository is 'maintained' if it isn't archived and has seen a commit in the last 12 months.
  if the addon manager is hosted on github we can use the 'pushed_at' and 'archived' values to determine this.
  the 'pushed_at' value is for the last commit on *any* branch, including stagnant pull requests.
  in the case of `sysworx/wowam` I can't find the very recent commit it's talking about."
  [row]
  (when-let [data (github-data row)]
    (let [archived? (get data "archived")
          ;; disabled? this happens when paid-for accounts don't pay their bill. ignoring
          last-updated (-> data (get "pushed_at") dt-y-m-d, dt-as-int)
          days-since-today (- dt-today last-updated)
          has-recent-commit (< days-since-today 365)]

      (y-n-m (and (not archived?)
                  has-recent-commit)))))

(defn update-row
  "wrapper around some boilerplate updating individual values in the row map"
  [row key func]
  (let [current-value (get row key)]
    (assoc row key
           (if-not (explicitly-set? current-value)
             ;; if func doesn't update value, use existing one
             (or (func row) current-value)
             ;; value has been explicitly set to what it is.
             ;; just remove the special character that marks it as explicitly set
             (strip-explicit-mark current-value)))))

(defn update-data
  "updates each row in the list of given maps"
  [map-list]
  (let [update (fn [row]
                 (-> row
                     (update-row "Maintained" maintained?)
                     ;;(update-row ...)
                     ))]
    (map update map-list)))

;;

(defn -main
  []
  (let [rows (read-csv! "comrades.csv")
        header (first rows) ;; preferred ordering
        ]
    (-> rows
        to-maps
        update-data
        (to-sorted-vecs header)
        (write-csv! "comrades2.csv"))))
