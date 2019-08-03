(ns mango.media
  (:require [oops.core :refer [oget oset!]]))

(enable-console-print!)
(defn ^:export allowMediaDrop
  [event]
  (.preventDefault event))

(defn ^:export mediaDragStart
  [event]
  (let [dataTransfer (oget event "dataTransfer")
        target (oget event "target")]
    (.setData dataTransfer "src" (.getAttribute target "src"))))

(defn ^:export mediaDrop
  [event]
  (.preventDefault event)
  (let [dataTransfer (oget event "dataTransfer")
        data (str "![](" (.getData dataTransfer "src") ")")
        target (oget event "target")
        value (oget target "value")
        start (oget target "selectionStart")
        end (oget target "selectionEnd")]
    (oset! target "value" (str value "\n" data))))
