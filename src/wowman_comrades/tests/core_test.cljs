(ns wowman-comrades.tests.core-test
  (:require
   [wowman-comrades.core :as core]
   [cljs.test :refer-macros [deftest is testing run-tests]]
   ))

(comment "run tests with: 
  (require '[wowman-comrades.tests.core-test as tests])
  (tests/run)")

(deftest utils
  (testing "format"
    (is (= "foo bar baz" (core/format "%s %s %s" "foo" "bar" "baz")))))

(defn run
  []
  (run-tests))
