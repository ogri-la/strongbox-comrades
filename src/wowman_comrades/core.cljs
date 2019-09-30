(ns wowman-comrades.core
  (:require
   [testdouble.cljs.csv :as csv]
   [rum.core :as rum]
   [secretary.core :refer-macros [defroute]]
   )
  (:require-macros
   [wowman-comrades.macro :as macro]))

(enable-console-print!)

(def comrades (macro/compile-time-comrades-csv)) ;; compile-time data

(def rum-deref rum/react) ;; just an alias, I find 'react' confusing

(def unselected "")

(def -state-template
  {:csv-data nil

   ;; default selections
   :selected {:ads "no"
              :eula "no"
              :maintained "yes"
              :source-available "yes"}})

(def state (atom -state-template))

;; utils

(defn info
  [msg]
  (js/console.info "[:info]" msg)
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

;; matrix wrangling

(defn find-column-idx
  [column-name csv-head]
  (let [idx-if-eq (fn [[idx val]]
                    (if (map? val)
                      (when (= (:name val) column-name) idx)
                      (when (= val column-name) idx)))]
    (->> csv-head (map-indexed vector) (map idx-if-eq) (remove nil?) first)))

(defn column-map
  [csv-data]
  (let [idx-if-eq (fn [[idx val]]
                    (if (map? val)
                      {(:name val) idx}
                      {val idx}))]
    (->> csv-data first (map-indexed vector) (map idx-if-eq) (into {}))))

;;

(defn -project-hyperlink
  [row]
  (let [name (first row)
        url (second row)]
    (into [{:href url :label name}] row)))

(defn project-hyperlink
  "adds a new first column that is a map of the next two columns"
  [csv-data]
  (let [header (into [{:label "project" :name :project}] (first csv-data))
        body (rest csv-data)]
    (into [header] (mapv -project-hyperlink body))))

;;

(defn unique-column-values
  "returns a list of unique values for the given column"
  [column-idx csv-data]
  (distinct (map #(nth % column-idx) csv-data)))

(defn header-options
  "convert the values in the first row to a map of {:label '...' :name '...' :options [...]}"
  [csv-data]
  (let [header (first csv-data)
        columns-with-options [:maintained :linux :mac :windows :ui :retail :classic :f-oss :source-available :ads :eula :language]
        processor (fn [[column-idx text]]
                    (let [slug (keywordify text)]
                      {:label text
                       :name slug
                       :option-list (when (some #{slug} columns-with-options)
                                        (into [unselected] (unique-column-values column-idx (rest csv-data))))}))
        new-header-row (for [pair (map-indexed vector header)]
                         (processor pair))]
    (into [new-header-row] (rest csv-data))))

(defn drop-column
  [column-name csv-data]
  (let [idx (find-column-idx column-name (first csv-data))]
    (mapv #(drop-nth idx %) csv-data)))

;; components

(defn in?
  [v coll]
  (not (empty? (some #{v} coll))))

(rum/defc dropdown < rum/reactive 
  [{:keys [name label option-list]}]
  [:select {:value (-> state rum-deref :selected name (or unselected))
            :on-change (fn [ev]
                         (let [val (.. ev -target -value)]
                           (if (= val unselected)
                             (swap! state update-in [:selected] dissoc name)
                             (swap! state assoc-in [:selected name] val))))}
   (for [option option-list]
     [:option {}
      option])])

(rum/defc csv-header
  [header-data]
  [:tr
   (for [data header-data]
     [:th {}
      (:label data)
      (when-not (empty? (:option-list data))
        (dropdown data))])])

(rum/defc csv-row
  [row]
  [:tr {}
   (for [text row]
     [:td {}
      (if (map? text)
        [:a {:href (:href text)
             :target "_blank"}
         (:label text)]
        text)
      ])])

(rum/defc csv-body
  [row-list]
  [:tbody {}
   (mapv csv-row row-list)])

;; bootstrap

(def transformations [header-options
                      project-hyperlink
                      (partial drop-column :name)
                      (partial drop-column :url)])

(rum/defc root-component < rum/reactive
  []
  (let [csv-data (-> state rum-deref :csv-data)
        csv-data (apply-all csv-data transformations)

        header-idx-list (column-map csv-data) ;; {:ads? 9, :windows 3, ...}

        ;; give a key+val, returns a predicate that accepts a csv row
        ;; returns true if given `key` in given `row` matches given `val`
        ;; if given `val` is "yes*", then that also matches "yes" (without asterisk)
        fltr (fn [[column-name match-value]]
               (fn [row]
                 (let [col-idx (column-name header-idx-list)
                       row-value (when col-idx (nth row col-idx))]
                   (if-not col-idx
                     ;; code/configuration error, we should know about this
                     (do
                       (warn (format "column '%s' not found! ignoring" column-name))
                       true)

                     (if (= match-value "yes*") ;; 'yes*' also means 'yes'
                       (in? row-value [match-value "yes"])
                       (= match-value row-value))))))

        selected-headers (:selected (rum-deref state))
        
        fltrfn (if-not (empty? selected-headers)
                 (apply every-pred (map fltr selected-headers))
                 identity)

        header (first csv-data)        
        body (filter fltrfn (rest csv-data))
        ]
    [:table {}
     (csv-header header)
     (csv-body body)]))

;;

;; these are behaving weird because they are macros ...?
;; new routes require a page refresh
(defroute index "/" [query-params]
  (info (str "params:" (pr-str query-params))))

;;

(defn start
  []
  (rum/mount (root-component) (-> js/document (.getElementById "app")))
  (secretary.core/dispatch! (index {:query-params {:foo "bar"}})) ;; works
  (secretary.core/dispatch! "/?maintained=yes*") ;; also works
  )

(defn init
  []
  (info "init")
  (let [csv-data (csv/read-csv comrades)
        new-state {:csv-data csv-data}]
    (swap! state merge new-state)))

(defn on-js-reload
  []
  (info "reloading")
  (init)
  (start))

(on-js-reload)
