;; https://github.com/yogthos/markdown-clj
(ns mango.hydrate
  (:require
            [markdown.core :as md]
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.dataprovider :as dp]))

(defn media
  "Hydrates the media collection of x"
  [data-provider x]
  (let [media-ids (:media x)]
    (if-let [media (dp/media-by-ids data-provider media-ids)]
      (let [hydrated-media (map #(assoc % :src (str config/cdn-url "/" (:src %))) media)]
        (assoc x :media hydrated-media))
      x)))

(defn user
  "Hydrates the user field of x"
  [data-provider x]
  (let [user-id (:user x)]
    (if (not (nil? user-id))
      (assoc x :user (auth/public-user (dp/user-by-id data-provider user-id)))
      x)))

(defn content
  "Hydrates the content for an article."
  [article]
  (let [content (:content article)]
    (if (not (nil? content))
      (assoc article :rendered-content (md/md-to-html-string content :footnotes? true :inhibit-separator "|"))
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
