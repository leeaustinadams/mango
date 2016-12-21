(ns mango.util-test
  (:require [clojure.test :refer :all]
            [mango.util :refer :all]))

(deftest without-limits
  (is (= "test" (slugify "test")))
  (is (= "test" (slugify "Test")))
  (is (= "this-is-a-test" (slugify "this is a test")))
  (is (= "this-is-a-test" (slugify "This is a TeST")))
  (is (= "this-is-a-test" (slugify "this is a ??? test")))
  (is (= "this-has-digits-0123456789" (slugify "this has digits 0123456789"))))

(deftest with-limits
  (is (= "test" (slugify "test" :limit 1)))
  (is (= "test" (slugify "test" :limit 4)))
  (is (= "this-is-a-test" (slugify "this is a test" :limit 4)))
  (is (= "this-is-a-test" (slugify "this is a test" :limit 100)))
  (is (= "this-is-a" (slugify "this is a test" :limit 3)))
  (is (= "this-is-a-test" (slugify "This is a Test" :limit 4)))
  (is (= "this-is-a-test" (slugify "This is a ??? test" :limit 4)))
  (is (= "this-has-digits-0123456789" (slugify "this has digits 0123456789 and is long" :limit 4))))
