;; http://ring-clojure.github.io/ring/
;; https://github.com/dakrone/cheshire
;; https://github.com/weavejester/compojure
(ns mango.routes
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session.cookie :refer [cookie-store]]
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
            [mango.util :refer [slugify xform-ids xform-tags xform-time-to-string xform-string-to-time url-decode]]))

(defn sanitize-article
  "Cleans and prepares an article from parameters posted"
  [params]
  (let [article (-> params
                    keywordize-keys
                    (select-keys [:_id :title :description :content :created :media :tags :status]))
        media (xform-ids (:media article))
        created (xform-string-to-time (:created article))
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

(defn json-status
  "A JSON response for a status. Generates JSON from the obj passed in"
  [status obj & rest]
  (reduce merge {
                 :status status
                 :headers {"Content-Type" "application/json"}
                 :body (generate-string obj)}
          rest))

(defn json-success
  "A JSON response for a success (200). Generates JSON from the obj passed in"
  [obj & [rest]]
  (json-status 200 obj rest))

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

(defn redir-response
  "A response for a redirection."
  [code location]
  {
   :status code
   :headers {"Content-Type" "text/html"
             "Location" location}})

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
  [{:keys [filename tempfile content-type]}]
  (storage/upload config/aws-media-bucket (str "blog/" filename) tempfile content-type))

(defn article-response
  "Renders a response for a hydrated article"
  [data-provider article]
  (if (not (empty? article))
    (json-success (hydrate/article data-provider article))
    (json-status 404 {:msg "Not found"})))

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
    (json-status 403 {:message "Forbidden"})))

(defn published
  "Route handler for a page worth of articles"
  [data-provider user options]
  (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "published" options))))

(defn drafts
  "Route handler for a page worth of drafts"
  [data-provider user options]
  (if (auth/editor? user)
    (json-success (hydrate/articles data-provider (dp/blog-articles data-provider "draft" options)))
    (json-status 403 {:message "Forbidden"})))

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
          article (sanitize-article params)
          inserted (dp/insert-blog-article data-provider article user-id)]
      (redir-response 302 (str "/blog/" (:slug article))))
    (json-status 403 {:message "Forbidden"})))

(defn update-article
  "Route handler for updating an existing article"
  [data-provider user params]
  (if (auth/editor? user)
    (let [user-id (:_id user)
          article (sanitize-article params)
          updated (dp/update-blog-article data-provider article user-id)]
      (redir-response 302 (str "/blog/" (:slug article))))
    (json-status 403 {:message "Forbidden"})))

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
          (json-status 500 {:message "Media upload failed"}))))
    (json-status 403 {:message "Forbidden"})))

(defn list-media
  "Route handler to list media"
  [data-provider user options]
  (if (auth/editor? user)
    (json-success (dp/blog-media data-provider options))
    (json-status 403 {:message "Forbidden"})))

(defn media-by-id
  "Route handler for a single media description by id"
  [data-provider user id]
  (if (auth/editor? user)
    (json-success (list (dp/blog-media-by-id data-provider id)))
    (json-status 403 {:message "Forbidden"})))

(defn list-users
  "Route handler for a list of all users"
  [data-provider user options]
  (if (auth/admin? user)
    (json-success (map auth/public-user (dp/users data-provider options)))
    (json-status 403 {:message "Forbidden"})))

(defn me
  "Route handler for currently logged in user"
  [user]
  (json-success user))

(defn user-by-id
  "Route handler for a single user by id"
  [data-provider user id]
  (when (auth/admin? user)
    (json-success (list (dp/user data-provider id)))))

(defn session-anti-forgery-token
  [session]
  (get session :ring.middleware.anti-forgery/anti-forgery-token))

(defn signin
  "Route handler for signing in"
  [data-provider session username password]
  (if-let [user (auth/user username password)]
    (let [sess (assoc session :user (str (:_id user)))]
      (merge (redir-response 302 "/")
             {:session sess}))
    (pages/sign-in nil (session-anti-forgery-token session) "Invalid login")))

(defn signout
  "Route handler for signing out"
  [data-provider user session]
  (merge (redir-response 302 "/")
         { :session nil}))

