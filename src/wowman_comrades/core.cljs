(ns wowman-comrades.core
  (:require
   [testdouble.cljs.csv :as csv]
   [rum.core :as rum]))

(enable-console-print!)

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

(defn process-header
  [csv-data]
  (let [columns-with-options [:linux :mac :windows :ui :retail? :classic? :foss? :source-available? :ads? :eula? :language]
        header (first csv-data)
        processor (fn [[column-idx text]]
                    (let [slug (keywordify text)]
                      {:label text
                       :name slug
                       :unique-values (when (some #{slug} columns-with-options)
                                        (unique-column-values column-idx (rest csv-data)))}))
        ]
    (for [pair (map-indexed vector header)]
      (processor pair))))

;;

(rum/defc csv-header
  [header-data]
  [:tr
   (for [data header-data]
     [:th {}
      [(:label data)]])])

(rum/defc csv-row
  [row]
  [:tr {}
   (for [text row]
     [:td {} text])])

(rum/defc csv-body
  [row-list]
  [:tbody {}
   (mapv csv-row row-list)])

(rum/defc root-component
  []
  (let [csv-data (csv/read-csv comrades)
        ;; convert the values in the first row to a map of {:label "..." :name "..." :options [...]}
        csv-header-data (vec (process-header csv-data))
        ]
    (println (unique-column-values 0 csv-data))
    [:table {}
     (csv-header csv-header-data) ;; csv-header-data)
     (csv-body (rest csv-data))]))

(defn start
  []
  (rum/mount (root-component) (-> js/document (.getElementById "app"))))  

(defn on-js-reload []
  (println "(reloading)")
  (start))

(start)



