(ns mango.article
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [mango.location :as location]
            [clojure.string :as str]))

(defn edit-article
  []
  (let [path (location/path)]
    (location/browse-to (str/replace-first path "/blog" "/blog/edit"))))

(def keymap { "e" {:handler edit-article :desc "Edit this article"}})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  [])
