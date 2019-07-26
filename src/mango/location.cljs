(ns mango.location)

(defn path
  []
  (.-pathname js/location))

(defn browse-to
  [url]
  (.assign js/location url))
