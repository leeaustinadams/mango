(ns ^:figwheel-hooks  mango.core
  (:require [mango.media]
            [mango.xhr]
            [mango.dom :as dom]
            [mango.article]
            [mango.page]
            [mango.edit]))

(enable-console-print!)
;(set! *warn-on-infer* true)

(println "    __  ___
   /  |/  /___ _____  ____ _____
  / /|_/ / __ `/ __ \\/ __ `/ __ \\
 / /  / / /_/ / / / / /_/ / /_/ /
/_/  /_/\\__,_/_/ /_/\\__, /\\____/
                   /____/  v1.0")

(defn bind-twitter
  [id src]
  (let [fjs (first (array-seq (.getElementsByTagName js/document "script")))
        t (or (.-twttr js/window) (js-obj))]
    (if-not (dom/element-by-id id)
      (let [s (.createElement js/document "script")]
        (set! (.-id s) id)
        (set! (.-src s) src)
        (.insertBefore (.-parentNode fjs) s fjs)
        (set! (.-_e t) #js [])
        (set! (.-ready (fn [f] (.push (.-_e t) f))))
        t))))

(defn bind-pocket
  [id src]
  (let [existing (dom/element-by-id id)]
    (when (nil? existing)
      (let [s (.createElement js/document "script")]
        (set! (.-id s) id)
        (set! (.-src s) src)
        (.appendChild (.-body js/document) s)))))

;; Highlight code blocks
(.initHighlightingOnLoad js/hljs)

(defn bind-upload-form
  [upload-form]
  (let [file-select (dom/element-by-id "file-select")
        upload-status (dom/element-by-id "upload-status")
        anti-forgery (dom/element-by-id "anti-forgery-token")
        article-id (dom/element-by-id "article-id")
        file-upload (dom/element-by-id "file-upload")]
    (set! (.-onsubmit upload-form) (fn [event]
                                     (.preventDefault event)
                                     (set! (.-value file-upload) "Uploading...")
                                     (set! (.-innerHTML upload-status) "Uploading...")
                                     (let [form-data (js/FormData.)]
                                       (.append form-data "__anti-forgery-token" (.-value anti-forgery))
                                       (.append form-data "article-id" (.-value article-id))
                                       (doseq [file (array-seq (.-files file-select))]
                                         (.append form-data "files[]" file (.-name file)))
                                       (mango.xhr/send (.-method upload-form)
                                                       (.-action upload-form)
                                                       form-data
                                                       (fn [status message] (set! (.-innerHTML upload-status) message) (set! (.-value file-upload) "Upload"))
                                                       (fn [event] (set! (.-innerHTML upload-status) (.-loaded event)))))))))

(defn unbind-upload-form
  [upload-form]
  (set! (.-onsubmit upload-form) nil))

;; Has to occur before the DOMContentLoaded
(bind-twitter "twitter-wjs" "https://platform.twitter.com/widgets.js")

;; Has to happen after DOMContentLoaded
(defn bind
  []
  (when-let [upload-form (dom/element-by-id "upload-form")]
    (bind-upload-form upload-form))
  (bind-pocket "pocket-btn-js" "https://widgets.getpocket.com/v1/j/btn.js?v=1"))

(defn unbind
  []
  (when-let [upload-form (dom/element-by-id "upload-form")]
    (unbind-upload-form upload-form)))

;; Initialize
(.addEventListener js/window "DOMContentLoaded" bind)

;; For reloading
(defn ^:after-load setup []
  (bind))

(defn ^:before-load teardown []
  (unbind))
