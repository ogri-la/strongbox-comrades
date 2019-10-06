(ns wowman-comrades.tests.utils-test
  (:require
   [wowman-comrades.utils :as utils]
   [cljs.test :refer-macros [deftest is testing run-tests]]
   ))

(deftest utilities
  (testing "format"
    (is (= "foo bar baz" (utils/format "%s %s %s" "foo" "bar" "baz"))))

  (testing "info, warning, error"
    (is (nil? (utils/info "?")))
    (is (nil? (utils/warn "!")))
    (is (nil? (utils/error "!!")))))
