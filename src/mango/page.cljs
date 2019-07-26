(ns mango.page
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [mango.location :as location]
            [clojure.string :as str]))

(defn edit-page
  []
  (let [path (location/path)]
    (location/browse-to (str/replace-first path "/pages" "/pages/edit"))))

(def keymap { "e" edit-page})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  [])
