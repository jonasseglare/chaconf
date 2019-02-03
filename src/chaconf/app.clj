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
            [clojure.pprint :as pp]
            [chaconf.htmloutput :as htmloutput]
            [chaconf.exceloutput :as exceloutput]
            [dk.ative.docjure.spreadsheet :as spreadsheet])
  (:gen-class))


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
    [:html (case error-code
             :failed-to-parse
             (htmloutput/failed-to-parse input)
             :section-not-recognized
             (htmloutput/section-not-recognized input))]
    (let [input (compress-sections input)
          bad-ensemble-names (find-duplicates
                              :name
                              (:ensembles input))]
      (cond
        (empty? (:sessions input)) [:html
                                    htmloutput/no-session-error]
        (empty? (:ensembles input)) [:html
                                     htmloutput/no-ensemble-error]
        bad-ensemble-names [:html
                            (htmloutput/duplicate-name-error
                             bad-ensemble-names)]
        :default
        (let [session-instruments (core/instrument-set
                                   (:sessions input))
              ensemble-instruments (core/instrument-set
                                    (:ensembles input))
              instrument-diff (cljset/difference
                               session-instruments
                               ensemble-instruments)]
          (if (empty? instrument-diff)

            ;; Choose output format here.
            ;;(exceloutput/execute-validated input)
            [:html (htmloutput/execute-validated input)]
            
            [:html (htmloutput/uncovered-instruments
                    instrument-diff)]))))))

(defn render-output [[data-type data]]
  (case data-type
    :html (let [tmp-file (File/createTempFile
                          "chaconf" ".html")]
            (spit tmp-file
                  (hiccup/html
                   (page/html5
                    data)))
            (.browse (Desktop/getDesktop)
                     (.toURI tmp-file)))
    :excel (let [tmp-file (File/createTempFile
                           "chaconf" ".xlsx")]
             (spreadsheet/save-workbook!
              (.getAbsolutePath tmp-file)
              data)
             (.open (Desktop/getDesktop)
                    tmp-file))))

(defn process-file [filename]
  (-> filename
      slurp
      parser/parse
      execute
      render-output
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
;; (process-file (io/file (io/resource "miniconfig.txt")))
;; (process-file (io/file (io/resource "miniconfig2.txt")))
;; (process-file (io/file (io/resource "config_single.txt")))
;; (process-file (io/file (io/resource "config_bad0.txt")))
;; (process-file (io/file (io/resource "config_bad1.txt")))
;; (process-file (io/file (io/resource "config_bad2.txt")))
;; (process-file (io/file (io/resource "config_bad3.txt")))
;; (process-file (io/file (io/resource "config_bad4.txt")))
;; (process-file (io/file (io/resource "config_bad5.txt")))
;; (process-file (io/file (io/resource "config_bad6.txt")))
