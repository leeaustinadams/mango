(ns mango.dataprovider)

(defprotocol DataProvider
  (media-by-ids [this ids])
  (blog-media [this options])
  (blog-media-by-id [this id])
  (users [this options])
  (insert-user [this user])
  (update-user [this user])
  (user-by-id [this id])
  (user-by-username [this username])
  (delete-user-by-id [this id])
  (insert-blog-media [this media user-id])
  (update-blog-media [this media user-id])
  (delete-blog-media [this media])
  (delete-blog-media-by-id [this media-id])
  (blog-articles [this options])
  (blog-articles-count [this options])
  (blog-article-by-id [this id options])
  (blog-article-by-slug [this slug options])
  (insert-blog-article [this article user-id])
  (update-blog-article [this article user-id])
  (update-blog-article-media [this article-id media-id])
  (blog-article-tags [this options])
  (pages [this options])
  (page-by-slug [this slug options])
  (insert-page [this page user-id])
  (update-page [this page user-id])
  (update-page-media [this page-id media-id]))
