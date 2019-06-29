(ns ^:figwheel-hooks  mango.core
  (:require [mango.media] [mango.xhr]))

(enable-console-print!)
;(set! *warn-on-infer* true)

(println "    __  ___
   /  |/  /___ _____  ____ _____
  / /|_/ / __ `/ __ \\/ __ `/ __ \\
 / /  / / /_/ / / / / /_/ / /_/ /
/_/  /_/\\__,_/_/ /_/\\__, /\\____/
                   /____/  v1.0")

;; Initialize Twitter Widgets
(let [fjs (first (array-seq (.getElementsByTagName js/document "script")))
      t (or (.-twttr js/window) (js-obj))]
  (if-not (.getElementById js/document "twitter-wjs")
    (let [s (.createElement js/document "script")]
      (set! (.-id s) "twitter-wjs")
      (set! (.-src s) "https://platform.twitter.com/widgets.js")
      (.insertBefore (.-parentNode fjs) s fjs)
      (set! (.-_e t) #js [])
      (set! (.-ready (fn [f] (.push (.-_e t) f))))
    t)))

;; Highlight code blocks
(.initHighlightingOnLoad js/hljs)

(defn bind-upload-form
  [upload-form]
  (println "Binding upload form")
  (let [file-select (.getElementById js/document "file-select")
        upload-status (.getElementById js/document "upload-status")
        anti-forgery (.getElementById js/document "anti-forgery-token")
        article-id (.getElementById js/document "article-id")
        file-upload (.getElementById js/document "file-upload")]
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

(defn bind
  []
  (when-let [upload-form (.getElementById js/document "upload-form")]
    (bind-upload-form upload-form)))

(defn unbind
  []
  (when-let [upload-form (.getElementById js/document "upload-form")]
    (unbind-upload-form upload-form)))

(.addEventListener js/window "DOMContentLoaded" bind)

(defn ^:after-load setup []
  (println "setup")
  (bind))

(defn ^:before-load teardown []
  (println "teardown")
  (unbind))
