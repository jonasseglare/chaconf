(ns chaconf.app
  (:import [java.awt Desktop]
           [java.net URI]
           [java.io File]
           [javax.swing
            JFileChooser
            JOptionPane])
  (:require [chaconf.parser :as parser]
            [chaconf.core :as core]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.string :as cljstr]
            [clojure.set :as cljset]
            [clojure.pprint :as pp])
  (:gen-class))

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

(def table-style
  [:style
   "td, th {border: 1px solid black; padding: 0.5em;}"])

(defn render-count-table [m]
  (list   
   [:table
    [:tr [:th "Instrument"] [:th "Count"]]
    (for [[k v] m]
      [:tr
       [:td k] [:td v]])]))

(defn list-counts [ensemble instruments n]
  (for [inst instruments]
    [:td
     (if-let [m (get ensemble inst)]
       (* m n))]))

(defn render-solution [sol input ensemble-name-lookup]
  (let [inst (vec (instrument-set (:ensembles input)))]
    [:table
     [:tr
      [:th "Ensemble"]
      (map (fn [i] [:th i]) inst)
      (map (fn [i] [:th i " (total)"]) inst)
      [:th "Count"]]
     (for [[ensemble n] sol]
       (when (< 0 n)
         [:tr
          [:td (get ensemble-name-lookup ensemble)]
          (list-counts ensemble inst 1)
          (list-counts ensemble inst n)
          [:td n]]))]))

(defn render-solution-set [sol input ensemble-label-map]
  (list
   (if (empty? sol)
     [:p "No solutions found"]
     (list
      (let [sol-count (count sol)]
        (list
         [:p (format "Found %d solution(s)" sol-count)]
         (for [[index single-sol] (map vector  (range) sol)]
           (list
            [:h2 (format "Solution %d/%d: %d teachers"
                         (inc index) sol-count
                         (core/teacher-count single-sol))]
            (render-solution single-sol input
                             ensemble-label-map)))))))))

(def footer
  (list
   [:h1 "About"]
   [:p "Generated using Chaconf"]
   [:div
    [:a {:href "https://github.com/jonasseglare/chaconf"}
     "https://github.com/jonasseglare/chaconf"]]))

(defn render-single [input setup sol ensemble-name-lookup]
  [:body
   table-style
   [:h1 "Solution"]
   [:h2 "Participants"]
   (render-count-table (first (::core/sessions setup)))
   [:h2 "Solutions"]
   (-> sol
       :all
       first
       (render-solution-set input ensemble-name-lookup))
   footer])

(defn render-multiple [input setup sol ensemble-name-lookup]
  [:body
   [:h1 "Solution"]
   table-style
   (let [tc (sort (:teacher-counts sol))
         found-tc (not (empty? tc))
         key (if found-tc :sliced :all)
         sols (get sol key)
         sessions (:sessions input)]
     (assert (= (count sols) (count sessions)))
     (list
      (if (not found-tc)
        [:p "No single teacher count works for all sessions"]
        [:p "Found these teacher counts that work for all sessions: "
         (cljstr/join ", " tc)])
      (for [[i session-info session-sol] (map vector
                                              (range)
                                              sessions
                                              sols)]
        (list
         [:h2 (format "Session %s" (:name session-info))]
         (render-solution-set session-sol
                              input
                              ensemble-name-lookup)))))
   footer])



(defn execute-validated [input]
  (let [ensembles (:ensembles input)
        session-setup {::core/ensembles
                       (mapv :values ensembles)
                       ::core/sessions
                       (mapv :values (:sessions input))}
        sol (core/solve-sessions session-setup)
        session-count (count (:sessions input))
        ensemble-name-lookup (zipmap
                              (map :values ensembles)
                              (map :name ensembles))]
    (if (= 1 (count (:sessions input)))
      (render-single input session-setup sol ensemble-name-lookup)
      (render-multiple input session-setup sol ensemble-name-lookup))))

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

(defn duplicate-name-error [data]
  [:body
   [:h1 "Error"]
   [:p (format "The ensemble name '%s' is used more than once"
               (-> data first :name))]])

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
        bad-ensemble-names (duplicate-name-error
                            bad-ensemble-names)

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

(defn get-filename-from-dialog []
  (let [fc (JFileChooser.)]
    (if (= JFileChooser/APPROVE_OPTION
           (.showOpenDialog fc nil))
      (.getSelectedFile fc))))

(defn get-filename [args]
  (if (empty? args)
    (get-filename-from-dialog)
    (first args)))

(defn -main [& args]
  (if-let [filename (get-filename args)
           ]
    (let [file (io/file filename)]
      (if (.exists file)
        (process-file filename)
        (JOptionPane/showMessageDialog
         nil
         (format "The file '%s' does not exist."
                 filename)
         "No such file"
         JOptionPane/ERROR_MESSAGE)))))

;; (process-file (io/file (io/resource "config.txt")))
;; (process-file (io/file (io/resource "config_single.txt")))
;; (process-file (io/file (io/resource "config_bad0.txt")))
;; (process-file (io/file (io/resource "config_bad1.txt")))
;; (process-file (io/file (io/resource "config_bad2.txt")))
;; (process-file (io/file (io/resource "config_bad3.txt")))
;; (process-file (io/file (io/resource "config_bad4.txt")))
;; (process-file (io/file (io/resource "config_bad5.txt")))
;; (process-file (io/file (io/resource "config_bad6.txt")))



