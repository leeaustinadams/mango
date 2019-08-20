(ns mango.article
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [mango.location :as location]
            [clojure.string :as str]))

(defn edit-article
  []
  (let [path (location/path)]
    (location/browse-to (str/replace-first path "/blog" "/blog/edit"))))

(defn new-article
  []
  (location/browse-to "/blog/new"))

(def keymap { "e" {:handler edit-article :desc "Edit this article"}
             "n" {:handler new-article :desc "Create a new article"}})

(defn ^:export on-load
  []
  (println "article on-load")
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  []
  (println "article on-unload")
  (bind/keymap (dom/body) nil))
