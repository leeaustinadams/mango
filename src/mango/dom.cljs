(ns mango.dom
  (:require [clojure.string :as str]
            [markdown.core :refer [md->html]]
            [mango.xhr :as xhr]
            [oops.core :refer [oget oset! oset!+]]))

(defn document
  "Returns the document"
  []
  js/document)

(defn body
  "Returns the document body"
  []
  (oget (document) "body"))

(defn element-by-id
  ([id] (element-by-id (document) id))
  ([root id] (.getElementById root id)))

(defn elements-by-tag
  ([tag] (elements-by-tag (document) tag))
  ([root tag] (array-seq (.getElementsByTagName root tag))))

(defn elements-by-class
  ([class] (elements-by-class (document) class))
  ([root class] (array-seq (.getElementsByClassName root class))))

(defn- common-attributes
  "Sets common attributes from a map of attributes. Returns the element passed"
  [element {:keys [id class]}]
  (when id (oset! element "id" id))
  (when class (oset! element "className" class))
  element)

(defn script
  ([type src] (script type nil))
  ([type src attr]
   (let [element (.createElement (document) "script")]
     (oset! element "type" type)
     (oset! element "src" src)
     (common-attributes element attr))))

(defn js-script
  ([src] (js-script src nil))
  ([src attr]
   (script "text/javascript" src attr)))

(defn img
  [src & {:keys [alt] :as attr}]
  (let [element (.createElement (document) "img")]
    (oset! element "src" src)
    (when alt (oset! element "alt" alt))
    (common-attributes element attr)))

(defn p
  ([innerHTML] (p innerHTML nil))
  ([innerHTML attr]
   (let [element (.createElement (document) "p")]
     (when innerHTML (oset! element "innerHTML" innerHTML))
     (common-attributes element attr))))

(defn div
  [{:keys [id] :as attr}]
  (or (element-by-id id)
      (let [element (.createElement (document) "div")]
        (common-attributes element attr))))

(defn add-child
  [parent child]
  (.appendChild parent child))

(defn toggle-class
  "Adds class to the element's class names if not present, otherwise removes it"
  [element class]
  (let [classes (oget element "className")]
    (if (str/includes? classes class)
      (oset! element "className" (str/trim (str/replace classes class "")))
      (oset! element "className" (str classes " " class)))))

;; Events

(defn bind-event
  "Binds an event handler to an element by id"
  [id event handler]
  (oset!+ (element-by-id id) event handler))

;; Rendering functions

(defn markdown
  "Renders the markdown content to html"
  [content]
  (md->html content))

(defn highlight-code
  "Highlights code blocks"
  [root]
  (doseq [block (elements-by-tag root "code")]
    (.highlightBlock js/hljs block)))

(defn twemoji
  "Renders Twitter Emoji"
  [root]
  (.parse js/twemoji root))
