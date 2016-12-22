;; http://clojuremongodb.info/
(ns mango.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.query :as mq]
            [monger.joda-time]
            [mango.config :as config])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId))

(def conn (mg/connect))
(def DB (mg/get-db conn config/db-name))

(defn blog-articles
  "Query all blog articles that are published"
  [& {:keys [page per-page]}]
  (mq/with-collection DB config/db-article-collection
    (mq/find {:status "published"})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)
))
    
(defn blog-article
  "Query a single blog article by id"
  [id & {:keys [status]}]
  (mc/find-one-as-map DB config/db-article-collection {$and [{:_id (ObjectId. id)}, {:status {$in status}}]}))

(defn blog-article-by-slug
  "Query a single blog article by slug"
  [slug & {:keys [status]}]
  (mc/find-one-as-map DB config/db-article-collection {$and [{:slug slug}, {:status {$in status}}]}))

(defn blog-articles-by-tag
  "Query for articles tagged with tag"
  [tag & {:keys [page per-page]}]
  (println (str "tag: " tag " page: " page " per-page: " per-page))
  (mq/with-collection DB config/db-article-collection
    (mq/find {:status "published" :tags {$in [tag]}})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)))
  
(defn blog-drafts
  "Query all blog articles that are drafts"
  [& {:keys [page per-page]}]
  (mq/with-collection DB config/db-article-collection
    (mq/find {:status "draft"})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)))

(defn blog-draft
  "Query a single draft blog article by id"
  [id]
  (mc/find-one-as-map DB config/db-article-collection {$and [ {:_id (ObjectId. id)} {:status "draft"} ] }))

(defn insert-blog-article
  "Adds a single blog article"
  [article user-id]
  (mc/insert-and-return DB config/db-article-collection (merge article {:user user-id})))

(defn update-blog-article
  "Updates a single blog article"
  [article user-id]
  (let [article (->
                 article
                 (conj {:user user-id})
                 (conj {:_id (ObjectId. (:_id article))}))]
    (mc/save-and-return DB config/db-article-collection article)))

(defn blog-media [& {:keys [page per-page]}]
  (mq/with-collection DB config/db-media-collection
    (mq/find {:status ["published"]})
    (mq/paginate :page page :per-page per-page)))

(defn insert-blog-media
  "Adds a media item"
  [media user-id]
  (mc/insert-and-return DB config/db-media-collection (assoc media :user user-id)))

(defn blog-media-by-id [id]
  (mc/find-map-by-id DB config/db-media-collection (ObjectId. id)))

(defn blog-media-by-ids [ids]
  (mq/with-collection DB config/db-media-collection
    (mq/find {:_id {$in ids}})))

(defn users [& {:keys [page per-page]}]
  (mq/with-collection DB config/db-users-collection
    (mq/find {})
    (mq/paginate :page page :per-page per-page)))

(defn user [id]
  (mc/find-map-by-id DB config/db-users-collection (ObjectId. id)))

(defn user-by-id [id]
  (mc/find-map-by-id DB config/db-users-collection id))

(defn user-by-username [username]
  (mc/find-one-as-map DB config/db-users-collection {:username username}))

(defn insert-user
  "Add a new user record"
  [username first-name last-name display-name email password roles]
  (let [user {:username username :first-name first-name :last-name last-name :display-name display-name :email email :password password :roles roles}]
    (mc/insert-and-return DB config/db-users-collection user)))

(defn update-user
  "Updates a user record"
  [user]
  (mc/update-by-id DB config/db-users-collection (:_id user) user))

(defn delete-user [user])

(defn delete-session
  "Delete a session from the database"
  [id]
  (mc/remove-by-id DB config/db-sessions-collection (ObjectId. id)))

(defn read-session
  "Read a session from the database"
  [id]
  (when id
    (mc/find-one-as-map DB config/db-sessions-collection {:_id (ObjectId. id)})))

(defn add-session
  "Add a session to the database and return its id"
  [data]
  (let [id (:_id (mc/insert-and-return DB config/db-sessions-collection data))]
    id))

(defn write-session
  "Write a session to the database and return its id"
  [id data]
  (mc/update-by-id DB config/db-sessions-collection id data)
  id)
