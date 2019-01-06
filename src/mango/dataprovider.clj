(ns mango.dataprovider)

(defprotocol DataProvider
  (media-by-ids [this ids])
  (blog-media [this options])
  (blog-media-by-id [this id])
  (users [this options])
  (user-by-id [this id])
  (insert-blog-media [this media user-id])
  (update-blog-media [this media user-id])
  (delete-blog-media [this media])
  (delete-blog-media-by-id [this media-id])
  (blog-articles [this status options])
  (blog-articles-count [this status])
  (blog-article-by-id [this id options])
  (blog-article-by-slug [this slug options])
  (insert-blog-article [this article user-id])
  (update-blog-article [this article user-id])
  (update-blog-article-media [this article-id media-id])
  (insert-log-event [this event]))
