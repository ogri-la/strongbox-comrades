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

;; bootstrap
;; starts the app once but never again.
;; hot reloading during development is handled by figwheel calling `on-js-reload`
(defonce _ (do (on-js-reload) true))
