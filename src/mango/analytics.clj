(ns mango.analytics
  (:require [mango.config :as config]
            [hiccup.element :refer [javascript-tag]]))

(defn google [id]
  (javascript-tag (format "<!-- Global site tag (gtag.js) - Google Analytics -->
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());

  gtag('config', '%s');" id)))
