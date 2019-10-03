(ns wowman-comrades.tests.core-test
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
    (is (nil? (utils/error "!!"))))

  (testing "`apply-all` takes a list of N functions and essentially does this (-> x f1 f2 ... fn)"
    (is (= 3 (utils/apply-all 0 [inc inc inc])))
    (is (= "asdf" (utils/apply-all "a" [#(str % "s") #(str % "d") #(str % "f")])))
    (is (= {} (utils/apply-all {:foo "bar" :baz "bup"} [#(dissoc % :foo) #(dissoc % :baz)]))))

  (testing "`drop-nth` drops the Nth element in a given collection"
    (testing "standard case"
      (is (= [1 3] (utils/drop-nth 1 [1 2 3]))))
    (testing "zero based indexing"
      (is (= [] (utils/drop-nth 0 [1]))))
    (testing "dropping something from nothing"
      (is (= [] (utils/drop-nth 1 nil))))
    (testing "dropping negative index"
      (is (= [1 2 3] (utils/drop-nth -1 [1 2 3]))))
    (testing "dropping something outside range"
      (is (= [1 2 3] (utils/drop-nth 999 [1 2 3])))))

  (testing "`keywordify`, the poor man's keyword slug. used to refer to column names"
    (let [case-list [[nil nil]
                     ["" nil]
                     ["foo" :foo]
                     ["foo?" :foo?]
                     ["Foo...?" :foo-?]
                     ["foo bar baz" :foo-bar-baz]
                     ["foo,bar,baz" :foo-bar-baz]
                     ["foo+Bar & baz" :foo-bar-baz]]
          ]
      (doseq [[given expected] case-list]
        (is (= expected (utils/keywordify given)))))))
