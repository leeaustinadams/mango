(ns mango.bind
  (:require [mango.dom :as dom]
            [mango.widgets :refer [dialog]]
            [clojure.string :refer [join]]))

(enable-console-print!)

(defn build-help-item
  [item]
  (str "**" (first item) "** - " (-> item second :desc) "  \n"))

(defn build-help
  [keymap]
  (dom/markdown (join (for [item (map build-help-item keymap)] item))))

(def help (memoize build-help))
(def help-item {"?" {:desc "Show/hide help"}})

(defn show-help
  [keymap]
  (dom/toggle-class (dialog (help (merge keymap help-item))) "hidden"))

(defn keymap
  [element keymap]
  (set! (.-onkeypress element)
        (fn [event]
          (let [k (.-key event)]
            (if (= "?" k)
              (show-help keymap)
              (when-let [handler (:handler (get keymap k))]
                (handler event)))))))
