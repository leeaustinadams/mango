;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [clojure.data.json :as json]
            [stencil.core :as stencil]))

(defn render-page
  "Render a page"
  [template user & [vals]]
  (stencil/render-file template (merge
                                 (when user
                                   (let [auth-user (auth/public-user user)]
                                     {:js-logged-in-user (json/write-str auth-user)
                                      :logged-in-user auth-user}))
                                 {:app-css config/app-css}
                                 vals)))

(defn article-vals
  [article url]
  {:url url
   :card "summary"
   :site config/twitter-site-handle
   :article article
   :image (let [img_src (get (first (:media article)) :src)]
            (if (empty? img_src)
              "https://cdn.4d4ms.com/img/A.jpg"
              img_src))
   :tags (map (fn [t] {:tag t}) (:tags article))})

(defn article
  "Render an article. Expects media to have been hydrated"
  [user article url]
  (render-page "templates/article.html" user (article-vals article url)))

(defn articles
  "Render a list of articles"
  [user list-title articles]
  (let [app-css config/app-css]
    (render-page "templates/articles.html" user {:list-title list-title :articles (map (fn [a] {:article a}) articles)})))

(defn root
  "Render the root page"
  [user]
  (render-page "templates/root.html" user))

(defn photography
  "Render the photography page"
  [user]
  (render-page "templates/photography.html" user))

(defn about
  "Render the about page"
  [user]
  (render-page "templates/about.html" user))

(defn sign-in
  "Render the sign in page"
  [user & [message]]
  (render-page "templates/signin.html" user {:message message}))

(defn sign-in-success
  "Render the sign in success page"
  [user]
  (render-page "templates/signin_success.html" user))

(defn edit-article
  "Render the editing in page"
  [user & [article url]]
  (render-page "templates/edit_article.html" user (when article (article-vals article url))))

(defn not-found
  "Render a page for when a URI is not found"
  [user]
  (render-page "templates/simple_error.html" user
                      { :title "Not Found" :message "The page you are looking for could not be found."}))

(defn sitemap
  "Render a sitemap for indexing"
  [urls]
  (let [url-list (for [u urls] (hash-map :url u))]
    (stencil/render-file "templates/sitemap.txt" {:urls url-list})))
