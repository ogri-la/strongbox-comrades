(ns wowman-comrades.core
  (:require
   [wowman-comrades.utils :as utils :refer [debug info warn error spy]]
   [testdouble.cljs.csv :as csv]
   [cljs-http.client :as http])
  (:require-macros
   [wowman-comrades.macro :as macro]))

(def comrades (macro/compile-time-comrades-csv)) ;; compile-time data

;; order the fields are read in
(def -field-order [:name :url :maintained :linux :mac :windows :ui :retail :classic :f-oss :source-available :ads :eula :language])

(def -state-template
  {;; fields are displayed in the order they are read in by default
   :field-order -field-order

   ;; fields that can be selected
   :selectable-fields [:maintained :linux :mac :windows :ui :retail :classic :f-oss :source-available :ads :eula :language]
   
   ;; default selections for fields that can be selected
   :selected-fields {:ads "no"
                     :eula "no"
                     :maintained "yes"
                     :source-available "yes"}

   :csv-data nil})

(def state (atom -state-template))

(defn parse-field-params
  []
  (let [query-params (-> js/window.location.href http/parse-url :query-params)
        ;; at this point the query parameters could be *anything* the user sent us
        ;; however we only want *specific* parameters that look like :field/<known-csv-column>
        known-csv-columns (-> @state :csv-data first keys)]
    (spy query-params)))

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
  (let [option-list (fn [[key val]]
                      {key {:label val
                            :option-list (->> csv-data rest (map key) distinct (sort >))}})
        header (into {} (mapv option-list (first csv-data)))]
    (into [header] (rest csv-data))))

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
                   :field-order new-field-order}]
    (swap! state merge new-state)))

(defn start
  []
  (init)
  ;;(parse-field-params)
  )
