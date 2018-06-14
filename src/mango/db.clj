;; http://clojuremongodb.info
(ns mango.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.query :as mq]
            [monger.joda-time]
            [mango.config :as config]
            [mango.dataprovider :as dp])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId))

(def conn (atom ()))
(def DB (atom ()))
(def default-per-page 100)

(defn init
  "Initialize the database connection"
  []
  (reset! conn (mg/connect))
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
  [status {:keys [author]}]
  (let [query (merge {:status status}
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
  [status {:keys [tagged author] :as params}]
  (let [query (merge {:status status}
                     (when author {:user (:_id (user-by-username author))})
                     (when tagged {:tags {$in [tagged]}}))]
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
  (let [article (->
                 article
                 (conj {:user user-id})
                 (conj {:_id (ObjectId. (:_id article))}))]
    (mc/save-and-return @DB config/db-article-collection article)))

(defn blog-media
  "Lookup a page worth of media items"
  [{:keys [page per-page] :or {page 1 per-page default-per-page}}]
  (mq/with-collection @DB config/db-media-collection
    (mq/paginate :page (Integer. page) :per-page (Integer. per-page))))

(defn insert-blog-media
  "Adds a media item"
  [media user-id]
  (mc/insert-and-return @DB config/db-media-collection (assoc media :user user-id)))

(defn blog-media-by-id
  "Lookup a media item by its id"
  [id]
  (mc/find-map-by-id @DB config/db-media-collection (ObjectId. id)))

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
  [username first-name last-name display-name email twitter-handle password roles]
  (let [user {:username username
              :first-name first-name
              :last-name last-name
              :display-name display-name
              :email email
              :twitter-handle twitter-handle
              :password password
              :roles roles}]
    (mc/insert-and-return @DB config/db-users-collection user)))

(defn update-user
  "Updates a user record"
  [user]
  (mc/update-by-id @DB config/db-users-collection (:_id user) user))

(defn delete-user [user])

(defn delete-session
  "Delete a session from the database"
  [id]
  (mc/remove-by-id @DB config/db-sessions-collection (ObjectId. id)))

(defn read-session
  "Read a session from the database"
  [id]
  (when id
    (mc/find-one-as-map @DB config/db-sessions-collection {:_id (ObjectId. id)})))

(defn add-session
  "Add a session to the database and return its id"
  [data]
  (let [id (:_id (mc/insert-and-return @DB config/db-sessions-collection data))]
    id))

(defn write-session
  "Write a session to the database and return its id"
  [id data]
  (mc/update-by-id @DB config/db-sessions-collection id data)
  id)

(defn insert-log-event
  "Adds a single log event"
  [event]
  (mc/insert-and-return @DB config/db-log-collection event))

(deftype DBDataProvider []
  dp/DataProvider
  (media-by-ids [this ids] (blog-media-by-ids ids))
  (blog-media [this options] (blog-media options))
  (blog-media-by-id [this id] (blog-media-by-id id))
  (users [this options] (users options))
  (user-by-id [this id] (user-by-id id))
  (insert-blog-media [this media user-id] (insert-blog-media media user-id))
  (blog-articles [this status options] (blog-articles status options))
  (blog-articles-count [this status] (blog-articles-count status))
  (blog-article-by-id [this id options] (blog-article-by-id id options))
  (blog-article-by-slug [this slug options] (blog-article-by-slug slug options))
  (insert-blog-article [this article user-id] (insert-blog-article article user-id))
  (update-blog-article [this article user-id] (update-blog-article article user-id))
  (insert-log-event [this event] (insert-log-event event)))

(def data-provider (DBDataProvider.))
