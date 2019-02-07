(defproject chaconf "0.1.2-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main chaconf.app
  :aot :all
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.choco-solver/choco-solver "4.10.0"]
                 [hiccup "1.0.5"]
                 [dk.ative/docjure "1.12.0"]])
