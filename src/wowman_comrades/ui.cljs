(ns wowman-comrades.ui
  (:require
   [wowman-comrades.core :as core :refer [unselected]]
   [wowman-comrades.utils :as utils :refer [debug info warn error spy kv-map format]]
   [rum.core :as rum]
   ))

(def rum-deref rum/react) ;; just an alias, I find 'react' confusing

(defn ev-val
  [ev]
  (.. ev -target -value))

(rum/defc dropdown
  [label option-list callback & {:keys [default-value-fn]}]
  [:select {:value (if (fn? default-value-fn)
                     (default-value-fn)
                     unselected)
            :on-change callback}
   [:optgroup {:label label}
    (for [option (map str option-list)]
      [:option {}
       option])]])

(rum/defc field-dropdown
  [name {:keys [label option-list]}]
  (let [option-list (into [""] option-list)]
    [:select {:value (-> @core/state :selected-fields name (or unselected))
              :on-change (fn [ev]
                           (let [val (ev-val ev)]
                             (if (= val unselected)
                               (swap! core/state update-in [:selected-fields] dissoc name)
                               (swap! core/state assoc-in [:selected-fields name] val))))}
     (for [option option-list]
       [:option {}
        option])]))

(rum/defc csv-header
  [csv-header field-list]
  [:thead
   [:tr
    (for [field field-list
          :let [val (field csv-header)
                label (or (:label val) val)]]
      [:th {}
       label
       (when-not (empty? (:option-list val))
         (field-dropdown field val))
       ] ;; /th
      )] ;; /tr
   ]) ;; /thead

(rum/defc csv-row
  [row field-list]
  [:tr {}
   (for [field field-list
         :let [val (field row)
               label (or (:label val) val)]]
     [:td {}
      (if (= :project field)
        [:a {:href (:href val) :target "_blank"} label]
        label)
      ])])

(rum/defc permalink
  "creates a link to the report as it's currently configured"
  []
  (let [query-string (mapv (fn [[k v]]
                             (format "%s=%s" (->> k name (str "field/")) v))
                           (:selected-fields @core/state :selected-fields))
        query-string (clojure.string/join "&" query-string)

        abs-url (format "%s//%s%s"
                        js/window.location.protocol
                        js/window.location.host
                        js/window.location.pathname)
        abs-url (if-not (empty? query-string)
                  (str abs-url "?" query-string)
                  abs-url)]
    [:small
     [:a {:href abs-url}
      "permalink"]]))

(rum/defc profile-selection
  []
  (let [available-profiles (->> core/profiles keys (map name))
        on-select-callback (fn [ev]
                             (core/set-profile! (keyword (ev-val ev)))
                             nil)
        description (-> @core/state :profile :description)
        ]
    [:div
     (dropdown "presets" available-profiles on-select-callback
               :default-value-fn #(-> @core/state :profile :name name))
     [:quote
      (if (string? description)
        (format "\"%s\"" description)
        description)]]))

(rum/defc csv-body
  [row-list field-list]
  [:tbody {}
   (mapv #(csv-row % field-list) row-list)])

(rum/defc root-component < rum/reactive
  []
  (let [state (rum-deref core/state)
        csv-data (:csv-data state)
        
        ;; give a key+val, returns a predicate that accepts a csv row
        ;; returns true if given `key` in given `row` matches given `val`
        ;; if given `val` is "yes*", then that also matches "yes" (without asterisk)
        fltr (fn [[column-name match-value]]
               (fn [row]
                 (let [row-value (column-name row)]
                   (cond
                     ;; 'yes*' also means 'yes'
                     (= match-value "yes*") (utils/in? row-value [match-value "yes"])

                     ;; empty string means "don't filter"
                     (= match-value "") true

                     :else (= match-value row-value)))))

        selected-headers (:selected-fields state)
        
        fltrfn (if-not (empty? selected-headers)
                 (apply every-pred (map fltr selected-headers))
                 identity)

        header (first csv-data)
        body (filter fltrfn (rest csv-data))
        field-list (:field-order state)]
    [:div
     (profile-selection)
     (permalink)

     [:table {}
      [:caption "comrades.csv"]
      (csv-header header field-list)
      (csv-body body field-list)]]))

(defn start
  [dom-element]
  ;; this is equivalent to starting the ui
  (rum/mount (root-component) (-> js/document (.getElementById dom-element))))
