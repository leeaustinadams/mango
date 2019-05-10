(ns mango.json-api-test
  (:require [clojure.test :refer :all]
            [mango.fixtures :as fixtures]
            [mango.json-api :refer :all]))

(def json-headers {"Content-Type" "application/json"})

(deftest test-json-success
  (is (= (json-success nil)
         {:status 200 :headers json-headers :body "null"}))
  (is (= (json-success {:foo "bar"})
         {:status 200
          :headers json-headers
          :body "{\"foo\":\"bar\"}"}))
  (is (= (json-success {:foo "bar"} {:other "thing"})
         {:status 200
          :headers json-headers
          :body "{\"foo\":\"bar\"}"
          :other "thing"})))

(deftest test-json-status
  (is (= (json-status 200 nil)
         {:status 200 :headers json-headers :body "null"}))
  (is (= (json-status 302 {:foo "bar"})
         {:status 302
          :headers json-headers
          :body "{\"foo\":\"bar\"}"}))
  (is (= (json-status 420 {:foo "bar"} {:other "thing"})
         {:status 420
          :headers json-headers
          :body "{\"foo\":\"bar\"}"
          :other "thing"}))
  (is (= (json-status 404 {:message "not found"})
         {:status 404
          :headers json-headers
          :body "{\"message\":\"not found\"}"}))
  (is (= (json-status 401 {:message "unauthorized"} {:other "thing"})
         {:status 401
          :headers json-headers
          :body "{\"message\":\"unauthorized\"}"
          :other "thing"})))

(deftest test-article-count
  (is (= (article-count fixtures/data-provider)
         {:status 200,
          :headers json-headers
          :body "{\"count\":2}"})))

(deftest test-draft-article-count
  (is (= (draft-article-count fixtures/data-provider)
         {:status 200,
          :headers json-headers
          :body "{\"count\":0}"})))

(deftest test-published
  (is (= (published fixtures/data-provider nil)
         {:status 200
          :headers json-headers
          :body (str "[{\"content\":\"Hello\",\"media\":"
          "[{\"_id\":1,\"filename\":\"1.jpg\",\"src\":\"http://localhost/blog/1.jpg\"},{\"_id\":2,\"filename\":\"2.jpg\",\"src\":\"http://localhost/blog/2.jpg\"}],"
          "\"user\":{\"username\":\"User\",\"roles\":[]},"
          "\"rendered-content\":\"<p>Hello</p>\"},"
          "{\"content\":\"Howdy\",\"media\":"
          "[{\"_id\":3,\"filename\":\"3.jpg\",\"src\":\"http://localhost/blog/3.jpg\"},{\"_id\":4,\"filename\":\"4.jpg\",\"src\":\"http://localhost/blog/4.jpg\"}],"
          "\"user\":{\"username\":\"Editor\",\"roles\":[\"editor\"]},"
          "\"rendered-content\":\"<p>Howdy</p>\"}]")})))

(deftest test-drafts
  (is (not (nil? (drafts fixtures/data-provider nil)))))

(deftest test-list-users
  (is (= (list-users fixtures/data-provider nil)
         {:status 200,
          :headers {"Content-Type" "application/json"},
          :body "[{\"username\":\"User\",\"roles\":[]},{\"username\":\"Editor\",\"roles\":[\"editor\"]},{\"username\":\"Admin\",\"roles\":[\"admin\"]}]"})))
