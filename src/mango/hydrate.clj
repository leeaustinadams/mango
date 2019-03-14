;; https://github.com/yogthos/markdown-clj
(ns mango.hydrate
  (:require
            [markdown.core :as md]
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.dataprovider :as dp]
            [mango.util :refer [url-encode]]))

(defn media
  "Hydrates a media item"
  [item]
  (assoc item :src (str config/cdn-url (url-encode (:filename item)))))

(defn medias
  "Hydrates a collection of media"
  [items]
  (map media items))

(defn media-collection
  "Hydrates the media collection of x"
  [data-provider {media-ids :media :as x}]
  (if-let [media-items (dp/media-by-ids data-provider media-ids)]
    (assoc x :media (map media media-items))
    x))

(defn user
  "Hydrates the user field of x"
  [data-provider {user-id :user :as x}]
  (if (not (nil? user-id))
    (assoc x :user (auth/public-user (dp/user-by-id data-provider user-id)))
    x))

(defn content
  "Hydrates the content for an article."
  [{content :content :as article}]
  (if (not (nil? content))
    (assoc article :rendered-content (md/md-to-html-string content :footnotes? true :inhibit-separator "|"))
    article))

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
