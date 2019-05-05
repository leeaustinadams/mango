(defproject mango :lein-v
  :description "Mango - A blogging site written with Clojure"
  :url "https://www.4d4ms.com/mango"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.incubator "0.1.4"]
                 [environ "1.1.0"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [compojure "1.6.0"]
                 [cheshire "5.5.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [crypto-password "0.2.0"]
                 [stencil "0.5.0"]
                 [markdown-clj "1.0.8"]
                 [clj-time "0.14.2"]
                 [amazonica "0.3.76"]
                 [yogthos/config "0.8"]
                 [com.taoensso/timbre "4.10.0"]]
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
             :repl {:init-ns mango.core
                    :init (use 'mango.core :reload)}
             }
  :prep-tasks [["v" "cache" "src"]
               ["minify-assets"]
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
