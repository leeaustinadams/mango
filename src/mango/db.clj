;; http://clojuremongodb.info
(ns mango.db
  (:require [monger.core :as mg]
            [monger.credentials :as mcred]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.query :as mq]
            [monger.joda-time]
            [mango.config :as config]
            [mango.dataprovider :as dp]
            [taoensso.timbre :refer [tracef debugf info]])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId))

(def conn (atom ()))
(def DB (atom ()))
(def default-per-page 100)

(defn init
  "Initialize the database connection"
  []
  (reset! conn (mg/connect-with-credentials config/db-address (mcred/create config/db-user config/db-name config/db-password)))
  (reset! DB (mg/get-db @conn config/db-name)))

(defn terminate
  "Terminate the database"
  []
  (mg/disconnect @conn)
  (reset! DB nil)
  (reset! conn nil))

(defmulti user-by-id class)
(defmethod user-by-id String [id] (user-by-id (ObjectId. id)))
(defmethod user-by-id ObjectId [id] (mc/find-one-as-map @DB config/db-users-collection {:_id id}))

(defn user-by-username
  "Lookup a user by username"
  [username]
  (mc/find-one-as-map @DB config/db-users-collection {:username username}))

(defn blog-articles-count
  "Query the number of articles"
  [{:keys [status author]}]
  (let [query (merge {:status {$in status}}
                     (when author {:user (:_id (user-by-username author))}))]
    (mc/count @DB config/db-article-collection query)))

(defn blog-articles-by-query
  "Query blog articles"
  [query {:keys [page per-page] :or {page 1 per-page default-per-page}}]
  (mq/with-collection @DB config/db-article-collection
    (mq/find query)
    (mq/sort {:created -1})
    (mq/paginate :page (Integer. page) :per-page (Integer. per-page))))

(defn blog-articles
  "Query blog articles"
  [{:keys [status tagged author] :as params}]
  (let [query (merge {:status {$in status}}
                     (when author {:user (:_id (user-by-username author))})
                     (when (not (empty? tagged)) {:tags {$in tagged}}))]
    (blog-articles-by-query query params)))

(defn blog-article-by-id
  "Query a single blog article by id"
  [id {:keys [status]}]
  (mc/find-one-as-map @DB config/db-article-collection {$and [{:_id (ObjectId. id)}, {:status {$in status}}]}))

(defn blog-article-by-slug
  "Query a single blog article by slug"
  [slug {:keys [status]}]
  (mc/find-one-as-map @DB config/db-article-collection {$and [{:slug slug}, {:status {$in status}}]}))

(defn insert-blog-article
  "Adds a single blog article"
  [article user-id]
  (mc/insert-and-return @DB config/db-article-collection (merge article {:user user-id})))

(defn update-blog-article
  "Updates a single blog article"
  [article user-id]
  (let [article-id {:_id (ObjectId. (:_id article))}
        article (-> article
                    (conj {:user user-id})
                    (conj article-id))]
    (mc/update @DB config/db-article-collection article-id {$set article})))

(defn update-blog-article-media
  [article-id media-id]
  (mc/update @DB config/db-article-collection {:_id (ObjectId. article-id)} {$addToSet {:media media-id}}))

(defn blog-media
  "Lookup a page worth of media items"
  [{:keys [page per-page] :or {page 1 per-page default-per-page}}]
  (mq/with-collection @DB config/db-media-collection
    (mq/paginate :page (Integer. page) :per-page (Integer. per-page))))

(defn insert-blog-media
  "Adds a media item"
  [media user-id]
  (mc/insert-and-return @DB config/db-media-collection (assoc media :user user-id)))

(defn update-blog-media
  "Updates a media"
  [media]
  (mc/update-by-id @DB config/db-media-collection (:_id media) media))

(defmulti delete-blog-media-by-id class)
(defmethod delete-blog-media-by-id String [media-id] (delete-blog-media-by-id (ObjectId. media-id)))
(defmethod delete-blog-media-by-id ObjectId [media-id] (mc/remove-by-id @DB config/db-media-collection media-id))

(defn delete-blog-media
  "Deletes a media"
  [media]
  (delete-blog-media-by-id (:_id media)))

