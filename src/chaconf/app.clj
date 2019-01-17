(ns chaconf.app
  (:import [java.awt Desktop]
           [java.net URI]
           [java.io File])
  (:require [chaconf.parser :as parser]
            [chaconf.core :as core]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.string :as cljstr]))

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

(defn execute [input]
  (if-let [error-code (:error input)]
    (case error-code
      :failed-to-parse (failed-to-parse input)
      :section-not-recognized (section-not-recognized input))))

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
      render-page))

;; (process-file (io/file (io/resource "config.txt")))
;; (process-file (io/file (io/resource "config_bad0.txt")))
;; (process-file (io/file (io/resource "config_bad1.txt")))

