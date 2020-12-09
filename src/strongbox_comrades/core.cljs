(ns strongbox-comrades.core
  (:require
   [strongbox-comrades.utils :as utils :refer [debug info warn error spy in? kv-filter kv-map]]
   [testdouble.cljs.csv :as csv]
   [cljs-http.client :as http])
  (:require-macros
   [strongbox-comrades.macro :as macro]))

(def unselected "")

(def comrades (macro/compile-time-comrades-csv)) ;; compile-time data

;; order the fields are read in
(def -field-order [:name :url :maintained :linux :mac :windows :ui :retail :classic
                   :f-oss :software-licence :source-available :eula :ads :language
                   :feature-curseforge :feature-wowinterface :feature-tukui :feature-vcs-addons
                   :feature-catalog-search :feature-wago.io])

(def -state-template
  {;; fields are displayed in the order they are read in by default
   ;; this doubles as a means to show/hide fields
   :field-order -field-order

   ;; fields that can be selected
   ;; we need this to know how to create and check permalinks
   :selectable-fields [:maintained :linux :mac :windows :ui :retail :classic
                       :f-oss :software-licence :source-available :eula :ads :language
                       :feature-curseforge :feature-wowinterface :feature-tukui :feature-vcs-addons
                       :feature-catalog-search :feature-wago.io]

   ;; map of :name and :description for selected preset
   :profile nil
   
   ;; selections for fields that can be selected
   :selected-fields {}

   :csv-data nil})

(def state (atom -state-template))

