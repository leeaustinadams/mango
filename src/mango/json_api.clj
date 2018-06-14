;; https://github.com/dakrone/cheshire
(ns mango.json-api
  (:require
   [cheshire.core :refer :all]
   [mango.auth :as auth]
   [mango.dataprovider :as dp]
   [mango.hydrate :as hydrate]))

(defn json-status
  "A JSON response for a status. Generates JSON from the obj passed in"
  [status obj & rest]
  (reduce merge {
                 :status status
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn json-success
  "A JSON response for a success (200). Generates JSON from the obj passed in"
  [obj & [rest]]
  (json-status 200 obj rest))

(defn article-count
  "Route handler for total published article count"
  [data-provider]
  (json-success {:count (dp/blog-articles-count data-provider "published")}))

(defn draft-article-count
  "Route handler for total draft article count"
  [data-provider]
  (json-success {:count (dp/blog-articles-count data-provider "draft")}))

(defn published
  "Route handler for a page worth of articles"
  [data-provider options]
  (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "published" options))))

(defn drafts
  "Route handler for a page worth of drafts"
  [data-provider options]
  (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "draft" options))))

(defn- article-response
  "Renders a response for a hydrated article"
  [data-provider article]
  (if (not (empty? article))
    (json-success (hydrate/article data-provider article))
    (json-status 404 {:msg "Not found"})))

(defn article-by-id
  "Route handler for a single article by its id"
  [data-provider id]
  (let [status ["published" "draft"]
        article (dp/blog-article-by-id data-provider id {:status status})]
    (article-response data-provider article)))

(defn article-by-slug
  "Route handler for a single article by slug"
  [data-provider slug]
  (let [status ["published" "draft"]
        article (dp/blog-article-by-slug data-provider slug {:status status})]
    (article-response data-provider article)))

(defn list-media
  "Route handler to list media"
  [data-provider options]
  (json-success (dp/blog-media data-provider options)))

(defn media-by-id
  "Route handler for a single media description by id"
  [data-provider id]
  (json-success (list (dp/blog-media-by-id data-provider id))))

(defn list-users
  "Route handler for a list of all users"
  [data-provider options]
  (json-success (map auth/public-user (dp/users data-provider options))))

(defn me
  "Route handler for currently logged in user"
  [user]
  (json-success user))

(defn user-by-id
  "Route handler for a single user by id"
  [data-provider id]
  (json-success (list (dp/user-by-id data-provider id))))
