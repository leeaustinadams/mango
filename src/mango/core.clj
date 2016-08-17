(ns mango.core
  (:require [ring.adapter.jetty :as ring]
            [environ.core :refer [env]]
            [mango.routes :as routes]
            [mango.config :as config])
  (:gen-class))

(defn -main [& args]
  (let [port (when (:port env) (. Integer parseInt (:port env)))]
    (ring/run-jetty routes/application {:port (or port config/port) :join? false})))
