(ns ^:figwheel-hooks mango.edit
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

(defn toggle-syntax
  [event]
  (let [element (dom/element-by-id "content")
        syntax (dom/element-by-id "syntax")]
    (.preventDefault event)
    (dom/toggle-class syntax "hidden")))

(defn bind
  []
  (oset! (dom/element-by-id "syntax-button") "onclick" toggle-syntax)
  (oset! (dom/element-by-id "syntax") "ondblclick" toggle-syntax)
  (oset! (dom/element-by-id "preview-button") "onclick" toggle-preview)
  (oset! (dom/element-by-id "preview") "ondblclick" toggle-preview))

(defn unbind
  []
  (oset! (dom/element-by-id "syntax-button") "onclick" nil)
  (oset! (dom/element-by-id "syntax") "ondblclick" nil)
  (oset! (dom/element-by-id "preview-button") "onclick" nil)
  (oset! (dom/element-by-id "preview") "onclick" nil))

(defn ^:export on-load [] (bind))

(defn ^:export on-unload [] (unbind))

;; For reloading
(defn ^:after-load setup []
  (bind))

(defn ^:before-load teardown []
  (unbind))
