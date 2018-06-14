(ns ^:integration mango.db-test
  (:require [clojure.test :refer :all]
            [monger.db :as mdb]
            [mango.db :as db]
            [mango.fixtures :as fixtures]
            [mango.config :as config]))

(defn db-fixture [f]
  (println "Testing DB on " config/db-name)
  (db/init)
  (f)
  (mdb/drop-db @db/DB)
  (db/terminate)
)

(use-fixtures :once db-fixture)

(deftest test-blog-articles-count
  (is (= (db/blog-articles-count "published" fixtures/user) 0)))

(deftest test-blog-drafts-count
  (is (= (db/blog-articles-count "draft" fixtures/user) 0)))

(def test-keys [:status :title :description :tags :user])

(deftest test-insert-blog-article
  (is (= (select-keys (db/insert-blog-article {:status "published" :title "Title" :description "Description" :tags ["a" "b"]} (:_id fixtures/user)) test-keys)
         {:status "published" :title "Title" :description "Description" :tags ["a" "b"] :user (:_id fixtures/user)}))
  (is (= (db/blog-articles-count "published" fixtures/user) 1))
  (is (= (map #(select-keys % test-keys) (db/blog-articles "published" {:page 1 :per-page 1 :tagged "a"}))
         [{:status "published" :title "Title" :description "Description" :tags ["a" "b"] :user (:_id fixtures/user)}])))

(deftest test-blog-media-by-ids
  (is (= (db/blog-media-by-ids nil) nil))
  (is (= (db/blog-media-by-ids {}) nil)))

(deftest test-user-by-id
  (let [test-user (db/insert-user "tester" "Bob" "Testerson" "Bob Testerson" "bob@test.org" "@testersontweets" "alphabravo" [])]
    (is (= (dissoc test-user :_id)
         {:username "tester"
          :first-name "Bob"
          :last-name "Testerson"
          :display-name "Bob Testerson"
          :email "bob@test.org"
          :twitter-handle "@testersontweets"
          :password "alphabravo"
          :roles []}))
    (is (= (db/user-by-id (:_id test-user)) test-user))
    (is (= (db/user-by-id (.toString (:_id test-user))) test-user))))
