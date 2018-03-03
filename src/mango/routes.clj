;; http://ring-clojure.github.io/ring/
;; https://github.com/dakrone/cheshire
;; https://github.com/weavejester/compojure
(ns mango.routes
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
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
            [mango.dataprovider :as dp]
            [mango.hydrate :as hydrate]
            [mango.pages :as pages]
            [mango.storage :as storage]
            [mango.util :refer [slugify xform-ids xform-tags xform-time]]))

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
           (when media {:media media})
           (when created {:created created})
           {:slug (slugify (:title article) :limit 5)}
           {:tags tags})))

(defn accum-media
  "Inserts media item for each file in files and returns a sequence of the inserted media ids (or nil)"
  [data-provider files user-id]
  (loop [files files
         media '()]
    (let [file (first files)]
      (if (nil? file)
        media
        (let [m (dp/insert-blog-media data-provider {:src (:filename file)} user-id)]
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
  [code obj & rest]
  (reduce merge {
                 :status code
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn html-response
  "An HTML response with code."
  [code body]
  {
   :status code
   :headers {"Content-Type" "text/html"}
   :body body})

(defn html-success
  "An HTML response for a success (200)."
  [body]
  (html-response 200 body))

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

(defn upload-file
  "Uploads files to storage. Returns a future"
  [file]
  (storage/upload config/aws-media-bucket (str "blog/" (:filename file)) (:tempfile file) (:content-type file)))

(defn article-response
  "Renders a response for a hydrated article"
  [data-provider article]
  (if (not (empty? article))
    (json-success (hydrate/article data-provider article))
    (json-failure 404 {:msg "Not found"})))

(defn sitemap
  "Route handler for the sitemap.txt response"
  [data-provider]
  (let [urls (mapv #(str (or (:slug %) (:_id %))) (dp/blog-articles data-provider "published" {:page 1 :per-page 100}))]
    {
     :status 200
     :headers {"Content-Type" "text/plain"}
     :body (pages/sitemap urls)
     }))

(defn article-count
  "Route handler for total published article count"
  [data-provider ]
  (json-success {:count (dp/blog-articles-count data-provider "published")}))

(defn draft-article-count
  "Route handler for total draft article count"
  [data-provider user]
  (if (auth/editor? user)
    (json-success {:count (dp/blog-articles-count data-provider "draft")})
    (json-failure 403 {:message "Forbidden"})))

(defn published
  "Route handler for a page worth of articles"
  [data-provider user page per-page tagged]
  (let [page (if page (Integer. page) 1)
        per-page (if per-page (Integer. per-page) 10)]
    (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "published" {:page page :per-page per-page :tagged tagged})))))

(defn drafts
  "Route handler for a page worth of drafts"
  [data-provider user page per-page tagged]
  (if (auth/editor? user)
    (let [page (if page (Integer. page) 1)
          per-page (if per-page (Integer. per-page) 10)]
      (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "draft" {:page page :per-page per-page :tagged tagged}))))
    (json-failure 403 {:message "Forbidden"})))

(defn article-by-id
  "Route handler for a single article by its id"
  [data-provider user id]
  (let [status ["published" (when (auth/editor? user) "draft")]
        article (dp/blog-article-by-id data-provider id {:status status})]
    (article-response data-provider  article)))

(defn article-by-slug
  "Route handler for a single article by slug"
  [data-provider user slug]
  (let [status ["published" (when (auth/editor? user) "draft")]
        article (dp/blog-article-by-slug data-provider slug {:status status})]
    (article-response data-provider article)))

(defn post-article
  "Route handler for posting an article"
  [data-provider user params]
  (if (auth/editor? user)
    (let [user-id (:_id user)
          article (sanitize-article params)]
      (json-success (dp/insert-blog-article data-provider article user-id)))
    (json-failure 403 {:message "Forbidden"})))

(defn update-article
  "Route handler for updating an existing article"
  [data-provider user params]
  (if (auth/editor? user)
    (let [user-id (:_id user)
          article (sanitize-article params)]
      (json-success (dp/update-blog-article data-provider article user-id)))
    (json-failure 403 {:message "Forbidden"})))

(defn post-media
  "Route handler for uploading media"
  [data-provider user params]
  (if (auth/editor? user)
    (let [files (parse-files params)
          user-id (:_id user)
          media-ids (accum-media data-provider files user-id)]
      (println "files" files)
      (println "media-ids" media-ids)
      (let [result (upload-file (first files))]
        (if (nil? @result)
          (json-success media-ids)
          (json-failure 500 {:message "Media upload failed"}))))
    (json-failure 403 {:message "Forbidden"})))

(defn list-media
  "Route handler to list media"
  [data-provider page per-page]
  (let [page (if page (Integer. page) 1)
        per-page (if per-page (Integer. per-page) 10)]
    (json-success (dp/blog-media data-provider {:page page :per-page per-page}))))

(defn media-by-id
  "Route handler for a single media description by id"
  [data-provider id]
  (json-success (list (dp/blog-media-by-id data-provider id))))

(defn list-users
  "Route handler for a list of all users"
  [data-provider user page per-page]
  (if (auth/admin? user)
    (let [page (if page (Integer. page) 1)
          per-page (if per-page (Integer. per-page) 10)]
      (json-success (map auth/public-user (dp/users data-provider {:page page :per-page per-page}))))
    (json-failure 403 {:message "Forbidden"})))

(defn me
  "Route handler for currently logged in user"
  [user]
  (json-success user))

(defn user-by-id
  "Route handler for a single user by id"
  [data-provider user id]
  (when (auth/admin? user)
    (json-success (list (dp/user data-provider id)))))

(defn signin
  "Route handler for signing in"
  [data-provider session username password]
  (if-let [user (auth/user username password)]
    (let [sess (assoc session :user (str (:_id user)))]
      {:body (pages/sign-in-success user) :session sess})
    (pages/sign-in nil "Invalid login")))

(defn signout
  "Route handler for signing out"
  [data-provider user session]
  { :status 200 :session nil })

(defn log-event
  "Route handler for logging events"
  [data-provider user errorUrl category event]
  (json-success (dp/insert-log-event data-provider {:user user :error-url errorUrl :category category :event event})))

(defroutes routes
  (GET "/sitemap.txt" {} (sitemap db/data-provider))

  ;; Main
  (GET "/" {user :user}
       (pages/root user))
  (GET "/photography" {user :user}
       (pages/photography user))
  (GET "/about" {user :user}
       (pages/about user))
  (GET "/signin" {user :user}
       (pages/sign-in user))

  ;; Blog
  (GET "/blog" {user :user {:keys [slug]} :params {:strs [user-agent]} :headers :as request}
       (pages/articles user "Articles" (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "published" {:page 0 :per-page 100 :tagged nil}))))
  (GET "/blog/tagged/:tag{[0-9a-z-]+}" {user :user {:keys [tag]} :params {:strs [user-agent]} :headers :as request}
       (pages/articles user (str "Articles Tagged " tag) (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "published" {:page 0 :per-page 100 :tagged tag}))))
  (GET "/blog/drafts" {user :user {:keys [tag]} :params {:strs [user-agent]} :headers :as request}
       (when (auth/editor? user)
         (pages/articles user "Drafts" (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "draft" {:page 0 :per-page 100 :tagged tag})))))
  (GET "/blog/new" {user :user {:keys [slug]} :params {:strs [user-agent]} :headers :as request}
       (when (auth/editor? user) (pages/edit-article user)))
  (GET "/blog/:slug{[0-9a-z-]+}" {user :user {:keys [slug]} :params {:strs [user-agent]} :headers :as request}
       (when-let [article (dp/blog-article-by-slug db/data-provider slug {:status ["published" (when (auth/editor? user) "draft")]})]
         (pages/article user (hydrate/article db/data-provider article) (request-url request))))
  (GET "/edit/:slug{[0-9a-z-]+}" {user :user {:keys [slug]} :params {:strs [user-agent]} :headers :as request}
       (when-let [article (dp/blog-article-by-slug db/data-provider slug {:status ["published" (when (auth/editor? user) "draft")]})]
         (pages/edit-article user (hydrate/article db/data-provider article) (request-url request))))

  ;; JSON API
  (GET "/blog/count.json" {} (article-count db/data-provider))
  (GET "/blog/drafts/count.json" {user :user} (draft-article-count db/data-provider user))

  (GET "/blog/articles.json" {user :user {:strs [page per-page tagged]} :query-params} (published db/data-provider user page per-page tagged))
  (GET "/blog/articles/:id{[0-9a-f]+}.json" {user :user {:keys [id]} :params} (article-by-id db/data-provider user id))
  (GET "/blog/articles/:slug{[0-9a-z-]+}.json" {user :user {:keys [slug]} :params} (article-by-slug db/data-provider user slug))

  (POST "/blog/articles/post.json" {user :user params :params} (post-article db/data-provider user params))
  (POST "/blog/articles/:id.json" {user :user params :params} (update-article db/data-provider user params))
  (POST "/blog/media.json" {user :user params :params} (post-media db/data-provider user params))

  (GET "/blog/drafts/articles.json" {user :user {:strs [page per-page tagged]} :query-params} (drafts db/data-provider user page per-page tagged))

  (GET "/blog/media.json" {{:strs [page per-page]} :query-params} (list-media db/data-provider page per-page))
  (GET "/blog/media/:id.json" [id] (media-by-id db/data-provider id))

  (GET "/users.json" {{:strs [page per-page]} :query-params user :user} (list-users db/data-provider user page per-page))
  (GET "/users/me.json" {user :user} (me db/data-provider user))
  (GET "/users/:id.json" {user :user {:keys [id]} :params} (user db/data-provider user id))

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

  (POST "/auth/signin" {session :session {:strs [username password]} :params} (signin db/data-provider session username password))
  (POST "/auth/signout" {user :user session :session} (signout db/data-provider user session))

  (POST "/log/event" {user :user {:strs [errorUrl category event]} :params} (log-event db/data-provider user errorUrl category event))

  (route/resources "/" :root "public")

  ;; all other requests
  (rfn {user :user} (pages/not-found user)))

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
      (handler (assoc request :user (auth/private-user (dp/user db/data-provider user-id))))
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
  (println (str (select-keys request [:session :user :uri :request-method :query-string]) "\n"))
  request)

(defn wrap-logger
  "Log stuff"
  [handler & [options]]
  (fn [request]
    (handler (log-request request options))))

(def application (-> routes
                     wrap-logger
                     wrap-user
;                     (wrap-session {:store (DBSessionStore.)})
                     (wrap-session {:store (cookie-store {:key config/session-key})})
;                     wrap-session
                     wrap-cookies
                     wrap-params
                     wrap-multipart-params))

