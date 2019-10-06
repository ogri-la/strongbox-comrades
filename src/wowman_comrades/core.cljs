(ns wowman-comrades.core
  (:require
   [wowman-comrades.utils :as utils :refer [debug info warn error spy in? kv-filter kv-map]]
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
                                            (info known-csv-columns)
                                            (and
                                             (in? (namespace key) ["field" "hide"])
                                             (in? (-> key name keyword) known-csv-columns)
                                             (> max-val-len (count val))))
                                          query-params)]
    supported-query-params))

(defn update-selected-fields!
  [user-params]
  (info "user params" user-params)
  (let [;; only fields in the 'field' namespace
        user-params (kv-filter #(= "field" (namespace %1)) user-params)
        user-params (kv-map #(vector (-> %1 name keyword) %2) user-params)]
    (when-not (empty? user-params)
      ;; overrides default selected fields
      (swap! state assoc :selected-fields user-params))))

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
                   :field-order new-field-order}]
    (swap! state merge new-state))

  (let [user-params (parse-user-params)]
    (update-selected-fields! user-params))

  nil)

(defn start
  []
  (init))
