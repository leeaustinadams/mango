(ns mango.fixtures
  (:require [mango.auth :refer [public-user]]
            [mango.dataprovider :refer [DataProvider]]))

(def user {:_id 1234
           :username "User"
           :password "foo"
           :salt "salty"
           :provider :local
           :email "user@foo.com"
           :roles []})

(def editor {:_id 5678
             :username "Editor"
             :password "bar"
             :salt "salty"
             :provider :local
             :email "editor@bar.com"
             :roles ["editor"]})

(def admin {:_id 91011
            :username "Admin"
            :password "baz"
            :salt "salty"
            :provider :local
            :email "admin@bar.com"
            :roles ["admin"]})

(def users {1234 user 5678 editor 91011 admin})

(def media0 {:_id 0 :src "0.jpg"})
(def media1 {:_id 1 :src "1.jpg"})
(def media2 {:_id 2 :src "2.jpg"})
(def media3 {:_id 3 :src "3.jpg"})
(def media4 {:_id 4 :src "4.jpg"})
(def media [media0 media1 media2 media3 media4])

(def article {:content "Hello"
              :media [1 2]
              :user 1234})

(def hydrated-article {:content "Hello"
                       :rendered-content "<p>Hello</p>"
                       :user (public-user user)
                       :media [{:_id 1 :src "http://test-cdn.4d4ms.com/blog/1.jpg"}
                               {:_id 2 :src "http://test-cdn.4d4ms.com/blog/2.jpg"}]})

(def article2 {:content "Howdy"
               :media [3 4]
               :user 5678})

(def hydrated-article2 {:content "Howdy"
                        :rendered-content "<p>Howdy</p>"
                        :user (public-user editor)
                        :media [{:_id 3 :src "http://test-cdn.4d4ms.com/blog/3.jpg"}
                                {:_id 4 :src "http://test-cdn.4d4ms.com/blog/4.jpg"}]})

(def articles [article article2])

(deftype MockDataProvider []
  DataProvider
  (media-by-ids
    [this ids]
    (if (empty? ids)
      nil
      (for [i ids] (get media i))))
  (blog-media [this options] nil)
  (blog-media-by-id [this id] nil)
  (users [this options] (vals users))
  (user [this id] (get users id))
  (user-by-id [this id] (get users id))
  (insert-blog-media [this media user-id] nil)
  (blog-articles
    [this status options]
    (if (= status "published")
      articles
      nil))
  (blog-articles-count
    [this status]
    (if (= status "published")
      (count articles)
      0))
  (blog-article-by-id [this id options] nil)
  (blog-article-by-slug [this slug options] nil)
  (insert-blog-article [this article user-id] nil)
  (update-blog-article [this article user-id] nil)
  (insert-log-event [this event] nil))

(def data-provider (MockDataProvider.))
