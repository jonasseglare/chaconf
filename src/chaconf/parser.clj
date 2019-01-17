(ns chaconf.parser
  (:require [clojure.string :as cljstr]))

(def re-section #"^\s*(\S+)(\s+(\S.*))?:\s*$")
(def re-count #"^\s*(\S.*)\s+(\d+)\s*$")

(defn parse-section [x]
  (if-let [[full section-tag _ section-name] (re-matches
                                              re-section
                                              x)]
    (merge
     {:type (cljstr/trim section-tag)}
     (if section-name
       {:name (cljstr/trim section-name)}
       {}))))

(defn parse-count [x]
  (if-let [[_ key n] (re-matches re-count x)]
    {:key (cljstr/trim key)
     :count (read-string n)}))

(defn split-and-enumerate-lines [src]
  (let [lines (cljstr/split-lines src)]
    (map (fn [index line] {:index (inc index)
                           :line line})
         (range (count lines))
         lines)))

(defn drop-blanks [numbered-lines]
  (drop-while (comp cljstr/blank? :line) numbered-lines))

(def section-headers {"session" :sessions
                      "ensemble" :ensembles})

(defn add-at-key [dst k x]
  (update dst k (fn [dst] (conj (or dst []) x))))

(defn read-counts [src]
  (take-while (complement nil?)
              (map (comp parse-count :line) src)))

;; (read-counts [{:line "   Violin  4 " :index 8}])

(defn read-section [input]
  (let [numbered-lines (drop-blanks (:numbered-lines input))]
    (if (not (empty? numbered-lines))
      (let [[header-line & numbered-lines] numbered-lines]
        (if-let [header (parse-section (:line header-line))]
          (let [header-type (cljstr/lower-case (:type header))
                k (get section-headers header-type)]
            (if (nil? k)
              (merge {:error :section-not-recognized}
                     header header-line)
              (let [counts (read-counts numbered-lines)]
                (-> input
                    (add-at-key k counts)
                    (assoc :numbered-lines (drop (count counts)
                                                 numbered-lines))))))
          (merge header-line {:error :failed-to-parse}))))))

;; (read-section {:numbered-lines [{:index 9 :line "asdf"}]})
;; (read-section {:numbered-lines [{:index 9 :line "asdf:"}]})
;; (read-section {:numbered-lines [{:index 9 :line "session:"}]})

(defn read-sections [numbered-lines]
  (let [result
        (last
         (take-while
          (complement nil?)
          (iterate read-section
                   {:numbered-lines numbered-lines})))]
    result))

(defn parse [s]
  (-> s
      split-and-enumerate-lines
      read-sections))

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
    
    (def d  (-> test-data
                split-and-enumarate-lines
                read-sections))


    
    )


  )
