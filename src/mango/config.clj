(ns mango.config
  (:require [config.core :refer [env]]
            [version]))

(def db-address (or (:db-address env) "127.0.0.1"))
(def db-user (or (:db-user env) "mango"))
(def db-password (or (:db-password env) "mango"))
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
(def site-language (or (:site-language env) "en-US"))
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
(def app-js (or (:app-js env) (str "js/mango-" version/version ".min.js")))
(def app-css (or (:app-css env) "/css/mango.css"))
(def ads-enabled (:ads-enabled env))
(def header-ads-enabled (:header-ads-enabled env))
(def footer-ads-enabled (:footer-ads-enabled env))
(def analytics-enabled (:analytics-enabled env))
(def google-analytics-id (:google-analytics-id env))
(def google-ad-client (:google-ad-client env))
(def google-ad-slot (:google-ad-slot env))
