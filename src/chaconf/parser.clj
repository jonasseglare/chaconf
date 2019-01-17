(ns chaconf.parser
  (:require [clojure.string :as cljstr]))

(def re-section #"^\s*(\S+)(\s+(\S.*))?:\s*$")
(def re-blank #"^\s*$")
(def re-count #"^\s*(\S.*)\s+(\d+)$")

(defn parse-blank [x]
  (re-matches re-blank x))

(defn parse-section [x]
  (if-let [[full section-tag _ section-name] (re-matches
                                              re-section
                                              x)]
    {:type (cljstr/trim section-tag)
     :name (cljstr/trim section-name)}))

(defn parse-count [x]
  (if-let [[key n] (re-matches re-count x)]
    {:key (cljstr/trim key)
     :count (read-string n)}))

(defn split-and-enumarate-lines [src]
  (let [lines (cljstr/split-lines src)]
    (map (fn [index line] {:index (inc index)
                           :line line})
         (range (count lines))
         lines)))

(comment

  
  (do

    (def test-data
      (cljstr/join "\n"
                   ["\n"
                    "\n"
                    "\n"
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
    
    (split-and-enumarate-lines test-data)


    
    )


  )
