(ns wowman-comrades.core
  (:require
   [testdouble.cljs.csv :as csv]
   [rum.core :as rum]))

(enable-console-print!)

(def rum-deref rum/react)

(def unselected "")

(def -state-template
  {:csv-data nil
   :selected {:ads? "no"
              :eula? "no"
              :maintained? "yes"
              :source-available? "yes"}
   })

(def state (atom -state-template))

;;

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

(def comrades (str "name,url,Linux,Mac,Windows,UI,retail?,classic?,f/oss?,source available?,ads?,EULA?,language
antiwinter/wowa,https://github.com/antiwinter/wowa,yes*,yes*,yes*,CLI,yes,yes,yes,yes,no,no,Javascript
braier/wow-addon-updater,https://www.braier.net/wow-addon-updater/index.html,yes,yes,yes,GUI,yes,no,yes,yes,no,no,Pascal
cursebreaker,https://github.com/AcidWeb/CurseBreaker,no,no,yes,TUI^,yes,yes,yes,yes,no,no,Python
dark0dave/wow-addon-updater,https://gitlab.com/dark0dave/wow-addon-updater,yes*,yes,yes,CLI,yes,yes,yes,yes,no,no,python
erikabp123/ClassicAddonManager,https://github.com/erikabp123/ClassicAddonManager,no,no,yes,GUI,no,yes,yes,yes,no,no,Java
GitAddonsManager,https://gitlab.com/woblight/GitAddonsManager,yes,?,?,GUI,yes,no,yes,yes,no,no,C++
grrttedwards/wow-addon-updater,https://github.com/grrttedwards/wow-addon-updater,yes*,yes,yes,CLI,yes,yes,yes,yes,no,no,python
instawow,https://github.com/layday/instawow,yes*,yes*,yes*,CLI,yes,yes,yes,yes,no,no,Python
karolswdev/wow,https://github.com/karolswdev/wow,no,no,yes,CLI,no,yes,no,yes,no,no,C#
lcurse,https://github.com/ephraim/lcurse,yes,no,no,GUI,yes,no,no,yes,no,no,Python
Lund259/WoW-Addon-Manager,https://github.com/Lund259/WoW-Addon-Manager,no,no,yes,GUI,yes,no,yes,yes,no,no,C#
Minion,https://minion.mmoui.com/,yes*,yes,yes,GUI,yes,yes,no,no,yes,yes,Java
nazarov-tech/wowa,https://github.com/nazarov-tech/wowa,yes*,yes*,yes,CLI,yes,no,yes,yes,no,no,Python
OpenAddOnManager,https://github.com/OpenAddOnManager/OpenAddOnManager,no,no,yes,GUI,yes,no,yes,yes,no,no,C#
qwezarty/wow-addon-manager,https://github.com/qwezarty/wow-addon-manager,yes*,no,no,CLI,yes,no,yes,yes,no,no,Python
Saionaro/wow-addons-updater,https://github.com/Saionaro/wow-addons-updater,yes,yes,yes,GUI,yes,no,yes,yes,no,no,Javascript
Tukui Client,https://www.tukui.org/download.php?client=win,no,no,yes,GUI,yes,no,no,no,?,?,?
vargen2/Addon,https://github.com/vargen2/Addon,no,no,yes,GUI,yes,yes,yes,yes,no,no,C#
WorldOfAddons,https://github.com/WorldofAddons/worldofaddons,yes*,yes*,yes,GUI,yes,no,yes,yes,no,no,Javascript"))

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

(defn keywordify
  "'Source Available?' => :source-available?, 'f/oss?' => :f-oss?"
  [text]
  (-> text
      clojure.string/lower-case
      (clojure.string/replace #"[ _/]+" "-")
      keyword))

(defn header-options
  "convert the values in the first row to a map of {:label '...' :name '...' :options [...]}"
  [csv-data]
  (let [header (first csv-data)
        columns-with-options [:linux :mac :windows :ui :retail? :classic? :f-oss? :source-available? :ads? :eula? :language]
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
  (println "val" v "coll" coll)
  (let [res (-> v set (some coll) empty? not)]
    (println "res" res)
    res))

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
  (let [csv-data (:csv-data (rum-deref state))
        csv-data (apply-all csv-data transformations)

        header-idx-list (column-map csv-data) ;; {:ads? 9, :windows 3, ...}
        ;;_ (println header-idx-list)

        header (first csv-data)
        body (rest csv-data)
        
        fltr (fn [[column-name match-value]]
               ;;(println "got" column-name match-value)
               (fn [row]
                 ;;(println "got row" row)
                 (if-let [col-idx (column-name header-idx-list)]
                   (= match-value (nth row col-idx))
                   (do
                     (println (goog.string/format "column '%s' not found! ignoring" column-name))
                     true))))

        selected-headers (:selected (rum-deref state))
        ;;_ (println "selected headers" selected-headers)
        
        fltrfn (if-not (empty? selected-headers)
                 (apply every-pred (map fltr selected-headers))
                 identity)
        ;;_ (println "filterfn" fltrfn)
        
        body (filter fltrfn body)
        ]
    [:table {}
     (csv-header header)
     (csv-body body)]))

(defn start
  []
  (rum/mount (root-component) (-> js/document (.getElementById "app"))))

(defn init
  []
  (println "(init)") 
  (let [csv-data (csv/read-csv comrades)
        new-state {:csv-data csv-data}]
    (swap! state merge new-state)))

(defn on-js-reload
  []
  (println "(reloading)")
  (init)
  (start))

(on-js-reload)
