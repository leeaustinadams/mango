(ns mango.widgets
  (:require [mango.dom :refer [add-child body div]]
            [oops.core :refer [oset!]]))

(defn dialog
  [content]
  (let [dlg (div {:id "help-dlg" :class "dialog hidden"})]
    (oset! dlg "innerHTML" content)
    (add-child (body) dlg)))
