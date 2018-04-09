(ns mango.routes-test
  (:require [clojure.test :refer :all]
            [mango.fixtures :as fixtures]
            [mango.routes :refer :all]))

(def html-headers {"Content-Type" "text/html"})

(deftest test-html-success
  (is (= (html-success "") {:status 200 :headers html-headers :body ""}))
  (is (= (html-success "<p>foo</p>") {:status 200 :headers html-headers :body "<p>foo</p>"})))

(deftest test-sitemap
  (is (not (nil? (sitemap fixtures/data-provider)))))

(deftest test-post-article
  (is (= (post-article fixtures/data-provider fixtures/user {:title "foo"})
         {:status 302, :headers {"Content-Type" "text/html", "Location" "/blog/foo"}})))

(deftest test-update-article
  (is (= (update-article fixtures/data-provider fixtures/user {:title "foo"})
         {:status 302, :headers {"Content-Type" "text/html", "Location" "/blog/foo"}})))

;(deftest test-post-media
;  (is (= (post-media fixtures/data-provider fixtures/user {}) fixtures/forbidden-result)))
