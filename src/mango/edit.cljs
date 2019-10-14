(ns ^:figwheel-hooks mango.edit
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [clojure.string :as str]
            [oops.core :refer [oget oset!]]))

(def emoji-map (atom {}))

(defn fetch-emoji-map []
  (mango.xhr/recv "/emoji.json"
                  (fn [status responseText]
                    (swap! emoji-map (fn [a]
                                       (let [emoji (js->clj (.parse js/JSON responseText))]
                                         (reduce (fn [coll p] (assoc coll (get p "name") (get p "char"))) {} emoji)))))))

(def emoji-replace-pattern #":([^:]*):")

(defn replace-emoji
  [s]
  (str/replace s emoji-replace-pattern #(or (get @emoji-map (second %)) (first %))))

(defn toggle-preview
  [event]
  (let [element (dom/element-by-id "content")
        preview (dom/element-by-id "preview")
        value (oget element "value")]
    (.preventDefault event)
    (oset! preview "innerHTML" (dom/markdown (replace-emoji value)))
    (dom/twemoji preview)
    (dom/highlight-code preview)
    (dom/toggle-class preview "hidden")))

(defn toggle-syntax
  [event]
  (let [element (dom/element-by-id "content")
        syntax (dom/element-by-id "syntax")]
    (.preventDefault event)
    (dom/toggle-class syntax "hidden")))

(defn on-content-input
  [event]
  (let [textarea (oget event "target")
        text (oget textarea "value")]
    (oset! textarea "value" (replace-emoji text))
    (dom/twemoji textarea)))

(defn bind
  []
  (fetch-emoji-map)
  (oset! (dom/element-by-id "syntax-button") "onclick" toggle-syntax)
  (oset! (dom/element-by-id "syntax") "ondblclick" toggle-syntax)
  (oset! (dom/element-by-id "preview-button") "onclick" toggle-preview)
  (oset! (dom/element-by-id "preview") "ondblclick" toggle-preview)
  (oset! (dom/element-by-id "content") "oninput" on-content-input))

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
