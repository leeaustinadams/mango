(ns mango.bind
  (:require [mango.dom :as dom]))

(enable-console-print!)

(defn keymap
  [element keymap]
  (set! (.-onkeypress element)
        (fn [event]
          (println (.-key event))
          (when-let [handler (get keymap (.-key event))]
            (handler event)))))
