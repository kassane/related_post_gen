(defproject related "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ; [cheshire "5.12.0"]
                 ; [metosin/jsonista "0.3.8"]
                 ; [com.dslplatform/dsl-json "2.0.2"]
                 ; [com.alibaba/fastjson "2.0.41"]
                 [com.google.code.gson/gson "2.10.1"]]

  :profiles {:uberjar {:aot :all} }

  :uberjar-name "related.jar"

  :main related.core

  :repl-options {:init-ns related.core})
