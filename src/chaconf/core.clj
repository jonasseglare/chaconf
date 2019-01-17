(ns chaconf.core
  (:import [org.chocosolver.solver Model]
           [org.chocosolver.solver.variables IntVar])
  (:require [clojure.spec.alpha :as spec]
            [clojure.set :as cljset]))

(spec/def ::num-map (spec/map-of any? int?))
(spec/def ::participants ::num-map)
(spec/def ::ensemble ::num-map)
(spec/def ::ensembles (spec/coll-of ::ensemble))

(spec/def ::setup (spec/keys :req [::ensembles
                                   ::participants]))

(spec/def ::sessions (spec/coll-of ::participants))
(spec/def ::session-setup (spec/keys :req [::sessions ::ensembles]))


(defn all-instruments [x]
  (transduce
   (comp (map keys)
         cat)
   conj
   #{}
   (::ensembles x)))

(defn valididate-setup [x]
  (assert (spec/valid? ::setup x))
  (let [part-set (-> x ::participants keys set)
        ens-set (all-instruments x)]
    (when (not (cljset/subset? part-set ens-set))
      (throw (ex-info "Not a subset"
                      {:part-set part-set
                       :ens-set ens-set})))))

(defn assign-indices [coll]
  {:pre [(coll? coll)]}
  (zipmap coll (range (count coll))))

(defn compute-max-ensemble-count [participants ensemble]
  (transduce
   (comp (map (fn [[k v]]
                [(get participants k) v]))
         (filter (fn [[x _]] (not (nil? x))))
         (map (fn [[p v]] (int (Math/floor (/ p v))))))
   min
   Integer/MAX_VALUE
   ensemble))

(defn build-eqs-for-config [dst [i config]]
  (reduce
   (fn [dst [k v]]
     (update dst k (fn [dst] (conj (or dst []) {:factor v
                                                :index i}))))
   dst
   config))

(defn build-eqs [configs]
  (reduce
   build-eqs-for-config
   {}
   (map vector
        (range (count configs))
        configs)))

(defn build-eq [model pre-eq vars [instrument n]]
  (let [eq (get pre-eq instrument)
        var-array (into-array IntVar (map (comp (partial get vars) :index) eq))
        coeffs (int-array (map :factor eq))]
    (.post (.scalar model var-array coeffs "=" n))))

(defn get-solution [model configs vars]
  (zipmap
   configs
   (map #(.getValue %) vars)))

(defn count-solution-participants [sol]
  (reduce
   (fn [dst [ensemble n]]
     (reduce
      (fn [dst [k v]]
        (update dst k (fn [x] (+ (or x 0) (* v n)))))
      dst
      ensemble))
   {}
   sol))

(defn split-session-setup [session-setup]
  (let [b (select-keys session-setup [::ensembles])]
    (mapv
     (fn [session] (assoc b ::participants session))
     (::sessions session-setup))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn valid-solution? [solution participants]
  (= (count-solution-participants solution)
     participants))

(defn teacher-count [solution]
  (apply + (vals solution)))

(defn teacher-counts [solutions]
  (mapv teacher-count solutions))

(defn show-solutions [solutions]
  (doseq [sol solutions]
    (println "Solution with" (teacher-count sol) "teachers")
    (doseq [[config n] sol]
      (println "   " config ":" n))))

(defn solve [setup]
  (valididate-setup setup)
  (let [model (Model. "Chaconf")
        participants (::participants setup)
        instruments (vec (all-instruments setup))
        configs (-> setup ::ensembles set vec)
        instrument-map (assign-indices instruments)
        config-map (assign-indices configs)
        max-per-ensemble (vec (map (partial compute-max-ensemble-count participants)
                                   configs))
        vars (mapv (fn [c v]
                     (.intVar model (str c) 0 v))
                   configs
                   max-per-ensemble)
        pre-eqs (build-eqs configs)
        eqs (map (partial build-eq model pre-eqs vars) participants)]
    (doseq [e eqs])
    (loop [solutions []]
      (if (-> model
              .getSolver
              .solve)
        (let [sol (get-solution model configs vars)]
          (assert (valid-solution? sol participants))
          (recur (conj solutions sol)))
        (sort-by teacher-count solutions)))))

;; (count (solve setup))

(defn session-count [session-setup]
  (-> session-setup
      ::sessions
      count))

(defn solve-sessions [session-setup]
  {:pre (spec/valid? ::session-setup session-setup)}
  (let [setups (split-session-setup session-setup)
        solutions (mapv solve setups)
        teachers-per-session (mapv (comp set teacher-counts)
                                   solutions)]
    teachers-per-session))



(comment
 
  (do

    (def session-setup {::ensembles #{{:violin 2}
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
                        ::sessions [{:violin 20
                                     :cello 12
                                     :alto 5
                                     :piano 3}
                                    {:violin 22
                                     :cello 12
                                     :alto 4
                                     :piano 3}]})

    (session-count session-setup)
    (every? (partial spec/valid? ::setup)
              (split-session-setup session-setup))
    (first (split-session-setup session-setup))
    (solve-sessions session-setup)
    
   
   )


 )
