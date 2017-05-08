(ns mango.fixtures
  (:require [mango.auth :refer [public-user]]
            [mango.hydrate :refer [DataProvider]]))

(def user {:_id 1234
           :password "foo"
           :salt "salty"
           :provider :local
           :email "test@foo.com"
           :roles []})

(def editor {:_id 5678
             :password "bar"
             :salt "salty"
             :provider :local
             :email "test@bar.com"
             :roles ["editor"]})

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
    mango.hydrate.DataProvider
  (media-by-ids [this ids] [{:foo 1}, {:foo 2}, {:foo 3}, {:foo 4}])
  (user-by-id [this id] (if (= id 1234) user editor)))

(def data-provider (MockDataProvider.))
