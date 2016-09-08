;; http://ring-clojure.github.io/ring/
;; https://github.com/dakrone/cheshire
;; https://github.com/weavejester/compojure
;; http://clojuremongodb.info/
(ns mango.routes
  (:require [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as string]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.store :refer [SessionStore]]
            [ring.util.request :refer [request-url]]
            [ring.util.response :refer [file-response]]
            [compojure.core :refer [defroutes rfn GET PUT POST]]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [cheshire.generate :refer [add-encoder encode-str]]
            [markdown.core :as md]
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.db :as db]
            [mango.hydrate :as hydrate]
            [mango.pages :as pages])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:gen-class))

(add-encoder org.bson.types.ObjectId encode-str)

(defroutes routes
  (GET "/" {user :user session :session} (pages/index (json/write-str(auth/public-user user))))

  ;; JSON payload for a collection of articles
  (GET "/blog/articles.json" {user :user {:strs [page per-page]} :query-params}
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (hydrate/articles db/blog-articles page per-page))})

  ;; JSON payload for an article e.g. /blog/articles/1234.json
  (GET "/blog/articles/:id.json" {user :user {:keys [id]} :params}
       (let [article (db/blog-article id)]
         (if (not (nil? article))
           {
            :status 200
            :headers {"Content-Type" "application/json"}
            :body (generate-string (hydrate/media (hydrate/user (hydrate/content article))))}
           {
            :status 404
            :header {"Content-Type" "application/json"}
            :body (generate-string {:msg "Not found"})
            })))

  ;; Crawler specific route for an article e.g. /blog/1234
  (GET "/blog/:id" {user :user {:keys [id]} :params {:strs [user-agent]} :headers :as request}
       (let [article (db/blog-article id)]
         (when (not (nil? article))
           (let [hydrated-article (hydrate/media (hydrate/content article))
                 url (request-url request)]
             (cond
               (clojure.string/includes? user-agent "Twitterbot")
               {
                :status 200
                :headers {"Content-Type" "text/html"}
                :body (pages/article-for-twitter hydrated-article url)
                }
               (clojure.string/includes? user-agent "facebookexternalhit/1.1")
               {
                :status 200
                :headers {"Content-Type" "text/html"}
                :body (pages/article-for-facebook hydrated-article url)
                })))))

  ;; 
  (POST "/blog/articles/post.json" {user :user params :params}
        (if (auth/authorized user "editor")
          (let [article (keywordize-keys params)]
            {
             :status 200
             :headers {"Content-Type" "application/json"}
             :body (generate-string (db/insert-blog-article article (:_id user)))})
          {
           :status 403
           }))

  ;; 
  (POST "/blog/articles/:id.json" {user :user params :params}
        (if (auth/authorized user "editor")
          (let [article (keywordize-keys params)]
            {
             :status 200
             :headers {"Content-Type" "application/json"}
             :body (generate-string (db/update-blog-article article (:_id user)))})
          {
           :status 403
           }))

  ;; JSON payload for a collection of drafts
  (GET "/blog/drafts.json" {user :user {:strs [page per-page]} :query-params}
       (if (auth/authorized user "editor")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (generate-string (hydrate/articles db/blog-drafts page per-page))}
         {
          :status 403
          }))
       
  ;; JSON payload for a draft by id e.g. /blog/drafts/1234.json
  (GET "/blog/drafts/:id.json" [id]
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (list (db/blog-draft id)))})

  (GET "/blog/media.json" {{:strs [page per-page]} :query-params}
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (let [page (if page (Integer. page) 1)
                    per-page (if per-page (Integer. per-page) 10)]
                (generate-string (db/blog-media :page page :per-page per-page)))})

  (GET "/blog/media/:id.json" [id]
       {
        :status 200
        :headers {"Content-Type" "application/json"}
        :body (generate-string (list (db/blog-media-by-id id)))})

  ;; JSON payload of users
  (GET "/users.json" {{:strs [page per-page]} :query-params user :user}
       (if (auth/authorized user "editor")
         {
          :status 200
          :headers {"Content-Type" "application/json"}
          :body (let [page (if page (Integer. page) 1)
                      per-page (if per-page (Integer. per-page) 10)]
                  (generate-string (map auth/public-user (db/users :page page :per-page per-page))))}
         {
          :status 403
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

  (POST "/auth/signin" {session :session {:strs [username password]} :params}
        (if-let [user (auth/user username password)]
          (let [sess (assoc session :user (str (:_id user)))]
            {
             :status 200
             :session sess
             :body (generate-string sess)
             })
          {
           :status 401
           :header {"Content-Type" "application/json"}
           :body (generate-string {:msg "Invalid login"})
           }
          ))

  (POST "/auth/signout" {user :user session :session}
        (when user
          {
           :status 200
           :session nil
           }))

  (route/resources "/")

  ;; all other requests get redirected to index
  (rfn {user :user} (pages/index (json/write-str(auth/public-user user)))))

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

(defn log-request
  "Log request details"
  [request & [options]]
;  (println (request-url request))
  (println (str request))
;  (println (get-in request [:headers "user-agent"] ""))
  request)

(defn wrap-logger
  "Log stuff"
  [handler & [options]]
  (fn [request]
    (handler (log-request request options))))

(def application (-> routes
                     wrap-user
                     (wrap-session {:store (DBSessionStore.)})
                     wrap-cookies
                     wrap-params
                     wrap-logger))

