(ns mango.config
  (:require [config.core :refer [env]]
            [version]))

(def db-name (:db-name env))
(def db-article-collection (:db-article-collection env))
(def db-page-collection (:db-page-collection env))
(def db-media-collection (:db-media-collection env))
(def db-users-collection (:db-users-collection env))
(def db-sessions-collection (:db-sessions-collection env))
(def db-log-collection (:db-log-collection env))
(def session-key (:session-key env))
(def site-url (:site-url env))
(def site-title (:site-title env))
(def site-description (:site-description env))
(def site-copyright (:site-copyright env))
(def twitter-site-handle (:twitter-site-handle env))
(def twitter-creator-handle (:twitter-creator-handle env))
(def admin-email (:admin-email env))
(def version version/raw-version)
(def port (:port env))
(def cdn-url (:cdn-url env))
(def logo-url (:logo-url env))
(def aws-credentials {:access-key (:aws-access-key env)
                      :secret-key (:aws-secret-key env)
                      :endpoint (:aws-endpoint env)})
(def aws-media-bucket (:aws-media-bucket env))
(def aws-media-bucket-region (:aws-region env))
(def app-js (str "js/mango-" version/version ".min.js"))
(def app-css (str "css/mango-" version/version ".min.css"))
(def ads-enabled (:ads-enabled env))
(def google-analytics-id (:google-analytics-id env))
