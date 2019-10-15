;; https://github.com/yogthos/markdown-clj
(ns mango.hydrate
  (:require
   [markdown.core :as md]
   [mango.auth :as auth]
   [mango.config :as config]
   [mango.dataprovider :as dp]
   [mango.util :refer [url-encode]]
   [clojure.string :as str]))

(defn media
  "Hydrates a media item"
  [item]
  (assoc item :src (str config/cdn-url (url-encode (:filename item)))))

(defn medias
  "Hydrates a collection of media"
  [items]
  (map media items))

(defn media-collection
  "Hydrates the media collection of item"
  [data-provider {media-ids :media :as item}]
  (if-let [media-items (dp/media-by-ids data-provider media-ids)]
    (assoc item :media (map media media-items))
    item))

(defn user
  "Hydrates the user field of x"
  [data-provider {user-id :user :as x}]
  (if (not (nil? user-id))
    (assoc x :user (auth/public-user (dp/user-by-id data-provider user-id)))
    x))

(defn load-emoji
  []
  (let [emoji (clojure.data.json/read-json (slurp "resources/public/emoji.json"))]
    (reduce (fn [coll p] (assoc coll (:name p) (:char p))) emoji)))

(def emoji-map (memoize load-emoji))

(def emoji-replace-pattern (. java.util.regex.Pattern compile ":(.*):"))

(defn replace-emoji
  [s]
  (str/replace s emoji-replace-pattern #(or (get (emoji-map) (second %)) (first %))))

(defn content
  "Hydrates the content for an item."
  [{content :content :as item}]
  (if-not (nil? content)
    (assoc item :rendered-content (md/md-to-html-string (replace-emoji content) :footnotes? true))
    item))

(defn article
  "Hydrates a single article with content, users, and media"
  [data-provider article]
  (->> article
       (content)
       (user data-provider)
       (media-collection data-provider)))

(defn articles
  "Hydrates articles with content, users, and media"
  [data-provider articles]
  (map #(article data-provider %) articles))

(defn page
  "Hydrates a page with content and media"
  [data-provider page]
  (->> page
       (content)
       (media-collection data-provider)))

(defn pages
  "Hydrates pages with content and media"
  [data-provider pages]
  (map #(page data-provider %) pages))
