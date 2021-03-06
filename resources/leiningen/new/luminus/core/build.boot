(set-env!
 :dependencies '[<<dependencies>>]<% if resource-paths %>
 :source-paths <<source-paths>>
 :resource-paths <<resource-paths>><% endif %>)

(require '[adzerk.boot-test :refer [test]]
         '[luminus.boot-cprop :refer [cprop]])
<% if cljs %>
(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl]])
<% endif %><% if sassc-config-params %>
(require '[deraen.boot-sass :refer [sass]])
<% endif %>
(deftask dev
  "Enables configuration for a development setup."
  []
  (set-env!
   :source-paths #(conj % "env/dev/clj"<% if cljs %> <<dev-cljs.source-paths>><% endif %>)
   :resource-paths #(conj % "env/dev/resources")
   :dependencies #(concat % '[[prone "1.1.4"]
                              [ring/ring-mock "0.3.0"]
                              [ring/ring-devel "1.6.1"]<% if war %>
                              <<dev-http-server-dependencies>><% endif %>
                              [pjstadig/humane-test-output "0.8.2"]<% if dev-dependencies %>
                              <<dev-dependencies>><% endif %>]))
  (task-options! repl {:init-ns 'user})
  (require 'pjstadig.humane-test-output)
  (let [pja (resolve 'pjstadig.humane-test-output/activate!)]
    (pja))
  (cprop :profile :profiles/dev))

(deftask testing
  "Enables configuration for testing."
  []
  (dev)
  (set-env! :resource-paths #(conj % "env/test/resources"))<% if cljs %>
  (merge-env! :source-paths <<dev-cljs.test.source-paths>>)<% endif %>
  (cprop :profile :profiles/test))

(deftask prod
  "Enables configuration for production building."
  []
  (merge-env! :source-paths #{"env/prod/clj"<% if cljs %> "env/prod/cljs"<% endif %>}
              :resource-paths #{"env/prod/resources"})
  (cprop :profile :profiles/prod))

(deftask start-server
  "Runs the project without building class files.

  This does not pause execution. Combine with a wait task or use the \"run\"
  task."
  []
  (require '<<project-ns>>.core)
  (let [start-app (resolve '<<project-ns>>.core/start-app)]
    (with-pass-thru _
      (start-app nil))))

(deftask run
  "Starts the server and causes it to wait."
  []
  (comp
   (apply start-server *args*)
   (wait)))
<% if cljs %>
(require '[clojure.java.io :as io])
(require '[crisptrutski.boot-cljs-test :refer [test-cljs]])
(deftask figwheel
  "Runs figwheel and enables reloading."
  []
  (dev)
  (require '[powerlaces.boot-figreload :refer [reload]])
  (let [reload (resolve 'powerlaces.boot-figreload/reload)]
    (comp
     (start-server)
     (watch)
     (cljs-repl)
     (reload :client-opts {:debug true})
     (speak)
     (cljs))))

(deftask run-cljs-tests
  "Runs the doo tests for ClojureScript."
  []
  (comp
   (testing)
   (test-cljs :cljs-opts <<dev-cljs.test.compiler>>)))
<% endif %>
(deftask uberjar
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (prod)
   (aot :namespace #{'<<project-ns>>.core})<% if cljs %>
   (cljs :optimizations :advanced)<% endif %>
   (uber)
   (jar :file "<<name>>.jar" :main '<<project-ns>>.core)
   (sift :include #{#"<<name>>.jar"})
   (target)))
<% if war %>
(require '[boot.immutant :refer [gird]])
(deftask uberwar
  "Creates a war file ready to deploy to wildfly."
  []
  (comp
   (uber :as-jars true)
   (aot :all true)
   (gird :init-fn '<<project-ns>>.handler/init)
   (war)
   (target)))

(deftask dev-war
  "Creates a war file for development and testing."
  []
  (comp
   (dev)
   (gird :dev true :init-fn '<<project-ns>>.handler/init)
   (war)
   (target)))
<% endif %>
