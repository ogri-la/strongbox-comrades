(ns wowman-comrades.macro)

;; touch this file to reload comrades.csv 

(defmacro compile-time-comrades-csv
  []
  (slurp "comrades.csv"))
