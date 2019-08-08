(ns mango.page
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [mango.location :as location]
            [clojure.string :as str]))

(defn edit-page
  []
  (let [path (location/path)]
    (location/browse-to (str/replace-first path "/pages" "/pages/edit"))))

(defn new-page
  []
  (location/browse-to "/pages/new"))

(def keymap {"e" {:handler edit-page :desc "Edit this page"}
             "n" {:handler new-page :desc "Create new page"}})

(defn ^:export on-load
  []
  (println "page on-load")

  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  []
  (println "page on-unload")
  (bind/keymap (dom/body) nil))
