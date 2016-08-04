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
(def family-collection "familyarticles")
(def family-media-collection "familymedia")
(def sessions-collection "sessions")

(defn family-articles [& {:keys [page per-page tags]}]
  (mq/with-collection DB family-collection
    (mq/find {:status ["published"]})
    (mq/sort {:created -1})
    (mq/paginate :page page :per-page per-page)))
    
    ;; (let [query {:status ["published"]} ]
    ;;   (let [query (if tags (assoc query :tags {$in tags}) query)]
    ;;     (println query)
    ;;     (mq/find query)
    ;;     (mq/sort {:created -1})
    ;;     (mq/paginate :page page :per-page per-page)))))

(defn family-article [id]
  "Query a single family article by id"
  (mc/find-map-by-id DB family-collection (ObjectId. id)))

(defn family-drafts []
  "Query all family articles that are drafts"
  (mc/find-maps DB family-collection {:status ["draft"]}))

(defn family-draft [id]
  "Query a single family article by id"
  (mc/find-map-by-id DB family-collection (ObjectId. id)))

(defn family-media [& {:keys [page per-page]}]
  (mq/with-collection DB family-media-collection
    (mq/find {:status ["published"]})
    (mq/paginate :page page :per-page per-page)))

(defn family-media-item [id]
  (mc/find-map-by-id DB family-media-collection (ObjectId. id)))

(defn family-media-items [ids]
  (mq/with-collection DB family-media-collection
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

(defn user-write [user]
  (mc/update-by-id DB users-collection (:_id user) user))

(defn delete-session
  "Delete a session from the database"
  [id]
  (println "delete-session" id)
  (mc/remove-by-id DB sessions-collection (ObjectId. id)))

(defn read-session
  "Read a session from the database"
  [id]
  (println "read-session" id)
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
