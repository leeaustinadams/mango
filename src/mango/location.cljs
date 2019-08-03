(ns mango.location
  (:require [oops.core :refer [oget]]))

(defn path
  []
  (oget js/location "pathname"))

(defn browse-to
  [url]
  (.assign js/location url))
