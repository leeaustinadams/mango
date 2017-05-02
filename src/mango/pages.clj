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
   :image (let [img_src (get (first media) :src)]
            (if (empty? img_src)
              "https://cdn.4d4ms.com/img/A.jpg"
              (str "https://cdn.4d4ms.com/blog/" img_src)))
   :content (:rendered-content article)})

(defn article-for-bots
  "Render an article. Expects media to have been hydrated"
  [article url]
  (let [media (:media article)]
    (stencil/render-file "templates/article_for_bots.html" (tags url article media))))

(defn sitemap
  "Render a sitemap for indexing"
  [urls]
  (let [url-list (for [u urls] (hash-map :url u))]
    (stencil/render-file "templates/sitemap.txt" {:urls url-list})))
