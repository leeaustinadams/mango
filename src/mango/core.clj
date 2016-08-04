(ns mango.core
  (:require [ring.adapter.jetty :as ring]
            [environ.core :refer [env]]
            [mango.routes :as routes])
  (:gen-class))

(defn -main [& args]
  (let [port (. Integer parseInt (or (env :port) "8080"))]
    (ring/run-jetty routes/application {:port port :join? false})))
