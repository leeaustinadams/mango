(ns ^:figwheel-hooks mango.edit-article
  (:require [mango.dom :as dom]
            [mango.bind :as bind]))

(defn toggle-preview
  [event]
  (let [element (dom/element-by-id "content")
        preview (dom/element-by-id "preview")
        value (.-value element)]
    (.preventDefault event)
    (set! (.-innerHTML preview) (dom/markdown value))
    (dom/toggle-class preview "hidden")))

(def keymap {"p" toggle-preview
             "?" (fn [] (.log js/console "help"))})

(defn ^:export on-load
  []
  (bind/keymap (dom/body) keymap)
  (set! (.-onclick (dom/element-by-id "preview-button")) toggle-preview))

(defn ^:export on-unload
  [])
