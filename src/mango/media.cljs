(ns mango.media)

(enable-console-print!)
(defn ^:export allowMediaDrop
  [event]
  (.preventDefault event))

(defn ^:export mediaDragStart
  [event]
  (.setData (.-dataTransfer event) "src" (.getAttribute (.-target event) "src")))

(defn ^:export mediaDrop
  [event]
  (.preventDefault event)
  (let [data (str "![](" (.getData (.-dataTransfer event) "src") ")")
        target (.-target event)
        value (.-value target)
        start (.-selectionStart target)
        end (.-selectionEnd target)]
    (set! (.-value (.-target event)) (str value "\n" data))))
