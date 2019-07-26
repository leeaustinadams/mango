(ns mango.widgets
  (:require [mango.dom :refer [body div add-child]]))

(defn dialog
  [content]
  (let [dlg (div {:id "help-dlg" :class "dialog hidden"})]
    (set! (.-innerHTML dlg) content)
    (add-child (body) dlg)))
