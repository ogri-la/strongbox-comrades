(ns wowman-comrades.core
  (:require
   [wowman-comrades.utils :as utils :refer [debug info warn error spy in? kv-filter kv-map]]
   [testdouble.cljs.csv :as csv]
   [cljs-http.client :as http])
  (:require-macros
   [wowman-comrades.macro :as macro]))

(def unselected "")

(def comrades (macro/compile-time-comrades-csv)) ;; compile-time data

;; order the fields are read in
(def -field-order [:name :url :maintained :linux :mac :windows :ui :retail :classic :f-oss :source-available :ads :eula :language])

(def -state-template
  {;; fields are displayed in the order they are read in by default
   ;; this doubles as a means to hide fields
   :field-order -field-order

   ;; fields that can be selected
   :selectable-fields [:maintained :linux :mac :windows :ui :retail :classic :f-oss :source-available :ads :eula :language]

   ;; name and description of selected configuration
   :profile nil
   
   ;; default selections for fields that can be selected
   :selected-fields {}

   :csv-data nil})

(def state (atom -state-template))

(def profiles
  {:default {:description "the configuration you when visiting the page for the first time"
             :selected-fields {:ads "no" :eula "no" :maintained "yes" :source-available "yes"}}
   
   :no-selections {:description "all selectable fields are unselected"
                   :selected-fields (zipmap (:selectable-fields -state-template) (repeat unselected))}

   :simple {:description "simple view for simple folk"
            :field-order [:project :windows :mac]
            :selected-fields {:maintained "yes"
                              :windows "yes*"
                              :retail "yes" :classic "yes"
                              :ui "GUI"}}

   :linux {:description "good choices for Linux users"
           :field-order [:project :retail :classic :ui :f-oss :source-available :ads :eula :language]
           :selected-fields {:maintained "yes"
                             :linux "yes*"
                             :source-available "yes"
                             :ads "no" :eula "no"
                             :retail "yes" :classic "yes"}}
   
   :mac {:description "good choices for mac users"
         :field-order [:project :retail :classic :ui :ads :eula]
         :selected-fields {:maintained "yes"
                           :mac "yes*"
                           :retail "yes" :classic "yes"}}

   :windows {:description "good choices for windows users"
             :field-order [:project :retail :classic :ui :ads :eula]
             :selected-fields {:maintained "yes"
                               :windows "yes"
                               :retail "yes" :classic "yes"}}
   
   :perfect {:description "Torkus' perfect addon manager (doesn't exist)"
             :selected-fields {:maintained "yes"
                               :windows "yes" :mac "yes" :linux "yes"
                               :ui "GUI"
                               :retail "yes" :classic "yes"
                               :ads "no" :eula "no" :f-oss "yes"}}
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

        max-val-len (inc (count "javascript"))
        
        ;; strips out any query namespaces we're not looking at
        ;; strips out any keywords that don't match a known field
        ;; strips out any values that are too long
        supported-query-params (kv-filter (fn [key val]
                                            (and
                                             (in? (namespace key) ["field" "hide"])
                                             (in? (-> key name keyword) known-csv-columns)
                                             (> max-val-len (count val))))
                                          query-params)]
    supported-query-params))

(defn update-selected-fields!
  [user-params]
  (let [;; only fields in the 'field' namespace
        user-params (kv-filter #(= "field" (namespace %1)) user-params)
        user-params (kv-map #(vector (-> %1 name keyword) (or %2 "")) user-params)]
    (when-not (empty? user-params)
      ;; overrides default selected fields
      (swap! state assoc :selected-fields user-params))))

;;

(defn set-profile!
  "merges the predefined configuration in a profile over the current state"
  [profile-key]
  (let [profile-config (get profiles profile-key)
        extra-config {:profile {:name profile-key
                                :description (:description profile-config)}}]
    (swap! state merge (:safe-state @state) extra-config profile-config))
  nil)

;;

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

        new-field-order (into [:project] -field-order)
        new-state {:csv-data csv-data
                   :field-order new-field-order

                   ;; configuration profiles are merged over the top of this
                   ;; ensures changes don't accumulate in weird ways
                   :safe-state {:field-order new-field-order
                                :selected-fields (:selected-fields -state-template)}
                   }]
    (swap! state merge new-state)
    (set-profile! :default))

  (let [user-params (parse-user-params)]
    (update-selected-fields! user-params))

  nil)

(defn start
  []
  (init))
