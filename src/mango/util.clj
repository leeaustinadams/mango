(ns mango.util
  (:require [clojure.string :as str]
            [clojure.java.io :refer [resource]]
            [cheshire.generate :refer [add-encoder encode-str]]
            [clj-time.format :as time-format]
            [taoensso.timbre :as timbre :refer [debugf]])
  (:import org.bson.types.ObjectId
           java.net.URLEncoder
           java.net.URLDecoder))

(add-encoder org.bson.types.ObjectId encode-str)

(defn slugify
  "Make a slug from a title with an optional limit on the number of words"
  [title & {:keys [limit]}]
  (let [clean (str/join (filter #(re-matches #"[a-zA-Z0-9\s]" (str %)) title))
        tokens (map str/lower-case (str/split clean #"\s"))
        filtered (filter #(not (empty? %)) tokens)]
    (str/join "-" (take (or limit (count filtered)) filtered))))

(defn author-name
  [first-name last-name]
  (when (and first-name last-name)
    (str first-name " " last-name)))

(defn xform-ids
  "Transforms a comma seperated string of ids to a collection of ObjectIds"
  [ids]
  (when-not (empty? ids)
    (map (comp #(ObjectId. %) str/trim) (str/split ids #","))))

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
  (when-not (empty? tags)
    (mapv (comp str/lower-case str/trim) (str/split tags #","))))

(defn url-encode
  "URL encodes a string"
  [s]
  (URLEncoder/encode s))

(defn url-decode
  "URL decodes a string"
  [s]
  (URLDecoder/decode s))

(defn str-or-nil
  [s]
  (when-not (clojure.string/blank? s) s))

(defn load-edn
  [resource-name]
  (debugf "res: %s" resource-name)
  (when-let [file-url (resource resource-name)]
    (with-open [r (clojure.java.io/reader file-url)]
      (debugf "url: %s" file-url)
      (clojure.edn/read (java.io.PushbackReader. r)))))
