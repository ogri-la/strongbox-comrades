(defproject ogri-la/strongbox-comrades "0.2.0-unreleased"
  :description "strongbox comrades.csv SPA"
  :url "https://github.com/ogri-la/strongbox-comrades"
  :license {:name "GNU Affero General Public License (AGPL)"
            :url "https://www.gnu.org/licenses/agpl-3.0.en.html"}

  :min-lein-version "2.9.1"

  :dependencies [;; clj
                 [org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.namespace "0.2.11"] ;; reload code
                 [clj-commons/fs "1.5.0"]
                 [org.clojure/data.json "0.2.7"]
                 [hiccup "1.0.5"]

                 ;; cljs
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async  "0.4.500"]
                 [testdouble/clojurescript.csv "0.4.3"]
                 [rum "0.11.3"]
                 [cljs-http "0.1.46"]
                 [funcool/cuerdas "2.2.0"]

                 ;; remember to update the LICENCE.txt
                 ;; remember to update pom file (`lein pom`)

                 ]

  :plugins [[lein-figwheel "0.5.19"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]
                                                 [clj-http]
                                                 [org.clojure/data.csv]
                                                 [org.clojure/tools.namespace]
                                                 [clj-commons/fs]
                                                 [org.clojure/data]
                                                 [hiccup]]
             ]]

  ;; not possible for figwheel: https://github.com/bhauman/lein-figwheel/issues/614
  :main strongbox-comrades.update

  :source-paths ["src" "clj-src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                ;; The presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel {:on-jsload "strongbox-comrades.main/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and compiled your application.
                           ;; Comment this out once it no longer serves you.
                           :open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main strongbox-comrades.main
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/strongbox_comrades.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/strongbox_comrades.js"
                           :main strongbox-comrades.main
                           :optimizations :advanced
                           ;;:pseudo-names true
                           :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server :) (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"

             ;; to pipe all the output to the repl
             ;; :server-logfile false
             }

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.19"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
