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
  []
  (stencil/render-file "templates/index.html"
                       {
                        :title config/site-title
                        :description config/site-description
                        :adminEmail config/admin-email
                        :version config/version}))

(defn article-for-twitter
  "Render an article stub with metatags for Twitter Cards. Expects media to have been hydrated"
  [article]
  (let [media (:media article)]
    (stencil/render-file "templates/article_for_twitter.html"
                         {:card "summary"
                          :site "@tester_ladams"
                          :creator "@tester_ladams"
                          :title (:title article)
                          :description (:content article)
                          :image (if (not (nil? media))
                                   (-> media
                                       first
                                       :src)
                                   "http://www.4d4ms.com/cards/3.png")})))
