(ns mango.util
  (:require [clojure.string :as str]
            [cheshire.generate :refer [add-encoder encode-str]]
            [clj-time.format :as time-format])
  (:import org.bson.types.ObjectId))

(add-encoder org.bson.types.ObjectId encode-str)

(defn slugify
  "Make a slug from a title with an optional limit on the number of words"
  [title & {:keys [limit]}]
  (let [clean (str/join (filter #(re-matches #"[a-zA-Z0-9\s]" (str %)) title))
        tokens (map #(str/lower-case %) (str/split clean #"\s"))
        filtered (filter #(not (empty? %)) tokens)]
    (str/join "-" (take (or limit (count filtered)) filtered))))

(defn xform-ids
  "Transforms a comma seperated string of ids to a collection of ObjectIds"
  [ids]
  (when (not (empty? ids)) (map #(ObjectId. %) (map str/trim (str/split ids #",")))))

(defn xform-string-to-time
  "Transforms a timestring into a time object"
  [time]
  (when time
    (time-format/parse (time-format/formatters :date) time)))

(defn xform-time-to-string
  "Transforms a time object into a string"
  [time]
  (when time
    (time-format/unparse (time-format/formatters :date) time)))

(defn xform-tags
  "Make a vector of tags from a comma separated list"
  [tags]
  (when (not (empty? tags))
    (mapv #(str/trim %) (str/split tags #","))))

(defn csv
  "Make a string of comma separated values from s."
  [s]
  (let [e (interleave s (repeat ", "))]
    (reduce str (take (dec (count e)) e))))
