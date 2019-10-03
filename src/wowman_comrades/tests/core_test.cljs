(ns wowman-comrades.tests.core-test
  (:require
   [wowman-comrades.core :as core]
   [cljs.test :refer-macros [deftest is testing run-tests]]
   ))

(comment "run tests with: 
  (require '[wowman-comrades.tests.core-test as tests])
  (tests/run)")

(defn run
  []
  (run-tests))