(def profiles
  {:default {:description "some basic filtering, good for everybody"
             :field-order (into [:project] (remove #{:ads :eula :source-available :software-licence :f-oss :language :feature-vcs-addons :feature-wago.io} -field-order))
             :selected-fields {:maintained "yes" :classic "yes"}}
   
   :unfiltered {:description "no filtering, ordered by 'maintained' and then by 'name'"
                :selected-fields (zipmap (:selectable-fields -state-template) (repeat unselected))}

   ;;:snarky {:description [:a {:href "https://store.steampowered.com/hwsurvey/Steam-Hardware-Software-Survey-Welcome-to-Steam?platform=combined"
   ;;                           :target "_blank"}
   ;;                       "\"statistically, this is probably for you\""]
   ;;         :field-order [:project :windows :retail :classic]
   ;;         :selected-fields {:maintained "yes"
   ;;                           :windows "yes"
   ;;                           :retail "yes" :classic "yes"
   ;;                           :ui "GUI"}}

   :linux {:description "good choices for Linux users"
           :field-order [:project :retail :classic :ui :f-oss :source-available :software-licence :ads :eula
                         :language
                         :feature-curseforge :feature-wowinterface :feature-tukui
                         :feature-catalog-search]
           :selected-fields {:maintained "yes"
                             :linux "yes*"
                             :source-available "yes"
                             :ads "no" :eula "no"
                             :retail "yes" :classic "yes"}}
   
   :mac {:description "good choices for mac users"
         :field-order [:project :retail :classic :ui
                       :feature-curseforge :feature-wowinterface :feature-tukui
                       :feature-catalog-search]
         :selected-fields {:maintained "yes"
                           :mac "yes*"
                           :retail "yes" :classic "yes"}}

   :windows {:description "good choices for windows users"
             :field-order [:project :retail :classic :ui
                           :feature-curseforge :feature-wowinterface :feature-tukui
                           :feature-catalog-search]
             :selected-fields {:maintained "yes"
                               :windows "yes"
                               :retail "yes" :classic "yes"}}
   
   :perfect {:description "perfect addon managers (tick all the right boxes)"
             :selected-fields {:maintained "yes"
                               :windows "yes" :mac "yes" :linux "yes"
                               :ui "GUI"
                               :retail "yes" :classic "yes"
                               :ads "no" :eula "no" :f-oss "yes"
                               :feature-curseforge "yes"
                               :feature-wowinterface "yes"
                               :feature-catalog-search "yes"}}
   })

;;


(defn rows-to-maps
  "converts a regular array of values into a keyed map.
  this destroys ordering but that can enforced elsewhere."
  [raw-csv-data]
  (let [field-names (-> -state-template :field-order)
        to-map (fn [row]
                 (into {} (map vector field-names row)))]
    (mapv to-map raw-csv-data)))

(defn -project-hyperlink
  [row]
  (assoc row :project {:href (:url row)
                       :label (:name row)}))

(defn add-project-hyperlink
  "adds a new first column that is a map of the next two columns"
  [csv-data]
  (let [header (assoc (first csv-data) :project "Project")]
    (into [header] (mapv -project-hyperlink (rest csv-data)))))

(defn drop-column
  [csv-data column-name]
  (mapv #(dissoc % column-name) csv-data))

(defn add-header-options
  [csv-data]
  (let [option-list (fn [key val]
                      {key {:label val
                            :option-list (->> csv-data rest (map key) distinct (sort >))}})
        header (kv-map option-list (first csv-data))]
    (into [header] (rest csv-data))))

;;

(defn parse-user-params
  "called after initial setup to handle parameters sent to us by the user"
  []
  (let [query-params (-> js/window.location.href http/parse-url :query-params)
        ;; at this point the query parameters could be *anything* the user sent us
        ;; however we only want *specific* parameters that look like :field/<known-csv-column>
        known-csv-columns (-> @state :csv-data first keys)

        preset-parameters [:name] ;; :preset/name
        supported-keywords (into preset-parameters known-csv-columns)

        ;; the longest value we can expect. longer than this and it's ignored.
        ;; affects permalinks.
        max-val-len (inc (count "noassertion")) ;; previously 'javascript'
        
        ;; strips out any query namespaces we're not looking at
        ;; strips out any keywords that don't match a known field
        ;; strips out any values that are too long
        supported-query-params (kv-filter (fn [key val]
                                            (and
                                             (in? (namespace key) ["field" "preset"])
                                             (in? (-> key name keyword) supported-keywords)
                                             (> max-val-len (count val))))
                                          query-params)]
    supported-query-params))

(defn update-selected-fields!
  [user-params & {:keys [deepmerge?]}]
  (let [;; only fields in the 'field' namespace
        user-params (kv-filter #(= "field" (namespace %1)) user-params)
        user-params (kv-map #(vector (-> %1 name keyword) (or %2 "")) user-params)]
    (when-not (empty? user-params)
      (if deepmerge?
        ;; recursive merge of changes, may cause weirdness
        (swap! state utils/deep-merge {:selected-fields user-params})
        ;; overrides default selected fields
        (swap! state assoc :selected-fields user-params))))
  nil)


(defn set-profile!
  "merges the predefined configuration in a profile over the current state"
  [profile-key]
  (let [profile-config (get profiles profile-key)
        extra-config {:profile {:name profile-key
                                :description (:description profile-config)}}]
    (swap! state merge (:safe-state @state) extra-config profile-config))
  nil)

;;

(defn handle-user-params
  "handles the tricky business of merging multiple types of supported filtering.
  :field/names can be passed with values as well as :preset/name configuration presets.
  configuration presets are applied first, and then field values over it.

  for example: ?preset/name=unfiltered&field/language=Go

  says 'show me everything in the Go language'. without the :preset/name filter, the
  'default' preset is used, which would hide addons that are not maintained and don't
  support classic."
  [user-params]
  (let [user-params (parse-user-params)
        preset (->> user-params keys (some #{:preset/name}) (get user-params) keyword)]
    (when (get profiles preset)
      (set-profile! preset))
    (update-selected-fields! user-params :deepmerge? true)))

(defn init
  "read the data in, parse it into a list of maps, set the initial app state"
  []
  (info "init")
  (let [csv-data (-> comrades csv/read-csv
                     rows-to-maps
                     add-header-options
                     add-project-hyperlink
                     (drop-column :name)
                     (drop-column :url))

        new-field-order (into [:project] (remove #{:name :url} -field-order))
        new-state {:csv-data csv-data
                   :field-order new-field-order

                   ;; configuration profiles are merged over the top of this
                   ;; to ensure changes do not accumulate in weird ways
                   :safe-state {:field-order new-field-order
                                :selected-fields (:selected-fields -state-template)}
                   }]
    (swap! state merge new-state)
    (set-profile! :default))

  (handle-user-params (parse-user-params))

  nil)

(defn start
  []
  (init))
