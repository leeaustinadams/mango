;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.config :as config]
            [stencil.core :as stencil]))

(defn not-found
  "Render a page for when a URI is not found"
  []
  (stencil/render-file "templates/simple_error.html"
                       { :title "The page you are looking for could not be found."}))

(defn index
  "Render the root html"
  [user]
  (stencil/render-file "templates/index.html"
                       {
                        :title config/site-title
                        :description config/site-description
                        :adminEmail config/admin-email
                        :version config/version
                        :user user}))

(defn tags
  [url article media]
  {:url url
   :card "summary"
   :site config/twitter-site-handle
   :creator config/twitter-creator-handle
   :title (:title article)
   :description (:description article)
   :image (if (not (nil? media))
            (-> media
                first
                :src)
            "http://cdn.4d4ms.com/img/A.jpg")})

(defn article-for-twitter
  "Render an article stub with metatags for Twitter Cards. Expects media to have been hydrated"
  [article url]
  (let [media (:media article)]
    (stencil/render-file "templates/article_for_twitter.html" (tags url article media))))

(defn article-for-facebook
  "Render an article stub with open graph tags. Expects media to have been hydrated"
  [article url]
  (let [media (:media article)]
    (stencil/render-file "templates/article_for_facebook.html" (tags url article media))))
