(ns mango.fixtures
  (:require
   [cheshire.core :refer [generate-string]]
   [crypto.password.pbkdf2 :as password]
   [mango.auth :refer [public-user]]
   [mango.config :as config]
   [mango.dataprovider :refer [DataProvider]]))

(def forbidden-result {:status 403
                       :headers {"Content-Type" "application/json"}
                       :body (generate-string {:message "Forbidden"})})

(def user {:username "User"
           :first-name "Foo"
           :last-name "Bar"
           :email "user@foo.com"
           :twitter-handle "foouser"
           :password (password/encrypt "passwd")
           :roles []})

(def editor {:username "Editor"
             :first-name "Ed"
             :last-name "Itor"
             :email "editor@foo.com"
             :twitter-handle "editoruser"
             :password (password/encrypt "passwde")
             :roles ["editor"]})

(def admin {:username "Admin"
            :first-name "Ad"
            :last-name "Min"
            :email "admin@foo.com"
            :twitter-handle "adminuser"
            :password (password/encrypt "passwda")
            :roles ["admin"]})

(def users-by-id {1234 user 5678 editor 91011 admin})
(def users-by-username {"User" user "Editor" editor "Admin" admin})

(def media0 {:_id 0 :filename "0.jpg"})
(def media1 {:_id 1 :filename "1.jpg"})
(def media2 {:_id 2 :filename "2.jpg"})
(def media3 {:_id 3 :filename "3.jpg"})
(def media4 {:_id 4 :filename "4.jpg"})
(def media [media0 media1 media2 media3 media4])

(def article {:content "Hello"
              :media [1 2]
              :user 1234})

(def hydrated-article {:content "Hello"
                       :rendered-content "<p>Hello</p>"
                       :user (public-user user)
                       :media [{:_id 1 :filename "1.jpg" :src (str config/cdn-url "1.jpg")}
                               {:_id 2 :filename "2.jpg" :src (str config/cdn-url "2.jpg")}]})

(def article2 {:content "Howdy"
               :media [3 4]
               :user 5678})

(def hydrated-article2 {:content "Howdy"
                        :rendered-content "<p>Howdy</p>"
                        :user (public-user editor)
                        :media [{:_id 3 :filename "3.jpg" :src (str config/cdn-url "3.jpg")}
                                {:_id 4 :filename "4.jpg" :src (str config/cdn-url "4.jpg")}]})

(def articles [article article2])

(def page
  {:title "Hi"
   :content "Hello World"
   :slug "hi"})

(def hydrated-page
  {:title "Hi"
   :content "Hello World"
   :rendered-content "<p>Hello World</p>"
   :slug "hi"})

(def page2 {:title "Howdy"
            :content "Hey there"
            :rendered-content "<p>Hey there</p>"
            :slug "howdy"})

(def pages [page page2])

(def hydrated-page2
  {:title "Howdy"
   :content "Hey there"
   :slug "howdy"})

(deftype MockDataProvider []
  DataProvider
  (media-by-ids
    [this ids]
    (if (empty? ids)
      nil
      (for [i ids] (get media i))))
  (blog-media [this options] nil)
  (blog-media-by-id [this id] nil)
  (users [this options] (vals users-by-id))
  (user-by-id [this id] (get users-by-id id))
  (user-by-username [this username] (get users-by-username username))
  (insert-blog-media [this media user-id] nil)
  (blog-articles
    [this options]
    (if (= (:status options) "published")
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
  (pages [this options] pages)
  (page-by-slug [this slug options] (first (filter #(= (:slug %) slug) pages)))
  (insert-page [this page user-id] nil)
  (update-page [this page user-id] nil))

(def data-provider (MockDataProvider.))
