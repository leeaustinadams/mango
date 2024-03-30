(ns ^:figwheel-hooks mango.edit
  (:require [mango.dom :as dom]
            [mango.bind :as bind]
            [clojure.string :as str]
            [oops.core :refer [oget oset!]]))

(def emoji-map (atom {}))

(defn- fetch-emoji-map []
  (mango.xhr/recv "/emoji.json"
                  (fn [status responseText]
                    (swap! emoji-map (fn [a]
                                       (let [emoji (js->clj (.parse js/JSON responseText))]
                                         (reduce (fn [coll p] (assoc coll (get p "name") (get p "char"))) {} emoji)))))))

(def emoji-replace-pattern
  "e.g. :grinning face:"
  #":([^:]*):")

(defn replace-emoji
  "Given a string, replaces the occurences of emoji sequences
  (see emoji-replace-pattern) with the emoji they refer to if found.
  Returns a vector of the modified string and the difference in length between
  the original and new strings."
  [s]
  (let [after (str/replace s emoji-replace-pattern #(or (get @emoji-map (second %)) (first %)))]
    [after (- (.-length s) (.-length after))]))

(defn toggle-preview
  [event]
  (let [element (dom/element-by-id "content")
        preview (dom/element-by-id "preview")
        value (oget element "value")]
    (.preventDefault event)
    (oset! preview "innerHTML" (dom/markdown (first (replace-emoji value))))
    (dom/highlight-code preview)
    (dom/toggle-class preview "hidden")))

(defn toggle-syntax
  [event]
  (let [element (dom/element-by-id "content")
        syntax (dom/element-by-id "syntax")]
    (.preventDefault event)
    (dom/toggle-class syntax "hidden")))

(defn on-content-input
  [event]
  (let [target (oget event "target")
        text (oget target "value")
        start (oget target "selectionStart")
        [replaced diff] (replace-emoji text)]
    (oset! target "value" replaced)
    (.setSelectionRange target (- start diff) (- start diff))))

(defn bind
  []
  (fetch-emoji-map)
  (dom/bind-event "syntax-button" "onclick" toggle-syntax)
  (dom/bind-event "syntax" "ondblclick" toggle-syntax)
  (dom/bind-event "preview-button" "onclick" toggle-preview)
  (dom/bind-event "preview" "ondblclick" toggle-preview)
  (dom/bind-event "content" "oninput" on-content-input)
  (dom/bind-event "title" "oninput" on-content-input)
  (dom/bind-event "description" "oninput" on-content-input))

(defn unbind
  []
  (dom/bind-event "syntax-button" "onclick" nil)
  (dom/bind-event "syntax" "ondblclick" nil)
  (dom/bind-event "preview-button" "onclick" nil)
  (dom/bind-event "preview" "onclick" nil)
  (dom/bind-event "content" "oninput" nil)
  (dom/bind-event "title" "oninput" nil)
  (dom/bind-event "description" "oninput" nil))

(defn ^:export on-load [] (bind))

(defn ^:export on-unload [] (unbind))

;; For reloading
(defn ^:after-load setup []
  (bind))

(defn ^:before-load teardown []
  (unbind))
