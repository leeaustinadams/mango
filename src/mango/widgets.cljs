(ns mango.widgets
  (:require [mango.dom :refer [elements-by-class elements-by-tag add-child body div]]
            [mango.location :refer [browse-to]]
            [oops.core :refer [oget oset!]]))

(enable-console-print!)

(defn dialog
  [content]
  (let [dlg (div {:class "dialog hidden"})]
    (oset! dlg "innerHTML" content)
    (add-child (body) dlg)))

(defn bind-click
  [class]
  (doseq [item (elements-by-class class)]
    (let [link (first (elements-by-tag item "a"))
          dest (oget link "href")]
      (oset! item "onclick" (fn [] (browse-to dest))))))

(defn ^:export on-load
  []
  (bind-click "article-list-item")
  (bind-click "page-list-item"))
