(ns mango.hydrate
  (:require
            [markdown.core :as md]
            [mango.auth :as auth]
            [mango.db :as db])
  (:gen-class))

(defprotocol DataProvider
  (media-by-ids [this ids])
  (user-by-id [this id]))

(defn media
  "Hydrates the media collection of x"
  [data-provider x]
  (let [media-ids (:media x)]
    (if (not (nil? media-ids))
      (assoc x :media (media-by-ids data-provider media-ids))
      x)))

(defn user
  "Hydrates the user field of x"
  [data-provider x]
  (let [user-id (:user x)]
    (if (not (nil? user-id))
      (assoc x :user (auth/public-user (user-by-id data-provider user-id)))
      x)))

(defn content
  "Hydrates the content for an article."
  [article]
  (let [content (:content article)]
    (if (not (nil? content))
      (assoc article :rendered-content (md/md-to-html-string content :footnotes? true))
      article)))

(defn article
  "Hydrates a single article with content, users, and media"
  [data-provider article]
  (->> article
       (content)
       (user data-provider)
       (media data-provider)))

(defn articles
  "Hydrates articles with content, users, and media"
  [data-provider articles]
  (map #(article data-provider %) articles))