(defroutes routes
  (GET "/sitemap.txt" {} (sitemap db/data-provider))

  ;; Main
  (GET "/" {:keys [user]}
       (pages/root user))
  (GET "/photography" {:keys [user]}
       (pages/photography user))
  (GET "/about" {:keys [user]}
       (pages/about user))
  (GET "/signin" {:keys [user session]}
       (pages/sign-in user (session-anti-forgery-token session)))
  (GET "/signout" {:keys [user session]}
       (pages/sign-out user (session-anti-forgery-token session)))

  ;; Blog
  (GET "/blog" {:keys [user]}
       (pages/articles-list user "Articles" (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "published" {:page 0 :per-page 100 :tagged nil}))))
  (GET "/blog/tagged/:tag" {user :user {:keys [tag]} :params}
       (pages/articles-list user (str "Articles Tagged \"" (url-decode tag) \") (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "published" {:page 0 :per-page 100 :tagged (url-decode tag)}))))
  (GET "/blog/drafts" {user :user {:keys [tag]} :params}
       (when (auth/editor? user)
         (pages/articles-list user "Drafts" (hydrate/articles db/data-provider (dp/blog-articles db/data-provider "draft" {:page 0 :per-page 100 :tagged tag})))))
  (GET "/blog/new" {:keys [user session]}
       (when (auth/editor? user) (pages/edit-article user (session-anti-forgery-token session))))
  (GET "/blog/:slug{[0-9a-z-]+}" {user :user {:keys [slug]} :params :as request}
       (when-let [article (dp/blog-article-by-slug db/data-provider slug {:status ["published" (when (auth/editor? user) "draft")]})]
         (pages/article user (hydrate/article db/data-provider article) (request-url request))))
  (GET "/edit/:slug{[0-9a-z-]+}" {:keys [user session] {:keys [slug]} :params}
       (when (auth/editor? user)
         (when-let [article (dp/blog-article-by-slug db/data-provider slug {:status ["published" "draft"]})]
           (pages/edit-article user (session-anti-forgery-token session) (hydrate/article db/data-provider article)))))
  (GET "/me" {:keys [user]}
       (when user (pages/user-details user)))

  ;; JSON API
  (GET "/blog/count.json" {} (article-count db/data-provider))
  (GET "/blog/drafts/count.json" {:keys [user]} (draft-article-count db/data-provider user))

  (GET "/blog/articles.json" {:keys [user params]} (published db/data-provider user params))
  (GET "/blog/articles/:id{[0-9a-f]+}.json" {user :user {:keys [id]} :params} (article-by-id db/data-provider user id))
  (GET "/blog/articles/:slug{[0-9a-z-]+}.json" {user :user {:keys [slug]} :params} (article-by-slug db/data-provider user slug))

  (POST "/blog/articles/post.json" {:keys [user params]} (post-article db/data-provider user params))
  (POST "/blog/articles/:id.json" {:keys [user params]} (update-article db/data-provider user params))
  (POST "/blog/media.json" {:keys [user params]} (post-media db/data-provider user params))

  (GET "/blog/drafts/articles.json" {:keys [user params]} (drafts db/data-provider user params))

  (GET "/blog/media.json" {:keys [user params]} (list-media db/data-provider user params))
  (GET "/blog/media/:id.json" {user :user {:keys [id]} :params} (media-by-id db/data-provider user id))

  (GET "/users.json" {:keys [user params]} (list-users db/data-provider user params))
  (GET "/users/me.json" {:keys [user]} (me db/data-provider user))
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

  (POST "/auth/signin" {session :session {:keys [username password]} :params} (signin db/data-provider session username password))
  (POST "/auth/signout" {:keys [user session]} (signout db/data-provider user session))

  (route/resources "/" :root "public")

  ;; all other requests
  (rfn {:keys [user]} (pages/not-found user)))

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
  (println (str (select-keys request [:session :user :uri :request-method :query-string :params]) "\n"))
  request)

(defn wrap-logger
  "Log stuff"
  [handler & [options]]
  (fn [request]
    (handler (log-request request options))))

(def this-site-defaults
  (-> site-defaults
      (assoc-in [:session :store] (cookie-store {:key config/session-key}))))

(def application (-> routes
                     wrap-logger
                     wrap-user
                     (wrap-defaults this-site-defaults)))
