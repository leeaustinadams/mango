(ns mango.routes-test
  (:require [clojure.test :refer :all]
            [mango.routes :refer :all]))

(def json-headers {"Content-Type" "application/json"})

(deftest test-json-success
  (is (= (json-success nil) {:status 200 :headers json-headers :body "null"}))
  (is (= (json-success {:foo "bar"}) {:status 200 :headers json-headers :body "{\"foo\":\"bar\"}"}))
  (is (= (json-success {:foo "bar"} {:other "thing"}) {:status 200 :headers json-headers :body "{\"foo\":\"bar\"}" :other "thing"})))

(deftest test-json-failure
  (is (= (json-failure 404 {:message "not found"}) {:status 404 :headers json-headers :body "{\"message\":\"not found\"}"}))
  (is (= (json-failure 401 {:message "unauthorized"} {:other "thing"}) {:status 401 :headers json-headers :body "{\"message\":\"unauthorized\"}" :other "thing"})))

(def html-headers {"Content-Type" "text/html"})

(deftest test-html-success
  (is (= (html-success "") {:status 200 :headers html-headers :body ""}))
  (is (= (html-success "<p>foo</p>") {:status 200 :headers html-headers :body "<p>foo</p>"})))

(deftest test-crawler-article-response
  (is (not (nil? (crawler-article-response "article" "Twitterbot" "http://article"))))
  (is (not (nil? (crawler-article-response "article" "facebookexternalhit/1.1" "http://article"))))
  (is (not (nil? (crawler-article-response "article" "Googlebot" "http://article"))))
  (is (nil? (crawler-article-response "article" "Chrome" "http://article")))
  (is (nil? (crawler-article-response "article" "" "http://article"))))
