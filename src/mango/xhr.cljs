(ns mango.xhr
  (:require [oops.core :refer [oget oset!]]))

(defn send
  [method url data & [onload onprogress]]
  (let [xhr (js/XMLHttpRequest.)]
    (when onload
      (oset! xhr "onload" (fn [event] (onload (oget xhr "status") (oget xhr "responseText")))))
    (when onprogress
      (oset! xhr "onprogress" onprogress))
    (.open xhr method url true)
    (.send xhr data)))

(defn recv
  [url & [onload onprogress]]
  (let [xhr (js/XMLHttpRequest.)]
    (when onload
      (oset! xhr "onload" (fn [event] (onload (oget xhr "status") (oget xhr "responseText")))))
    (when onprogress
      (oset! xhr "onprogress" onprogress))
    (.open xhr "GET" url true)
    (.send xhr nil)))
