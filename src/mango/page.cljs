(ns mango.page
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [mango.location :as location]
            [clojure.string :as str]))

(defn edit-page
  []
  (let [path (location/path)]
    (location/browse-to (str/replace-first path "/pages" "/pages/edit"))))

(def keymap { "e" {:handler edit-page :desc "Edit this page"}})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  [])
