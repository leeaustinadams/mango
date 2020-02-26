;; https://github.com/dakrone/cheshire
(ns mango.json-api
  (:require
   [cheshire.core :refer [generate-string]]
   [mango.auth :as auth]
   [mango.dataprovider :as dp]
   [mango.hydrate :as hydrate]))

(defn json-status
  "A JSON response for a status. Generates JSON from the obj passed in"
  [status obj & rest]
  (reduce merge {
                 :status status
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn json-success
  "A JSON response for a success (200). Generates JSON from the obj passed in"
  [obj & [rest]]
  (json-status 200 obj rest))
