(ns mango.page
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [clojure.string :as str]))

(defn edit-page
  []
  (let [path (.-pathname js/location)]
    (.assign js/location (str/replace path "/pages" "/pages/edit"))))

(def keymap { "e" edit-page})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  [])
