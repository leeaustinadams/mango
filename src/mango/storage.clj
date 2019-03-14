;; https://github.com/mcohen01/amazonica
;; https://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectOps.html
(ns mango.storage
  (:require [mango.config :as config]
            [amazonica.aws.s3 :as s3]
            [taoensso.timbre :as timbre :refer [fatal trace debugf warn]])
  (:import [com.amazonaws.services.s3.model AmazonS3Exception CannedAccessControlList Region]))

(defn init-bucket
  [remote-bucket]
  (try
    (when-not (some #(= remote-bucket %) (map :name (s3/list-buckets)))
      (debugf "Creating bucket %s" remote-bucket)
      (s3/create-bucket remote-bucket (. Region fromValue config/aws-media-bucket-region)))
    (catch AmazonS3Exception e (fatal e))))

(defn upload
  "Upload a file to storage. Returns a future"
  [remote-bucket remote-name local-name content-type]
  (future
    (init-bucket remote-bucket)
    (debugf "Putting %s" local-name)
    (try
      (s3/put-object :bucket-name remote-bucket
                     :key remote-name
                     :file local-name
                     :meta-data {:content-type content-type})
      (debugf "Setting permissions %s" remote-name)
      (s3/set-object-acl remote-bucket remote-name (. CannedAccessControlList PublicRead))
      (debugf "Done")
      (catch AmazonS3Exception e (warn e)))))

(defn delete
  "Delete a file in storage. Returns a future"
  [remote-bucket remote-name]
  (future
    (debugf "Deleting %s/%s" remote-bucket remote-name)
    (try
      (s3/delete-object :bucket-name remote-bucket
                        :key remote-name)
      (catch AmazonS3Exception e (warn e)))))
