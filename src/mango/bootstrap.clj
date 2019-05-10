(ns mango.bootstrap
  (:require [mango.db :as db]
            [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [slugify]]
            [mango.storage :as storage]
            [taoensso.timbre :refer [info]]))


(def default-user
  {:username "admin"
   :first-name ""
   :last-name ""
   :email config/admin-email
   :twitter-handle config/twitter-site-handle
   :roles ["admin" "editor"]
   :password "admin"})

(def default-article
  (let [title "Hello World"]
        {:title title
         :description "This is a default article for your new site"
         :content (str "# Welcome to Mango\n\n"
                       "You may delete this article and replace it with your own content!\n"
                       "This software is a hobby project and no guarantees are made about its correctness of performance. If you'd like to contribute, see [leeaustinadams/mango](https://github.com/leeaustinadams/mango).\n"
                       "## Default User\n"
                       "Username: " (:username default-user) "\n"
                       "Password: " (:password default-user) "\n")
         :created (clj-time.core/now)
         :tags ["dev"]
         :status "published"
         :slug (slugify title :limit 10)}))

(def default-root-page
  (let [title config/site-title]
    {:title title
     :content (str "## [Blog](/blog)\n\n"
                   "## [First Page: Making New Pages](/pages/making-new-pages)")
     :status "published"
     :slug (slugify title :limit 10)}))

(def default-making-new-pages
  (let [title "Making New Pages"]
    {
     :title title
     :content (str "1. Log in\n"
                   "1. Tap [New Page](/pages/new)\n"
                   "1. Fill in the page's title\n"
                   "1. Add media as needed.\n"
                   "1. Fill in the page's content, Markdown or HTML is supported too!\n"
                   "1. Set the page's status to `Published`\n"
                   "1. Hit Submit\n"
                   "1. Link to the new page from the root page or any other pages you've created!")
     :status "published"
     :slug (slugify title)
     }))

(defn run
  "Bootstrap an empty site"
  []
  (when (< (count (db/users {})) 1)
    (info "No users found, bootstrapping users")
    (when-let [user (auth/new-user default-user)]
      (when (< (db/blog-articles-count "published" {}) 1)
        (info "No articles found, bootstrapping default article")
        (db/insert-blog-article default-article (:_id user)))
      (when (empty? (db/pages {}))
        (info "No root page found, bootstrapping default pages")
        (let [user-id (:_id user)]
          (db/insert-page default-root-page user-id)
          (db/insert-page default-making-new-pages user-id)))))
  (storage/init-bucket config/aws-media-bucket))
