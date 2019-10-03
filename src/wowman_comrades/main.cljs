(ns wowman-comrades.main
  (:require
   [wowman-comrades.core :as core]
   [wowman-comrades.utils :as utils]
   [wowman-comrades.ui :as ui]))

(enable-console-print!)

(defn start
  []
  (core/start)
  (ui/start "app"))

(defn on-js-reload
  []
  ;; unmount root component?
  (utils/info "reloading")
  (start))

(on-js-reload)

            
