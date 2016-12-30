(ns mango.storage
  (:require [mango.config :as config]
            [amazonica.aws.s3 :as s3]))

(defn upload
  "Upload a file to storage"
  [name filename content-type]
  (future (s3/put-object config/aws-credentials :bucket-name config/aws-media-bucket :key (str "blog/" name) :file filename :meta-data {:content-type content-type})))
