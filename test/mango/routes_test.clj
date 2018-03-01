(ns mango.routes-test
  (:require [clojure.test :refer :all]
            [mango.fixtures :as fixtures]
            [mango.routes :refer :all]))

(def json-headers {"Content-Type" "application/json"})
(def html-headers {"Content-Type" "text/html"})

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

(deftest test-json-failure
  (is (= (json-failure 404 {:message "not found"})
         {:status 404
          :headers json-headers
          :body "{\"message\":\"not found\"}"}))
  (is (= (json-failure 401 {:message "unauthorized"} {:other "thing"})
         {:status 401
          :headers json-headers
          :body "{\"message\":\"unauthorized\"}"
          :other "thing"})))

(def forbidden-result (json-failure 403 {:message "Forbidden"}))

(deftest test-html-success
  (is (= (html-success "") {:status 200 :headers html-headers :body ""}))
  (is (= (html-success "<p>foo</p>") {:status 200 :headers html-headers :body "<p>foo</p>"})))

(deftest test-sitemap
  (is (not (nil? (sitemap fixtures/data-provider)))))

(deftest test-article-count
  (is (= (article-count fixtures/data-provider)
         {:status 200,
          :headers json-headers
          :body "{\"count\":2}"})))

(deftest test-draft-article-count
  (is (= (draft-article-count fixtures/data-provider fixtures/user) forbidden-result))
  (is (= (draft-article-count fixtures/data-provider fixtures/editor)
         {:status 200,
          :headers json-headers
          :body "{\"count\":0}"})))

(deftest test-published
  (is (= (published fixtures/data-provider fixtures/user 1 10 nil)
         {:status 200
          :headers json-headers
          :body (str "[{\"content\":\"Hello\",\"media\":"
          "[{\"_id\":1,\"src\":\"http://test-cdn.4d4ms.com/blog/1.jpg\"},{\"_id\":2,\"src\":\"http://test-cdn.4d4ms.com/blog/2.jpg\"}],"
          "\"user\":{\"username\":\"User\",\"roles\":[]},"
          "\"rendered-content\":\"<p>Hello</p>\"},"
          "{\"content\":\"Howdy\",\"media\":"
          "[{\"_id\":3,\"src\":\"http://test-cdn.4d4ms.com/blog/3.jpg\"},{\"_id\":4,\"src\":\"http://test-cdn.4d4ms.com/blog/4.jpg\"}],"
          "\"user\":{\"username\":\"Editor\",\"roles\":[\"editor\"]},"
          "\"rendered-content\":\"<p>Howdy</p>\"}]")})))

(deftest test-drafts
  (is (= (drafts fixtures/data-provider fixtures/user 1 10 nil) forbidden-result))
  (is (= (drafts fixtures/data-provider fixtures/user 1 10 "tag") forbidden-result))
  (is (not (nil? (drafts fixtures/data-provider fixtures/editor 1 10 nil))))
  )

(deftest test-post-article
  (is (= (post-article fixtures/data-provider fixtures/user {}) forbidden-result)))

(deftest test-update-article
  (is (= (update-article fixtures/data-provider fixtures/user {}) forbidden-result)))

(deftest test-post-media
  (is (= (post-media fixtures/data-provider fixtures/user {}) forbidden-result)))

(deftest test-list-users
  (is (= (list-users fixtures/data-provider fixtures/user 1  10) forbidden-result))
  (is (= (list-users fixtures/data-provider fixtures/editor 1 10) forbidden-result))
  (is (= (list-users fixtures/data-provider fixtures/admin 1 10)
         {:status 200,
          :headers {"Content-Type" "application/json"},
          :body "[{\"username\":\"User\",\"roles\":[]},{\"username\":\"Editor\",\"roles\":[\"editor\"]},{\"username\":\"Admin\",\"roles\":[\"admin\"]}]"})))
