(ns mango.auth-test
  (:require [clojure.test :refer :all]
            [mango.auth :refer :all]
            [mango.fixtures :as fixtures]))

(deftest test-editor?
  (is (not (editor? fixtures/user)))
  (is (editor? fixtures/editor)))

(deftest test-public-user
  (is (empty? (select-keys (public-user fixtures/user) [:_id :password :provider :email]))))
