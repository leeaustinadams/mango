(ns mango.pages-test
  (:require [clojure.test :refer :all]
            [mango.config :as config]
            [mango.pages :refer [render-page-meta]]
            [mango.fixtures :as fixtures]))

(deftest test-render-page-meta
  (let [options {:url (str config/site-url "/blog/first")
                 :title "A title"
                 :description "A description"
                 :image-url "https://cdn/animage.png"
                 :og-type "article"
                 :robots "noindex"
                 :keywords '["some" "key" "words" "and phrases"]}
        result (render-page-meta options)]
    (testing "og:title"
      (is (some #(= % [:meta {:property "og:title", :content "A title"}]) result)))
    (testing "og:type"
      (is (some #(= % [:meta {:property "og:type", :content "article"}]) result)))
    (testing "og:url"
      (is (some #(= % [:meta {:property "og:url", :content "https://yourdomain.com/blog/first"}]) result)))
    (testing "description"
      (is (some #(= % [:meta {:name "description", :content "A description"}]) result)))
    (testing "robots"
      (is (some #(= % [:meta {:name "robots", :content "noindex"}]) result)))
    (testing "keywords"
      (is (some #(= % [:meta {:name "keywords", :content "some,key,words,and phrases"}]) result)))))
