(ns mango.dom
  (:require [clojure.string :as str]
            [markdown.core :refer [md->html]]))

(defn element-by-id
  ([id] (element-by-id js/document id))
  ([root id] (.getElementById root id)))

(defn elements-by-tag
  ([tag] (elements-by-tag js/document tag))
  ([root tag] (array-seq (.getElementsByTagName root tag))))

(defn elements-by-class
  ([class] (elements-by-class js/document class))
  ([root class] (array-seq (.getElementsByClassName root class))))

(defn- common-attributes
  [element {:keys [id class]}]
  (when id (set! (.-id element) id))
  (when class (set! (.-className element) class))
  element)

(defn img
  [src & {:keys [alt] :as attr}]
  (let [element (.createElement js/document "img")]
    (set! (.-src element) src)
    (when alt (set! (.-alt element) alt))
    (common-attributes element attr)))

(defn p
  [innerHTML & attr]
  (let [element (.createElement js/document "p")]
    (when innerHTML (set! (.-innerHTML element) innerHTML))
    (common-attributes element attr)))

(defn div
  [{:keys [id] :as attr}]
  (or (element-by-id id)
      (let [element (.createElement js/document "div")]
        (common-attributes element attr))))

(defn add-child
  [parent child]
  (.appendChild parent child))

(defn toggle-class
  "Adds class to the element's class names if not present, otherwise removes it"
  [element class]
  (let [classes (.-className element)]
    (if (str/includes? classes class)
      (set! (.-className element) (str/trim (str/replace classes class "")))
      (set! (.-className element) (str classes " " class)))))

(defn document
  "Returned the document"
  []
  js/document)

(defn body
  "Returns the document body"
  []
  (.-body (document)))

(defn markdown
  "Renders the markdown content to html"
  [content]
  (md->html content))
