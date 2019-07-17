(ns mango.xhr)

(defn send
  [method url data & [onload onprogress]]
  (let [xhr (js/XMLHttpRequest.)]
    (when onload
      (set! (.-onload xhr) (fn [event] (onload (.-status xhr) (.-responseText xhr)))))
    (when onprogress
      (set! (.-onprogress xhr) onprogress))
    (.open xhr method url true)
    (.send xhr data)))
