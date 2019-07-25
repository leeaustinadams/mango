(ns mango.article
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [clojure.string :as str]))

(defn edit-article
  []
  (let [path (.-pathname js/location)]
    (.assign js/location (str/replace path "/blog" "/blog/edit"))))

(def keymap { "e" edit-article})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap))

(defn ^:export on-unload
  [])
