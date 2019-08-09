(ns ^:figwheel-hooks mango.core
  (:require [mango.media]
            [mango.xhr]
            [mango.article]
            [mango.page]
            [mango.edit]
            [mango.dom :refer [body js-script element-by-id elements-by-tag]]
            [oops.core :refer [oget oset!]]))

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
  (let [fjs (first (elements-by-tag "script"))
        t (or (.-twttr js/window) (js-obj))]
    (if-not (element-by-id id)
      (let [s (js-script src {:id id})]
        (.insertBefore (oget fjs "parentNode") s fjs)
        (oset! t "!_e" #js [])
        (oset! t "!ready" (fn [f] (.push (oget t "_e") f))))
        t)))

(defn bind-pocket
  [id src]
  (let [existing (element-by-id id)]
    (when (nil? existing)
      (let [s (js-script src {:id id})]
        (.appendChild (body) s)))))

;; Highlight code blocks
(.initHighlightingOnLoad js/hljs)

(defn bind-upload-form
  [upload-form]
  (let [file-select (element-by-id "file-select")
        upload-status (element-by-id "upload-status")
        anti-forgery (element-by-id "anti-forgery-token")
        article-id (element-by-id "article-id")
        page-id (element-by-id "page-id")
        file-upload (element-by-id "file-upload")]
    (oset! upload-form "onsubmit" (fn [event]
                                    (.preventDefault event)
                                    (oset! file-upload "value" "Uploading...")
                                    (oset! upload-status "innerHTML" "Uploading...")
                                    (let [form-data (js/FormData.)]
                                      (.append form-data "__anti-forgery-token" (oget anti-forgery "value"))
                                      (when article-id (.append form-data "article-id" (oget article-id "value")))
                                      (when page-id (.append form-data "page-id" (oget page-id "value")))
                                      (doseq [file (array-seq (oget file-select "files"))]
                                        (.append form-data "files[]" file (oget file "name")))
                                      (mango.xhr/send (oget upload-form "method")
                                                      (oget upload-form "action")
                                                      form-data
                                                      (fn [status message]
                                                        (oset! upload-status "innerHTML" message)
                                                        (oset! file-upload "value" "Upload"))
                                                      (fn [event] (oset! upload-status "innerHTML" (oget event "loaded")))))))))

(defn unbind-upload-form
  [upload-form]
  (oset! upload-form "onsubmit" nil))

;; Has to occur before the DOMContentLoaded
(bind-twitter "twitter-wjs" "https://platform.twitter.com/widgets.js")

;; Has to happen after DOMContentLoaded
(defn bind
  []
  (when-let [upload-form (element-by-id "upload-form")]
    (bind-upload-form upload-form))
  (bind-pocket "pocket-btn-js" "https://widgets.getpocket.com/v1/j/btn.js?v=1"))

(defn unbind
  []
  (when-let [upload-form (element-by-id "upload-form")]
    (unbind-upload-form upload-form)))

;; Initialize
(.addEventListener js/window "DOMContentLoaded" bind)

;; For reloading
(defn ^:after-load setup []
  (bind))

(defn ^:before-load teardown []
  (unbind))