(defmulti blog-media-by-id class)
(defmethod blog-media-by-id String [media-id] (blog-media-by-id (ObjectId. media-id)))
(defmethod blog-media-by-id ObjectId [media-id] (mc/find-map-by-id @DB config/db-media-collection media-id))

(defn blog-media-by-ids
  "Lookup a collection of media by their ids"
  [ids]
  (when (not (empty? ids))
    (mq/with-collection @DB config/db-media-collection
      (mq/find {:_id {$in ids}}))))

(defn users
  "Lookup a page worth of users"
  [{:keys [page per-page] :or {page 1 per-page default-per-page}}]
  (mq/with-collection @DB config/db-users-collection
    (mq/find {})
    (mq/paginate :page (Integer. page) :per-page (Integer. per-page))))

(defn insert-user
  "Add a new user record"
  [user]
  (debugf "Inserting user: %s" (str user))
  (mc/insert-and-return @DB config/db-users-collection user))

(defn update-user
  "Updates a user record"
  [user]
  (let [user-id {:_id (ObjectId. (:_id user))}
        edit-user (conj user user-id)]
    (debugf "Updating user: %s" (str edit-user))
    (mc/update @DB config/db-users-collection user-id {$set edit-user})))

(defn delete-user [user])

(defn blog-article-tags
  [{:keys [status]}]
  (let [query (merge {:tags {$ne nil}} {:status {$in status}})]
    (flatten (map :tags (mc/find-maps @DB config/db-article-collection query {:tags 1})))))

(defn pages-by-query
  "Query pages"
  [query {:keys [page per-page] :or {page 1 per-page default-per-page}}]
  (mq/with-collection @DB config/db-page-collection
    (mq/find query)
    (mq/sort {:title -1})
    (mq/paginate :page (Integer. page) :per-page (Integer. per-page))))

(defn pages
  "Query pages"
  [{:keys [status] :as params}]
  (pages-by-query {:status {$in status}} params))

(defn page-by-slug
  "Query a single page by slug"
  [slug {:keys [status]}]
  (mc/find-one-as-map @DB config/db-page-collection {$and [{:slug slug}, {:status {$in status}}]}))

(defn insert-page
  "Adds a single page"
  [page user-id]
  (mc/insert-and-return @DB config/db-page-collection (merge page {:user user-id})))

(defn update-page
  "Updates a single page"
  [page user-id]
  (let [page-id {:_id (ObjectId. (:_id page))}
        page (-> page
                 (conj {:user user-id})
                 (conj page-id))]
    (mc/update @DB config/db-page-collection page-id {$set page})))

(defn update-page-media
  [page-id media-id]
  (mc/update @DB config/db-page-collection {:_id (ObjectId. page-id)} {$addToSet {:media media-id}}))

(deftype DBDataProvider []
  dp/DataProvider
  (media-by-ids [this ids] (blog-media-by-ids ids))
  (blog-media [this options] (blog-media options))
  (blog-media-by-id [this media-id] (blog-media-by-id media-id))
  (insert-blog-media [this media user-id] (insert-blog-media media user-id))
  (delete-blog-media-by-id [this media-id] (delete-blog-media-by-id media-id))
  (users [this options] (users options))
  (insert-user [this user] (insert-user user))
  (update-user [this user] (update-user user))
  (user-by-id [this id] (user-by-id id))
  (user-by-username [this username] (user-by-username username))
  (blog-articles [this options] (blog-articles options))
  (blog-articles-count [this options] (blog-articles-count options))
  (blog-article-by-id [this id options] (blog-article-by-id id options))
  (blog-article-by-slug [this slug options] (blog-article-by-slug slug options))
  (insert-blog-article [this article user-id] (insert-blog-article article user-id))
  (update-blog-article [this article user-id] (update-blog-article article user-id))
  (update-blog-article-media [this article-id media-id] (update-blog-article-media article-id media-id))
  (blog-article-tags [this options] (blog-article-tags options))
  (pages [this options] (pages options))
  (page-by-slug [this slug options] (page-by-slug slug options))
  (insert-page [this page user-id] (insert-page page user-id))
  (update-page [this page user-id] (update-page page user-id))
  (update-page-media [this page-id media-id] (update-page-media page-id media-id)))

(def data-provider (DBDataProvider.))
