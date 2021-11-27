(defproject mango :lein-v
  :description "Lee's Website"
  :url "https://www.4d4ms.com"
  :license {:name "MIT License"
            :url "https://tldrlegal.com/license/mit-license"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.bhauman/figwheel-main "0.2.0"]
                 [environ "1.1.0"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [compojure "1.6.0"]
                 [cheshire "5.5.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [crypto-password "0.2.0"]
                 [stencil "0.5.0"]
                 [markdown-clj "1.10.1"]
                 [clj-time "0.14.2"]
                 [amazonica "0.3.152"]
                 [yogthos/config "1.1.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [binaryage/oops "0.7.0"]]
  :plugins [[com.roomkey/lein-v "6.2.2"]
            [io.sarnowski/lein-docker "1.0.0"]]
  :profiles {
             :dev {:dependencies [[org.clojure/clojurescript "1.10.339"]
                                  [com.bhauman/figwheel-main "0.2.0"]]
                   :resource-paths ["config/dev"]
                   :clean-targets ^{:protect false} ["target"
                                                     "resources/public/js/prod-main.js"]}
             :prod {:resource-paths ["config/prod"]
                    :clean-targets ^{:protect false} ["target"
                                                      "resources/public/cljs-out"
                                                      "resources/public/js/dev-main.js"]}
             :test {:resource-paths ["config/test"]
                    :clean-targets ["target"]}
             :uberjar {:aot :all}
             :repl {:init-ns mango.core
                    :init (use 'mango.core :reload)}
             }

  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev"]
            "fig-repl" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "build-dev-client" ["trampoline" "run" "-m" "figwheel.main" "-bo" "dev"]
            "build-prod-client" ["trampoline" "run" "-m" "figwheel.main" "-bo" "prod"]
            "build-dev-server" ["with-profile" "dev" "uberjar"]
            "build-prod-uberjar" ["with-profile" "prod" "uberjar"]}

  :docker {}

  :prep-tasks [["v" "cache" "src"]
               "javac" "compile"]
  :release-tasks [["vcs" "assert-committed"]
                  ["v" "update"]]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :main mango.core)
