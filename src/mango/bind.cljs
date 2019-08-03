(ns mango.bind
  (:require [mango.dom :as dom]
            [mango.widgets :refer [dialog]]
            [clojure.string :refer [join]]
            [oops.core :refer [oget+ oset!]]))

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
  (oset! element
         "onkeypress"
         (fn [event]
           (let [k (oget+ event "key")]
             (if (= "?" k)
               (show-help keymap)
               (when-let [handler (:handler (get keymap k))]
                 (handler event)))))))
