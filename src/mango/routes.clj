;; https://github.com/ring-clojure
;; https://github.com/dakrone/cheshire
;; https://github.com/weavejester/compojure
;; http://clojuremongodb.info/
(ns mango.routes
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.store :refer [SessionStore]]
            [compojure.core :refer [defroutes rfn GET PUT POST]]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [cheshire.generate :refer [add-encoder encode-str]]
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.db :as db]
            [mango.pages :as pages])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:gen-class))

(add-encoder org.bson.types.ObjectId encode-str)

(defn hydrate-media
  "Hydrates the media collection of x"
  [x]
  (assoc x :media (db/family-media-items (:media x))))

(defn hydrate-user 
  "Hydrates the user field of x"
  [x]
  (when-let [user-id (:user x)]
    (println "hydrate-user" user-id)
    (assoc x :user (auth/public-user (db/user-by-id user-id)))))

(defn hydrated-articles
  "Hydrates a page worth of articles"
  [page per-page]
  (let [page (if page (Integer. page) 1)
        per-page (if per-page (Integer. per-page) 10)
        articles (db/family-articles :page page :per-page per-page)]
        (map hydrate-media (map hydrate-user articles))))
    
(defroutes routes
  (GET "/" [] (pages/index))

  ;; JSON payload for a collection of articles
  (GET "/family/articles.json" {user :user {:strs [page per-page]} :query-params}
       (if (auth/authorized user "family")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (generate-string (hydrated-articles page per-page))}
         {
          :status 403
          :body (pages/not-allowed)
          }))

  ;; JSON payload for an article e.g. /family/articles/1234.json
  (GET "/family/articles/:id.json" {user :user {:keys [id]} :params}
       (if (auth/authorized user "family")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (generate-string (hydrate-media (hydrate-user (db/family-article id))))}
         {
          :status 403
          :body (pages/not-allowed)
          }))

  ;; JSON payload for a collection of drafts
  (GET "/family/drafts.json" {user :user}
       (if (auth/authorized user "familyeditor")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (generate-string (db/family-drafts))}
         {
          :status 403
          :body (pages/not-allowed)
          }))
       
  ;; JSON payload for a draft by id e.g. /family/drafts/1234.json
  (GET "/family/drafts/:id.json" [id]
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (list (db/family-draft id)))})

  (GET "/family/media.json" {{:strs [page per-page]} :query-params}
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (let [page (if page (Integer. page) 1)
                    per-page (if per-page (Integer. per-page) 10)]
                (generate-string (db/family-media :page page :per-page per-page)))})

  (GET "/family/media/:id.json" [id]
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (list (db/family-media-item id)))})

  ;; JSON payload of users
  (GET "/users.json" {{:strs [page per-page]} :query-params user :user}
       (if (auth/authorized user "familyeditor")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (let [page (if page (Integer. page) 1)
                      per-page (if per-page (Integer. per-page) 10)]
                  (generate-string (map auth/public-user (db/users :page page :per-page per-page))))}
         {
          :status 403
          :body (pages/not-allowed)
          }))

  ;; JSON payload of current authenticated user
  (GET "/users/me.json" {user :user}
        {
         :status 200
         :headers {"Content-Type" "application/json"}
         :body (generate-string user)
         })

  ;; JSON payload of a user by id
  (GET "/users/:id.json" [id]
       { 
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (list (db/user id)))})

  (GET "/admin/users/:id.json" [id]
       {})

  (POST "/users/password" [request]
        {})

  (POST "/users/forgot" []
        {})

  (GET "/users/reset/:token" [token]
        {})

  (POST "/users/reset/:token" [token]
        {})

  (POST "/auth/signup" []
        {})

  (GET "/auth/signin" []
       {
        :status 200
        :body (pages/sign-in)
        })

  (POST "/auth/signin" {session :session params :form-params}
        (let [username (params "username")
              password (params "password")
              user (auth/user username password)]
          (if user
            (let [sess (assoc session :user (str (:_id user)))]
              {
               :status 200
               :session sess
               :body (generate-string sess)
               })
            {
             :status 401
             :body (pages/sign-in)
             }
            )))

  ;; page for signing out the current user
  (GET "/auth/signout" []
       {
        :status 200
        :body (pages/sign-out)
        })

  (POST "/auth/signout" {user :user session :session}
        (when user
          {
           :status 200
           :session nil;(dissoc session :user)
           :body (pages/signed-out (:username user))
           }))
  
  (route/resources "/")

  ;; debug page
  (GET "/auth/test" []
       ;; for debugging purposes, make sure we have a couple test users
       (db/user-write (auth/update-user-password (db/user-by-username "atester") "zzxxccvv"))
       (db/user-write (auth/update-user-password (db/user-by-username "ftester") "zzxxccvv"))
       {
        :status 200
        :body (pages/sign-in)
        })

  ;; all other requests get a not found error
  (rfn request (pages/not-found)))

(defrecord DBSessionStore []
  SessionStore
  (delete-session [this key] (db/delete-session key) nil)
  (read-session [this key] (db/read-session key))
  (write-session [this key data]
    (if (nil? key)
      (db/add-session data)
      (db/write-session key data))))

(defn wrap-user
  "Add a user to the request object if there is a user id in the session"
  [handler & [options]]
  (fn [request]
    (if-let [user-id (-> request :session :user)]
      (handler (assoc request :user (auth/private-user (db/user user-id))))
      (handler request))))

(def application (-> routes
                     wrap-user
                     (wrap-session {:store (DBSessionStore.)})
                     wrap-cookies
                     wrap-params))

