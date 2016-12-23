(ns mango.config
  (:require [environ.core :refer [env]]))

(def db-name "4d4ms-mean")
(def db-article-collection "blogarticles")
(def db-media-collection "blogmedia")
(def db-users-collection "users")
(def db-sessions-collection "sessions")
(def site-title "Lee Austin Adams")
(def site-description "Lee's Website")
(def twitter-site-handle "@beamjack")
(def twitter-creator-handle "@beamjack")
(def admin-email "admin@4d4ms.com")
(def version (:mango-version env))
(def port 8080)
(def aws-credentials {:access-key "AKIAJBCNLKTIY3RNQUKA"
                      :secret-key "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"
                      :endpoint "us-west-1"})
(def media-bucket "4d4ms")
