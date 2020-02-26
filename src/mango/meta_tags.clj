(ns mango.meta-tags
  (:require [mango.config :as config]))

(defn wrap-default
  [meta-tags]
  (fn [{:keys [description robots] :as options}]
    (conj (meta-tags options)
          (when robots [:meta {:name "robots" :content robots}])
          [:meta {:name "description" :content description}])))

(defn wrap-twitter
  [meta-tags]
  (fn [{:keys [twitter-card twitter-handle title image-url description] :as options}]
    (conj (meta-tags options)
          [:meta {:name "twitter:card" :content twitter-card}]
          [:meta {:name "twitter:site" :content twitter-handle}]
          [:meta {:name "twitter:title" :content title}]
          [:meta {:name "twitter:image" :content (or image-url config/logo-url)}]
          [:meta {:name "twitter:description" :content description}])))

(defn wrap-og
  [meta-tags]
  (fn [{:keys [og-type url title description image-url] :as options}]
    (conj (meta-tags options)
          [:meta {:property "og:url" :content url}]
          [:meta {:property "og:type" :content og-type}]
          [:meta {:property "og:title" :content title}]
          [:meta {:property "og:description" :content description}]
          [:meta {:property "og:image" :content (or image-url config/logo-url)}])))
