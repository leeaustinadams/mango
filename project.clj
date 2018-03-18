(defproject mango :lein-v
  :description "Lee's Website"
  :url "https://www.4d4ms.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [environ "1.1.0"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.2"]
                 [compojure "1.4.0"]
                 [cheshire "5.5.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [crypto-password "0.2.0"]
                 [stencil "0.5.0"]
                 [markdown-clj "1.0.2"]
                 [clj-time "0.14.2"]
                 [amazonica "0.3.76"]
                 [yogthos/config "0.8"]]
  :plugins [[lein-asset-minifier "0.4.4"]
            [com.roomkey/lein-v "6.2.2"]
            [lein-shell "0.5.0"]]
  :minify-assets [[:css {:source "dev-resources/css/mango.css" :target "dev-resources/css/mango.min.css"}]
                  [:js {:source "dev-resources/js/mango.js" :target "dev-resources/js/mango.min.js" :opts {:optimization :none}}]]
  :profiles {
             :dev {:resource-paths ["config/dev"]}
             :prod {:resource-paths ["config/prod"]}
             :test {:resource-paths ["config/test"]}
             :uberjar {:aot :all}
             :repl {:dependencies [[org.clojure/tools.nrepl "0.2.12"]]}
             }
  :prep-tasks [["v" "cache" "src"]
               ["shell" "rm" "-f" "resources/public/js/mango-*.js"]
               ["shell" "cp" "dev-resources/js/mango.min.js" "resources/public/js/mango-${:version}.min.js"]
               ["shell" "rm" "-f" "resources/public/css/mango-*.css"]
               ["shell" "cp" "dev-resources/css/mango.min.css" "resources/public/css/mango-${:version}.min.css"]
               "javac" "compile"]
  :release-tasks [["vcs" "assert-committed"]
                  ["v" "update"]]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :main mango.core)
