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

(def article {:content "Hello"
              :media [1 2 3 4]
              :user 1234})

(def hydrated-article {:content "Hello"
                       :rendered-content "<p>Hello</p>"
                       :user (public-user user)
                       :media [{:foo 1}, {:foo 2}, {:foo 3}, {:foo 4}]})

(def article2 {:content "Howdy"
               :media [5 6 7]
               :user 5678})

(def hydrated-article2 {:content "Howdy"
                        :rendered-content "<p>Howdy</p>"
                        :user (public-user editor)
                        :media [{:foo 1}, {:foo 2}, {:foo 3}, {:foo 4}]})

(deftype MockDataProvider []
  DataProvider
  (media-by-ids [this ids] [{:foo 1}, {:foo 2}, {:foo 3}, {:foo 4}])
  (blog-media [this options] nil)
  (blog-media-by-id [this id] nil)
  (users [this options] (vals users))
  (user-by-id [this id] (get users id))
  (insert-blog-media [this media user-id] nil)
  (blog-articles [this status options] nil)
  (blog-articles-count [this status] nil)
  (blog-article-by-id [this id options] nil)
  (blog-article-by-slug [this slug options] nil)
  (insert-blog-article [this article user-id] nil)
  (update-blog-article [this article user-id] nil)
  (insert-log-event [this event] nil))

(def data-provider (MockDataProvider.))
