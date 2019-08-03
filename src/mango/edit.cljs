(ns mango.edit
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [oops.core :refer [oget oset!]]))

(defn toggle-preview
  [event]
  (let [element (dom/element-by-id "content")
        preview (dom/element-by-id "preview")
        value (oget element "value")]
    (.preventDefault event)
    (oset! preview "innerHTML" (dom/markdown value))
    (dom/toggle-class preview "hidden")))

(def keymap {"p" {:handler toggle-preview :desc "Toggle preview"}})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap)
  (oset! (dom/element-by-id "preview-button") "onclick" toggle-preview))

(defn ^:export on-unload
  [])
