(ns mango.util
  (:require [clojure.string :as str])
  (:gen-class))

(defn slugify
  "Make a slug from a title with an option limit on the number of words"
  [title & {:keys [limit]}]
  (let [tokens (map #(str/lower-case %) (str/split title #"\s"))]
    (str/join "-" (take (or limit (count tokens)) tokens))))
