(ns mango.core
  (:require [ring.adapter.jetty :as ring]
            [environ.core :refer [env]]
            [mango.routes :as routes]
            [mango.config :as config]
            [mango.db :as db]))

(defn -main [& args]
  (db/init)
  (ring/run-jetty routes/application {:port config/port :join? false}))
