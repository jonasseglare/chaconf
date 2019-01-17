(ns chaconf.core-test
  (:require [clojure.test :refer :all]
            [chaconf.core :refer :all :as chaconf]))

(def instrument-counts {:violin 20
                        :cello 12
                        :alto 5
                        :piano 3})

(def instrument-counts2 {:violin 22
                         :cello 12
                         :alto 4
                         :piano 3})

(def configs #{{:violin 2}
               {:violin 1
                :alto 1}
               {:violin 1
                :cello 1}
               {:violin 1
                :piano 1}
               {:violin 1
                :alto 1
                :cello 1}
               {:violin 2
                :alto 1
                :cello 1}})

(def setup {::chaconf/participants instrument-counts
            ::chaconf/ensembles configs})

(deftest basic-solving
  (let [solutions (solve setup)]
    (is (= 12 (count solutions)))
    (is (every? #(valid-solution? % instrument-counts)
                solutions))))


(def session-setup2 {::chaconf/ensembles #{{:violin 2}
                                           {:violin 1
                                            :alto 1}
                                           {:violin 1
                                            :cello 1}
                                           {:violin 1
                                            :piano 1}
                                           {:violin 1
                                            :alto 1
                                            :cello 1}
                                           {:violin 2
                                            :alto 1
                                            :cello 1}}
                     ::chaconf/sessions [{:violin 20
                                          :cello 12
                                          :alto 5
                                          :piano 3}
                                         {:violin 22
                                          :cello 12
                                          :alto 4
                                          :piano 3}]})

(deftest solve-many
  (let [sols (solve-sessions session-setup2)]
    (is (= (-> sols
               :all
               first)
           (solve setup)))))
