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
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
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
            [mango.dehydrate :as dehydrate]
            [mango.pages :as pages]
            [mango.storage :as storage]
            [clj-time.format :as time-format])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:import org.bson.types.ObjectId)
  (:gen-class))

(add-encoder org.bson.types.ObjectId encode-str)

(defn sanitize-article
  "Cleans and prepares an article from parameters posted"
  [params]
  (let [article (select-keys (keywordize-keys params) [:_id :title :description :content :created :tags :status])]
    (dehydrate/media (assoc article :created (time-format/parse (time-format/formatters :date-time) (:created article))))))

(defn accum-media
  "Inserts media item for each file in files and returns a sequence of the inserted media ids (or nil)"
  [files user-id]
  (loop [files files
         media '()]
    (let [file (first files)]
      (if (nil? file)
        media
        (let [m (db/insert-blog-media {:src (:filename file)} user-id)]
          (recur (rest files) (conj media (:_id m))))))))

(defn json-success
  "Response for a success (200). Generates JSON for the obj passed in"
  [obj & rest]
  (reduce merge {
                 :status 200
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn json-failure
  "Response for a failure. Generates JSON for the obj passed in"
  [code obj]
  {
   :status code
   :headers {"Content-Type" "application/json"}
   :body (generate-string obj)})

(defn html-success
  "Response for a success (200)."
  [body]
  {
   :status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn parse-file-keys
  "Returns a collection of keys for files"
  [params]
  (filter (fn [x]
            (and (string? x)
                 (not (nil? (re-matches #"files\[[0-9]+\]" x))))) (keys params)))

(defn parse-files
  "Returns a collection of files from params"
  [params]
  (let [file-keys (parse-file-keys params)]
    (vals (select-keys params file-keys))))

(defn parse-media-keys
  "Returns a collection of keys for media"
  [params]
  (filter (fn [x]
            (and (string? x)
                 (not (nil? (re-matches #"media\[[0-9]+\]\[_id\]" x))))) (keys params)))

(defn parse-media
  "Returns a collection of media ids from params"
  [params]
  (let [media-keys (parse-media-keys params)]
    (vals (select-keys params media-keys))))

(defn upload-files
  "Uploads files to storage"
  [files]
  (doseq [file files]
    (storage/upload (:filename file) (:tempfile file) (:content-type file))))

(defroutes routes
  (GET "/" {user :user session :session} (pages/index (json/write-str(auth/public-user user))))

  (GET "/sitemap.txt" {}
       (let [urls (mapv #(str (:_id %)) (db/blog-articles :page 1 :per-page 100))]
         {
          :status 200
          :headers {"Content-Type" "text/plain"}
          :body (pages/sitemap urls)
          }))

  ;; JSON payload for a collection of articles
  (GET "/blog/articles.json" {user :user {:strs [page per-page]} :query-params}
       (json-success (hydrate/articles db/blog-articles page per-page)))

  ;; JSON payload for an article e.g. /blog/articles/1234.json
  (GET "/blog/articles/:id.json" {user :user {:keys [id]} :params}
       (let [article (db/blog-article id)]
         (if (not (nil? article))
           (json-success (hydrate/media (hydrate/user (hydrate/content article))))
           (json-failure 404 {:msg "Not found"}))))

  (GET "/blog/drafts"  {user :user} (pages/index (json/write-str(auth/public-user user))))
  (GET "/blog/post"  {user :user} (pages/index (json/write-str(auth/public-user user))))

  ;; Posting a new article
  (POST "/blog/articles/post.json" {user :user params :params}
        (if (auth/editor? user)
          (let [files (parse-files params)
                media-ids (map #(ObjectId. %)(parse-media params))
                user-id (:_id user)
                article (sanitize-article params)
                new-media-ids (concat media-ids (accum-media files user-id))]
            (upload-files files)
            (json-success (db/insert-blog-article (assoc article :media new-media-ids) user-id)))
          (json-failure 403 nil)))

  ;; Updating an existing article
  (POST "/blog/articles/:id.json" {user :user params :params}
        (if (auth/editor? user)
          (let [files (parse-files params)
                media-ids (map #(ObjectId. %)(parse-media params))
                user-id (:_id user)
                article (sanitize-article params)
                new-media-ids (accum-media files user-id)]
            (upload-files files)
            (json-success (db/update-blog-article (assoc article :media (concat media-ids new-media-ids)) user-id)))
          (json-failure 403 nil)))

  ;; JSON payload for a collection of drafts
  (GET "/blog/drafts.json" {user :user {:strs [page per-page]} :query-params}
       (if (auth/editor? user)
         (json-success (hydrate/articles db/blog-drafts page per-page))
         (json-failure 403 nil)))

    ;; Crawler specific route for an article e.g. /blog/1234
  (GET "/blog/:id" {user :user {:keys [id]} :params {:strs [user-agent]} :headers :as request}
       (let [article (db/blog-article id)]
         (when (not (nil? article))
           (let [hydrated-article (hydrate/media (hydrate/content article))
                 url (request-url request)]
             (cond
               (clojure.string/includes? user-agent "Twitterbot")
               (html-success (pages/article-for-twitter hydrated-article url))
               (clojure.string/includes? user-agent "facebookexternalhit/1.1")
               (html-success (pages/article-for-facebook hydrated-article url)))))))

  ;; JSON payload for a draft by id e.g. /blog/drafts/1234.json
  (GET "/blog/drafts/:id.json" [id]
       (json-success (list (db/blog-draft id))))

  (GET "/blog/media.json" {{:strs [page per-page]} :query-params}
       (let [page (if page (Integer. page) 1)
             per-page (if per-page (Integer. per-page) 10)]
         (json-success (db/blog-media :page page :per-page per-page))))

  (GET "/blog/media/:id.json" [id]
       (json-success (list (db/blog-media-by-id id))))

  ;; JSON payload of users
  (GET "/users.json" {{:strs [page per-page]} :query-params user :user}
       (if (auth/editor? user)
         (let [page (if page (Integer. page) 1)
               per-page (if per-page (Integer. per-page) 10)]
           (json-success (map auth/public-user (db/users :page page :per-page per-page))))
         (json-failure 403 nil)))

  ;; JSON payload of current authenticated user
  (GET "/users/me.json" {user :user}
       (json-success user))

  ;; JSON payload of a user by id
  (GET "/users/:id.json" [id]
       (json-success (list (db/user id))))

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
            (json-success sess {:session sess}))
          (json-failure 401  {:msg "Invalid login"})))

  (POST "/auth/signout" {user :user session :session}
        {
         :status 200
         :session nil
         })

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
;  (println (str request))
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
                     wrap-multipart-params
                     wrap-logger))

