(ns mango.util
  (:require [clojure.string :as str])
  (:gen-class))

(defn slugify
  "Make a slug from a title with an optional limit on the number of words"
  [title & {:keys [limit]}]
  (let [clean (str/join (filter #(re-matches #"[a-zA-Z0-9\s]" (str %)) title))
        tokens (map #(str/lower-case %) (str/split clean #"\s"))
        filtered (filter #(not (empty? %)) tokens)]
    (str/join "-" (take (or limit (count filtered)) filtered))))
