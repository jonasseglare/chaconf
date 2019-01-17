(ns chaconf.parser-test
  (:require [chaconf.parser :refer :all]
            [clojure.test :refer :all]
            [clojure.string :as cljstr]))

(deftest regex-test
  (is (= "Sec" (second (re-matches re-section "    Sec:"))))
  (is (= "A" (nth (re-matches re-section "    Sec A:") 3)))
  (is (nil? (re-matches re-section "asdfadsf")))
  (is (= {:type "Section"
          :name "Mjao"}
         (parse-section "   Section    Mjao :  ")))
  (is (nil? (parse-section "asdfsafd")))

)


(def test-data2
  (cljstr/join "\n"
               [""
                ""
                ""
                " Session: "
                " Violin 20  "
                "Cello 12"
                "Alto 5"
                "Piano 3"
                "\n"
                "\n"
                "Ensemble V+V: "
                "Violin 2"
                "\n"
                "Ensemble V+A :"
                "Violin 1"
                "Cello 1  "
                ]))

(deftest parse-test
  (let [result (parse test-data2)]
    (is (not (contains? result :error)))
    (is (= 1 (count (:sessions result))))
    (is (= 2 (count (:ensembles result)))))
  (is (:error (parse (str test-data2 "\nasdfasdfsadf"))))
  (is (= (:error (parse (str "sadfsadf\n" test-data2)))
         :failed-to-parse))
  (is (= (:error (parse (str "Kattskit:\n" test-data2)))
         :section-not-recognized)))
