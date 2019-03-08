(ns mango.bootstrap
  (:require [mango.db :as db]
            [mango.auth :as auth]
            [mango.util :refer [slugify]]))

(def default-article
  (let [title "Hello World"]
        {:title title
         :description "This is a default article for your new site"
         :content "# Welcome to Mango
You may delete this article and replace it with your own content!

This software is a hobby project and no guarantees are made about its correctness of performance. If you'd like to contribute, see [leeaustinadams/mango](https://github.com/leeaustinadams/mango).

"
         :created (clj-time.core/now)
         :tags ["dev"]
         :status "published"
         :slug (slugify title :limit 10)}))

(defn run
  "Bootstrap an empty site"
  []
  (when (< (count (db/users {})) 1)
    (println "No users found, bootstrapping users")
    (if-let [admin (auth/new-user "admin" "admin" ["admin" "editor"])]
      (when (< (db/blog-articles-count "published" {}) 1)
        (println "No articles found, bootstrapping default article")
        (db/insert-blog-article default-article (:_id admin))))))
