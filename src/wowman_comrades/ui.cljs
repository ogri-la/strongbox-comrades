(ns wowman-comrades.ui
  (:require
   [wowman-comrades.core :as core :refer [unselected]]
   [wowman-comrades.utils :as utils :refer [debug info warn error spy kv-map format]]
   [rum.core :as rum]
   ))

(def rum-deref rum/react) ;; just an alias, I find 'react' confusing

(def an-id (atom 0))

(defn get-an-id!
  []
  (swap! an-id inc))

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
      [:option {:key (get-an-id!)}
       option])]])

(rum/defc field-dropdown < {:key-fn get-an-id!}
  [name {:keys [label option-list]}]
  (let [option-list (into [""] option-list)]
    [:select {:value (-> @core/state :selected-fields name (or unselected))
              :on-change (fn [ev]
                           (let [val (ev-val ev)]
                             (if (= val unselected)
                               (swap! core/state update-in [:selected-fields] dissoc name)
                               (swap! core/state assoc-in [:selected-fields name] val))))}
     (for [option option-list]
       [:option {:key (get-an-id!)}
        option])]))

(defn html-quoted-label
  ;; "feature" => "feature"
  ;; "feature "foo bar baz"" => [:span "feature" [:span "foo bar "baz"]]
  [label]
  (let [fancy-left-quote "“"
        quote-pos (clojure.string/index-of label fancy-left-quote)]
    (if quote-pos
      [:span
       (subs label 0 quote-pos)
       [:span {:class "no-break"}
        (subs label quote-pos)]]
      label)))

(rum/defc csv-header < {:key-fn get-an-id!}
  [csv-header field-list]
  [:thead
   [:tr {:key (get-an-id!)}
    (for [field field-list
          :let [val (field csv-header)
                label (html-quoted-label (or (:label val) val ""))]]
      [:th {:key (get-an-id!)}
       label
       (when-not (empty? (:option-list val))
         (field-dropdown field val))
       ] ;; /th
      )] ;; /tr
   ]) ;; /thead

(rum/defc csv-row < {:key-fn get-an-id!}
  [row field-list]
  [:tr {:key (get-an-id!)}
   (for [field field-list
         :let [val (field row)
               label (or (:label val) val)]]
     [:td {:key (get-an-id!)}
      (if (= :project field)
        [:a {:href (:href val) :target "_blank"} label]
        label)
      ])])

(rum/defc permalink
  "creates a link to the report as it's currently configured"
  []
  (let [selected-field-list (mapv (fn [[k v]]
                                    (format "%s=%s" (->> k name (str "field/")) v))
                                  (:selected-fields @core/state :selected-fields))
        preset (str "preset/name=" (-> @core/state :profile :name name))

        query-string (into [preset] selected-field-list)
        query-string (clojure.string/join "&" query-string)

        abs-url (format "%s//%s%s"
                        js/window.location.protocol
                        js/window.location.host
                        js/window.location.pathname)
        abs-url (if-not (empty? query-string)
                  (str abs-url "?" query-string)
                  abs-url)]
    [:a {:href abs-url :id "permalink"}
     "permalink"]))

(rum/defc profile-selection
  []
  (let [available-profiles (->> core/profiles keys (map name))
        on-select-callback (fn [ev]
                             (core/set-profile! (keyword (ev-val ev))))
        description (-> @core/state :profile :description)]
    [:span
     (dropdown "presets" available-profiles on-select-callback
               :default-value-fn #(-> @core/state :profile :name name))
     [:span {:class "quote"}
      (if (string? description)
        (format "\"%s\"" description)
        description)]]))

(rum/defc csv-body < {:key-fn get-an-id!}
  [row-list field-list]
  [:tbody {:key (get-an-id!)}
   (mapv #(csv-row % field-list) row-list)])

(defn footer
  []
  [:ul
   [:li "Maintained: an addon manager is considered 'maintained' if it has seen an update in 12 months."]
   [:li [:strong [:code "*"]] " OS caveat: when " [:em "separate installation"] " of other software like Python, Java, NPM or GTK/QT, etc is required, 
or compilation or similar."]
   [:li [:strong [:code "*"]] " Linux OS caveat: if a packaged version of the addon manager exists for " [:em "at least one"] " distribution of Linux, I drop the caveat asterisk.
This may be an AUR, DEB, RPM/DNF, Snap, Flatpak, Zypper, AppImage, etc."]
   [:li [:strong [:code "^"]] " VCS caveat: only supports VCS through services like Github/Bitbucket/Gitlab and not git/hg/svn etc directly."]
   [:li [:a {:href "https://github.com/ogri-la/wowman-comrades/issues" :target "_blank"} "report any issues here"]]])

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
                     (or (= match-value "yes*")
                         (= match-value "yes^")) (utils/in? row-value [match-value "yes"])

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
     [:header
      (profile-selection)
      (permalink)]

     [:table {}
      [:caption "comrades.csv"]
      (csv-header header field-list)
      (csv-body body field-list)]

     [:footer (footer)]]))

(defn start
  [dom-element]
  ;; this is equivalent to starting the ui
  (rum/mount (root-component) (-> js/document (.getElementById dom-element))))
