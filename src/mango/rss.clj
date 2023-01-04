(ns mango.rss
  (:require [clj-rss.core :as rss]))

(defn- article-list-item
  "Render an article list item"
  [{:keys [title description tags media created rendered-content status slug] {author-user-name :username author-first-name :first-name author-last-name :last-name  author-twitter-handle :twitter-handle} :user :as article} site-url feed-url]
  {:title title
   :description (str "<![CDATA[ " rendered-content " ]]>")
   :author author-user-name
   :category tags
   :pubDate (.toInstant (.toDate created)) ; clj-rss uses java.time.Instant
   :link (str site-url "/blog/" slug)
   :source feed-url})

(defn articles-list
  "Render a list of articles"
  [admin-email site-title site-description site-url site-copyright site-language feed-url articles]
  (rss/channel-xml {:title site-title
                    :link site-url
                    :description site-description
                    :feed-url feed-url
                    :managingEditor admin-email
                    :webMaster admin-email
                    :language site-language
                    :copyright site-copyright}
                   (map #(article-list-item % site-url feed-url) articles)))
                
