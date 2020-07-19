(ns strongbox-comrades.tests.core-test
  (:require
   [strongbox-comrades.core :as core]
   [cljs.test :refer-macros [deftest is testing run-tests]]
   ))

(comment "run tests with: 
  (require '[strongbox-comrades.tests.core-test as tests])
  (tests/run)")

(defn run
  []
  (run-tests))
