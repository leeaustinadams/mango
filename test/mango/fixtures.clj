(ns mango.fixtures
  (:require [clojure.test :refer :all]))

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
