(ns chaconf.exceloutput
  (:require [dk.ative.docjure.core :as docjure]
            [dk.ative.docjure.spreadsheet :as spreadsheet]
            [chaconf.core :as core]))

(defn make-header [sol]
  (into ["Solution"]
        (map (fn [i] (inc i))
             (range (count sol)))))

(defn zero-to-nil [x]
  (if (not= x 0)
    x))

(defn make-table-row [ensemble-name ensemble sol]
  (into [ensemble-name]
        (map (fn [m] (zero-to-nil (get m ensemble))) sol)))

(defn make-teacher-count-row [sol]
  (into ["Teacher count"]
        (map core/teacher-count sol)))

(defn make-table [ensemble-name-lookup sol]
  (let [rows (sort-by (fn [[ensemble ensemble-name]]
                        (- (core/ensemble-participants ensemble)))
                      ensemble-name-lookup)]
    (reduce into
            [(make-header sol)]
            [(for [[ensemble ensemble-name] rows]
               (make-table-row ensemble-name ensemble sol))
             [(make-teacher-count-row sol)]])))

(defn render-workbook [teacher-counts name-sheet-pairs]
  (let [teacher-counts (set teacher-counts)
        wb (apply spreadsheet/create-workbook
                  (reduce into [] name-sheet-pairs))
        important-style (spreadsheet/create-cell-style!
                         wb {:font {:bold true}})]
    (doseq [[sheet-name table] name-sheet-pairs]
      (println "name:" sheet-name)
      (let [sheet (spreadsheet/select-sheet sheet-name wb)
            rows (spreadsheet/row-seq sheet)]
        (doseq [row rows]
          (spreadsheet/set-cell-style! (first row) important-style))
        (doseq [tc (rest (last rows))]
          (let [v (int (.getNumericCellValue tc))]
            (if (contains? teacher-counts v)
              (spreadsheet/set-cell-style! tc important-style))))))
    wb))

(defn execute-validated [input]
  (let [ensembles (:ensembles input)
        session-setup {::core/ensembles
                       (mapv :values ensembles)
                       ::core/sessions
                       (mapv :values (:sessions input))}
        sol (core/solve-sessions session-setup)
        session-count (count (:sessions input))
        ensemble-name-lookup (core/make-ensemble-name-lookup
                              ensembles)
        session-count (count (:sessions input))]
    [:excel
     (render-workbook
      (:teacher-counts sol)
      (for [[session-info solution]
            (map vector
                 (:sessions input)
                 (:all sol))]
        [(:name session-info)
         (make-table ensemble-name-lookup
                     solution)]))]))
