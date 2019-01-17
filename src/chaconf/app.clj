(ns chaconf.app
  (:import [java.awt Desktop]
           [java.net URI]
           [java.io File])
  (:require [chaconf.parser :as parser]
            [chaconf.core :as core]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.string :as cljstr]
            [clojure.set :as cljset]
            [clojure.pprint :as pp]))

(defn mark-line [input]
  (cljstr/join
   "\n"
   (update
    (vec (cljstr/split-lines (:raw input)))
    (:index input)
    #(str % "  <---------- error on this line"))))

(defn input-error [input msg]
  [:body
   [:h1 "Input error"]
   [:p "on line " (inc (:index input)) ":"]
   [:p msg]
   [:h2 "Raw input"]
   [:pre (mark-line input)]])

(defn failed-to-parse [input]
  (input-error input "Failed to parse section header"))

(defn section-not-recognized [input]
  (input-error input (format "Section type '%s' not recognized"
                             (:type input))))

(defn uncovered-instruments [instruments]
  [:body
   [:h1 "Instrument set error"]
   [:p "The following instruments are not part of any ensemble:"]
   [:ul
    (for [inst instruments]
      [:li inst])]])

(defn parsed-counts-to-map [counts]
  (reduce
   (fn [dst key-and-count]
     (update dst (:key key-and-count)
             (fn [dst]
               (+ (or dst 0)
                  (:count key-and-count)))))
   {}
   counts))

(defn compress-section [section]
  (update section :values parsed-counts-to-map))

(defn compress-sections [input]
  (-> input
      (update :sessions (partial map compress-section))
      (update :ensembles (partial map compress-section))))

(defn instrument-set [sections]
  (transduce
   (comp (map (comp set keys :values)))
   cljset/union
   #{}
   sections))

(defn render-count-table [m]
  [:table
   [:tr [:th "Instrument"] [:th "Count"]]
   (for [[k v] m]
     [:tr
      [:td k] [:td v]])])

(defn render-single [input setup sol]
  [:body
   [:h1 "Solution"]
   [:h2 "Participants"]
   (render-count-table (first (::core/sessions setup)))
   [:h2 "Solutions"]
   (let [sol (-> sol
                 :all
                 first)]
     (list
      (if (empty? sol)
        [:p "No solutions found"]
        [:p (format "Found %d solution(s)" (count sol))])))])

(defn render-multiple [input sol])

(defn execute-validated [input]
  (let [session-setup {::core/ensembles
                       (mapv :values (:ensembles input))
                       ::core/sessions
                       (mapv :values (:sessions input))}
        sol (core/solve-sessions session-setup)
        session-count (count (:sessions input))]
    (if (= 1 (count (:sessions input)))
      (render-single input session-setup sol)
      (render-multiple input sol))))

(def no-session-error [:body
                       [:h1 "Error"]
                       [:p "No sessions declared."]])

(def no-ensemble-error [:body
                        [:h1 "Error"]
                        [:p "No ensembles declared."]])

(defn find-duplicates [f src]
  (second
   (first
    (filter
     (fn [[k v]] (< 1 (count v)))
     (reduce
      (fn [m x]
        (update m (f x) #(conj (or % []) x)))
      {}
      src)))))

(defn execute [input]
  (if-let [error-code (:error input)]
    (case error-code
      :failed-to-parse (failed-to-parse input)
      :section-not-recognized (section-not-recognized input))
    (let [input (compress-sections input)
          bad-ensemble-names (find-duplicates
                              :name
                              (:ensembles input))]
      (cond
        (empty? (:sessions input)) no-session-error
        (empty? (:ensembles input)) no-ensemble-error
        bad-ensemble-names [:h1 "Error dup"]

        :default
        (let [session-instruments (instrument-set
                                   (:sessions input))
              ensemble-instruments (instrument-set
                                    (:ensembles input))
              instrument-diff (cljset/difference
                               session-instruments
                               ensemble-instruments)]
          (if (empty? instrument-diff)
            (execute-validated input)
            (uncovered-instruments instrument-diff)))))))

(defn render-page [hiccup-data]
  (let [tmp-file (File/createTempFile "chaconf" ".html")]
    (spit tmp-file
          (hiccup/html
           (page/html5
            hiccup-data)))
    (.browse (Desktop/getDesktop)
             (.toURI tmp-file))))

(defn process-file [filename]
  (-> filename
      slurp
      parser/parse
      execute
      render-page
      ))

;; (process-file (io/file (io/resource "config.txt")))
;; (process-file (io/file (io/resource "config_single.txt")))
;; (process-file (io/file (io/resource "config_bad0.txt")))
;; (process-file (io/file (io/resource "config_bad1.txt")))
;; (process-file (io/file (io/resource "config_bad2.txt")))
;; (process-file (io/file (io/resource "config_bad3.txt")))
;; (process-file (io/file (io/resource "config_bad4.txt")))


