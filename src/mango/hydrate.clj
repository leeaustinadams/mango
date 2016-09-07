(ns mango.hydrate
  (:require 
            [markdown.core :as md]
            [mango.auth :as auth]
            [mango.db :as db])
  (:gen-class))

(defn media
  "Hydrates the media collection of x"
  [x]
  (let [media-ids (:media x)]
    (if (not (nil? media-ids))
      (assoc x :media (db/blog-media-by-ids media-ids))
      x)))

(defn user 
  "Hydrates the user field of x"
  [x]
  (let [user-id (:user x)]
    (if (not (nil? user-id))
      (assoc x :user (auth/public-user (db/user-by-id user-id)))
      x)))

(defn content
  "Hydrates the content for an article."
  [article]
  (let [content (:content article)]
    (if (not (nil? content))
      (assoc article :rendered-content (md/md-to-html-string content :footnotes? true))
      article)))

(defn articles
  "Hydrates a page worth of articles"
  [source page per-page]
  (let [page (if page (Integer. page) 0)
        per-page (if per-page (Integer. per-page) 10)
        articles (source :page page :per-page per-page)]
        (map media (map user (map content articles)))))
