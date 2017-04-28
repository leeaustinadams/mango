(defproject mango "0.1.11"
  :description "Lee's Website"
  :url "http://www.4d4ms.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [environ "1.1.0"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.2"]
                 [compojure "1.4.0"]
                 [cheshire "5.5.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [crypto-random "1.2.0"]
                 [crypto-password "0.2.0"]
                 [stencil "0.5.0"]
                 [markdown-clj "0.9.97"]
                 [clj-time "0.12.0"]
                 [amazonica "0.3.76"]
                 [yogthos/config "0.8"]]
  :profiles {
             :dev {:resource-paths ["config/dev"]}
             :prod {:resource-paths ["config/prod"]}
             :test {:resource-paths ["config/test"]}
             :uberjar {:aot :all}
             :repl {:dependencies [[org.clojure/tools.nrepl "0.2.12"]]}
             }
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]]
  :main mango.core)
