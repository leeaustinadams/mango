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
(def users-collection "users")
(def blog-collection "blogarticles")
(def blog-media-collection "blogmedia")
;(def blog-collection "familyarticles")
;(def blog-media-collection "familymedia")
(def sessions-collection "sessions")

(defn blog-articles
  "Query all blog articles that are published"
  [& {:keys [page per-page tags]}]
  (mq/with-collection DB blog-collection
    (mq/find {:status "published"})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)))
    
(defn blog-article
  "Query a single blog article by id"
  [id]
  (mc/find-map-by-id DB blog-collection (ObjectId. id)))

(defn blog-drafts
  "Query all blog articles that are drafts"
  [& {:keys [page per-page tags]}]
  (mq/with-collection DB blog-collection
    (mq/find {:status "draft"})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)))

(defn blog-draft
  "Query a single blog article by id"
  [id]
  (mc/find-map-by-id DB blog-collection (ObjectId. id)))

(defn insert-blog-article
  "Adds a single blog article"
  [article user-id]
  (mc/insert-and-return DB blog-collection (conj article {:user user-id})))

(defn blog-media [& {:keys [page per-page]}]
  (mq/with-collection DB blog-media-collection
    (mq/find {:status ["published"]})
    (mq/paginate :page page :per-page per-page)))

(defn blog-media-by-id [id]
  (mc/find-map-by-id DB blog-media-collection (ObjectId. id)))

(defn blog-media-by-ids [ids]
  (mq/with-collection DB blog-media-collection
    (mq/find {:_id {$in ids}})))

(defn users [& {:keys [page per-page]}]
  (mq/with-collection DB users-collection
    (mq/find {})
    (mq/paginate :page page :per-page per-page)))

(defn user [id]
  (mc/find-map-by-id DB users-collection (ObjectId. id)))

(defn user-by-id [id]
  (mc/find-map-by-id DB users-collection id))

(defn user-by-username [username]
  (mc/find-one-as-map DB users-collection {:username username}))

(defn insert-user
  "Add a new user record"
  [username first-name last-name display-name email password roles]
  (let [user {:username username :first-name first-name :last-name last-name :display-name display-name :email email :password password :roles roles}]
    (mc/insert-and-return DB users-collection user)))

(defn update-user
  "Updates a user record"
  [user]
  (mc/update-by-id DB users-collection (:_id user) user))

(defn delete-user [user])

(defn delete-session
  "Delete a session from the database"
  [id]
  (mc/remove-by-id DB sessions-collection (ObjectId. id)))

(defn read-session
  "Read a session from the database"
  [id]
  (when id
    (let [data (mc/find-one-as-map DB sessions-collection {:_id (ObjectId. id)})]
      (println data)
      data)))

(defn add-session
  "Add a session to the database and return its id"
  [data]
  (let [id (:_id (mc/insert-and-return DB sessions-collection data))]
    id))

(defn write-session
  "Write a session to the database and return its id"
  [id data]
  (println "write-session" id data)
  (mc/update-by-id DB sessions-collection id data)
  id)
