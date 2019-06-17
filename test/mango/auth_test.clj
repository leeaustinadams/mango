(ns mango.auth-test
  (:require [clojure.test :refer :all]
            [mango.auth :refer :all]
            [mango.fixtures :as fixtures]))

(deftest test-encrypt-user-password
  (let [plaintext-password "secret"
        user (:password (encrypt-user-password fixtures/user plaintext-password))]
    (is (not= (:password user) plaintext-password))))

(deftest test-user
  (is (not (nil? (user fixtures/data-provider "User" "passwd")))))

(deftest test-check-password
  (is (true? (check-password fixtures/data-provider "User" "passwd")))
  (is (false? (check-password fixtures/data-provider "User" "foo"))))

(deftest test-private-user
  (is (empty? (select-keys (public-user fixtures/user) [:password :provider]))))

(deftest test-public-user
  (is (empty? (select-keys (public-user fixtures/user) [:_id :password :provider :email]))))

(deftest test-editor?
  (is (not (editor? fixtures/user)))
  (is (editor? fixtures/editor)))

(deftest test-admin?
  (is (not (admin? fixtures/user)))
  (is (not (admin? fixtures/editor)))
  (is (admin? fixtures/admin)))
