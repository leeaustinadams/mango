(ns mango.core
  (:require [ring.adapter.jetty :as ring]
            [environ.core :refer [env]]
            [mango.routes :as routes]
            [mango.bootstrap :as bootstrap]
            [mango.config :as config]
            [mango.db :as db])
  (:gen-class))

(defn -main [& args]
  (db/init)
  (bootstrap/run)
  (ring/run-jetty routes/application {:port config/port :join? false}))
