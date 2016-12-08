(ns mango.auth-test
  (:require [clojure.test :refer :all]
            [mango.auth :refer :all]))

(def test-user {:_id 1234 
                :password "foo" 
                :salt "salty" 
                :provider :local 
                :email "test@foo.com"
                :roles []})

(def test-editor {:_id 5678 
                :password "bar" 
                :salt "salty" 
                :provider :local 
                :email "test@bar.com"
                :roles ["editor"]})

(deftest test-editor?
  (is (not (editor? test-user)))
  (is (editor? test-editor)))

(deftest test-public-user
  (is (empty? (select-keys (public-user test-user) [:_id :password :salt :provider :email]))))
