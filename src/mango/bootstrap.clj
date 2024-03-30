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
   :roles ["admin" "editor"]
   :password "admin"})

(def default-article
  (let [title "Hello World"]
        {:title title
         :description "This is a default article for your new site"
         :content (str "# Welcome to Mango\n\n"
                       "You may delete this article and replace it with your own content!\n")
         :created (clj-time.core/now)
         :tags ["dev"]
         :status "published"
         :slug (slugify title :limit 10)}))

(def default-draft
  (let [title "This is a draft"]
    {:title title
     :description "This article exists but is not publicly accessible"
     :content "You can draft up articles without publishing them. Once you're ready for it to go live, just change the status to \"Published\" from \"Draft\" and submit it."
     :created (clj-time.core/now)
     :tags ["dev"]
     :status "draft"
     :slug (slugify title :limit 10)}))

(def default-root-page
  (let [title config/site-title]
    {:title title
     :content (str "## [Blog](/blog)\n"
                   "## [Getting Started](/pages/getting-started)\n"
                   "## [Making New Pages](/pages/making-new-pages)\n"
                   "## [About the Root Page](/pages/about-the-root-page)\n")
     :status "root"
     :slug (slugify title :limit 10)}))

(def getting-started
  (let [title "Getting Started"]
    {
     :title title
     :content (str "This software is a hobby project and no guarantees are made about its correctness of performance. If you'd like to contribute, see [leeaustinadams/mango](https://github.com/leeaustinadams/mango).\n"
                   "## Default User\n"
                   "Username: " (:username default-user) "\n"
                   "Password: " (:password default-user) "\n")
     :status "published"
     :slug (slugify title)
     }))

(def making-new-pages
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

(def about-the-root-page
  (let [title "About the Root Page"]
    {
     :title title
     :content (str "The root page is a special page that is shown when a browser navigates to the url of your site. (e.g. https://4d4ms.com/)\n"
                   "It can be edited like any other page, but it cannot be deleted or unpublished\n"
                   "You can edit your root page [here](/pages/edit/" (:slug default-root-page) ")\n")
     :status "published"
     :slug (slugify title)
     }))

(def default-articles [default-article default-draft])
(def default-pages [getting-started default-root-page making-new-pages about-the-root-page])

(defn run
  "Bootstrap an empty site"
  []
  (when (< (count (db/users {})) 1)
    (info "No users found, bootstrapping users")
    (when-let [user (auth/new-user db/data-provider default-user)]
      (let [user-id (:_id user)]
        (when (< (db/blog-articles-count {:status ["published"]}) 1)
          (info "No articles found, bootstrapping default articles")
          (doseq [article default-articles] (db/insert-blog-article article user-id)))
        (when (empty? (db/pages {:status ["root"]}))
          (info "No root page found, bootstrapping default pages")
          (doseq [page default-pages] (db/insert-page page user-id))))))
  (storage/init-bucket config/aws-media-bucket))
