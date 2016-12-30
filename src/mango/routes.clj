;; http://ring-clojure.github.io/ring/
;; https://github.com/dakrone/cheshire
;; https://github.com/weavejester/compojure
(ns mango.routes
  (:require [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]
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
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.db :as db]
            [mango.hydrate :as hydrate]
            [mango.pages :as pages]
            [mango.storage :as storage]
            [mango.util :refer [slugify xform-ids xform-tags xform-time]])
  (:gen-class))

(defn sanitize-article
  "Cleans and prepares an article from parameters posted"
  [params]
  (let [article (-> params
                    keywordize-keys
                    (select-keys [:_id :title :description :content :created :media :tags :status]))
        media (xform-ids (:media article))
        created (xform-time (:created article))
        tags (xform-tags (:tags article))]
    (merge article
           (when (not (nil? media)) {:media media})
           (when (not (nil? created)) {:created created})
           {:slug (slugify (:title article) :limit 5)}
           {:tags tags})))

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
  "A JSON response for a success (200). Generates JSON from the obj passed in"
  [obj & rest]
  (reduce merge {
                 :status 200
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn json-failure
  "A JSON response for a failure. Generates JSON from the obj passed in"
  [code obj]
  {
   :status code
   :headers {"Content-Type" "application/json"}
   :body (generate-string obj)})

(defn html-success
  "An HTML response for a success (200)."
  [body]
  {
   :status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn html-failure
  "An HTML response for a failure."
  [code body]
  {
   :status code
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

(defn article-response
  "Renders a response for a hydrated article"
  [article]
  (if (not (empty? article))
    (json-success (hydrate/media (hydrate/user (hydrate/content article))))
    (json-failure 404 {:msg "Not found"})))

(defn crawler-article-response
  "If the user-agent is a crawler, renders an appropriate response for a hydrated article"
  [article user-agent url]
  (let [hydrated-article (hydrate/media (hydrate/content article))]
    (cond
      (str/includes? user-agent "Twitterbot") (html-success (pages/article-for-bots hydrated-article url))
      (str/includes? user-agent "facebookexternalhit/1.1") (html-success (pages/article-for-bots hydrated-article url))
      (str/includes? user-agent "Googlebot") (html-success (pages/article-for-bots hydrated-article url)))))

(defn html-index
  [user]
  (pages/index (json/write-str (auth/public-user user))))

(defroutes routes
  (GET "/" {user :user} (html-index user))
  (GET "/blog/drafts" {user :user} (html-index user))
  (GET "/blog/post" {user :user} (html-index user))

  (GET "/sitemap.txt" {}
       (let [urls (mapv #(str (or (:slug %) (:_id %))) (db/blog-articles :page 1 :per-page 100))]
         {
          :status 200
          :headers {"Content-Type" "text/plain"}
          :body (pages/sitemap urls)
          }))

  ;; JSON payload for a collection of articles
  (GET "/blog/articles.json" {user :user {:strs [page per-page]} :query-params}
       (json-success (hydrate/articles db/blog-articles page per-page)))

  ;; JSON payload for a collection of articles with specified tag
  (GET "/blog/tagged/:tag.json" {user :user {:keys [tag]} :params {:strs [ page per-page]} :query-params}
       (json-success (hydrate/articles (partial db/blog-articles-by-tag tag) page per-page)))

  ;; JSON payload for an article e.g. /blog/articles/1234.json
  (GET "/blog/articles/:id{[0-9a-f]+}.json" {user :user {:keys [id]} :params}
       (let [status ["published" (when (auth/editor? user) "draft")]
             article (db/blog-article id :status status)]
         (article-response article)))

  ;; e.g. /blog/articles/unce-upon-a-time.json
  (GET "/blog/articles/:slug{[0-9a-z-]+}.json" {user :user {:keys [slug]} :params}
       (let [status ["published" (when (auth/editor? user) "draft")]
             article (db/blog-article-by-slug slug :status status)]
         (article-response article)))

  ;; Posting a new article
  (POST "/blog/articles/post.json" {user :user params :params}
        (if (auth/editor? user)
          (let [user-id (:_id user)
                article (sanitize-article params)]
            (json-success (db/insert-blog-article article user-id)))
          (json-failure 403 nil)))

  ;; Updating an existing article
  (POST "/blog/articles/:id.json" {user :user params :params}
        (if (auth/editor? user)
          (let [user-id (:_id user)
                article (sanitize-article params)]
            (json-success (db/update-blog-article article user-id)))
          (json-failure 403 nil)))

  (POST "/blog/media" {user :user params :params}
        (if (auth/editor? user)
          (let [files (parse-files params)
                user-id (:_id user)
                media-ids (accum-media files user-id)]
            (println "files" files)
            (println "media-ids" media-ids)
            (upload-files files)
            (json-success media-ids))))

  ;; JSON payload for a collection of drafts
  (GET "/blog/drafts.json" {user :user {:strs [page per-page]} :query-params}
       (if (auth/editor? user)
         (json-success (hydrate/articles db/blog-drafts page per-page))
         (json-failure 403 nil)))

  ;; Crawler specific route for an article e.g. /blog/1234
  (GET "/blog/:id{[0-9a-f]+}" {user :user {:keys [id]} :params {:strs [user-agent]} :headers :as request}
       (when-let [article (db/blog-article id :status ["published"])]
         (crawler-article-response article user-agent (request-url request))))

  (GET "/blog/:slug{[0-9a-z-]+}" {user :user {:keys [slug]} :params {:strs [user-agent]} :headers :as request}
       (println slug)
       (when-let [article (db/blog-article-by-slug slug :status ["published"])]
         (crawler-article-response article user-agent (request-url request))))

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
  (GET "/users/:id.json" {user :user {:keys [id]} :params}
       (when (auth/admin? user)
         (json-success (list (db/user id)))))

  ;; (GET "/admin/users/:id.json" [id]
  ;;      {})

  ;; (POST "/users/password" [request]
  ;;       {})

  ;; (POST "/users/forgot" []
  ;;       {})

  ;; (GET "/users/reset/:token" [token]
  ;;       {})

  ;; (POST "/users/reset/:token" [token]
  ;;       {})

  ;; (POST "/auth/signup" []
  ;;       {})

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

  (POST "/log/event" {user :user {:strs [errorUrl category event]} :params}
        (json-success (db/insert-log-event {:user user :error-url errorUrl :category category :event event})))

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

;; Example request
;; {:ssl-client-cert nil,
;;  :protocol "HTTP/1.1",
;;  :remote-addr "0:0:0:0:0:0:0:1",
;;  :headers {"cookie" "__utma=111872281.651807314.1472234250.1472241506.1472252459.3; __utmz=111872281.1472234250.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); ring-session=585b6bdaada47034e19d68df; _ga=GA1.1.651807314.1472234250; _gat=1",
;;            "cache-control" "max-age=0",
;;            "accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
;;            "upgrade-insecure-requests" "1",
;;            "connection" "keep-alive",
;;            "user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36",
;;            "host" "localhost:8080",
;;            "accept-encoding" "gzip, deflate, sdch, br",
;;            "accept-language" "en-US,en;q=0.8"},
;;  :server-port 8080,
;;  :content-length nil,
;;  :content-type nil,
;;  :character-encoding nil,
;;  :uri "/blog/articles.json",
;;  :server-name "localhost",
;;  :query-string "page=2&per-page=9",
;;  :body #object[org.eclipse.jetty.server.HttpInputOverHTTP 0x508bb9d6 "HttpInputOverHTTP@508bb9d6"],
;;  :scheme :http, :request-method :get}
(defn log-request
  "Log request details"
  [request & [options]]
  (println (str (select-keys request [:uri :request-method :query-string]) "\n"))
  request)

(defn wrap-logger
  "Log stuff"
  [handler & [options]]
  (fn [request]
    (handler (log-request request options))))

(def application (-> routes
                     wrap-user
;                     (wrap-session {:store (DBSessionStore.)})
                     wrap-session
                     wrap-cookies
                     wrap-params
                     wrap-multipart-params
                     wrap-logger))

