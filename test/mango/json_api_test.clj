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
