(ns chaconf.parser-test
  (:require [chaconf.parser :refer :all]
            [clojure.test :refer :all]))

(deftest regex-test
  (is (= "Sec" (second (re-matches re-section "    Sec:"))))
  (is (= "A" (nth (re-matches re-section "    Sec A:") 3)))
  (is (nil? (re-matches re-section "asdfadsf")))
  (is (= {:type "Section"
          :name "Mjao"}
         (parse-section "   Section    Mjao :  "))))


