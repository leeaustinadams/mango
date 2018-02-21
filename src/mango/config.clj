(ns mango.config
  (:require [config.core :refer [env]]
            [version]))

(def db-name (:db-name env))
(def db-article-collection (:db-article-collection env))
(def db-media-collection (:db-media-collection env))
(def db-users-collection (:db-users-collection env))
(def db-sessions-collection (:db-sessions-collection env))
(def db-log-collection (:db-log-collection env))
(def site-title (:site-title env))
(def site-description (:site-description env))
(def twitter-site-handle (:twitter-site-handle env))
(def twitter-creator-handle (:twitter-creator-handle env))
(def admin-email (:admin-email env))
(def version version/raw-version)
(def port (:port env))
(def cdn-url (:cdn-url env))
(def bot-user-agents '("Twitterbot" "facebookexternalhit/1.1" "Googlebot"))
(def aws-credentials {:access-key (:aws-access-key env)
                      :secret-key (:aws-secret-key env)
                      :endpoint (:aws-endpoint env)})
(def aws-media-bucket (:aws-media-bucket env))
(def aws-media-bucket-region (:aws-region env))
(def app-js (:app-js env))
(def app-css (:app-css env))
