;; https://github.com/mcohen01/amazonica
;; https://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectOps.html
(ns mango.storage
  (:require [mango.config :as config]
            [amazonica.aws.s3 :as s3])
  (:import com.amazonaws.services.s3.model.CannedAccessControlList)
  (:import com.amazonaws.services.s3.model.Region))

(defn upload
  "Upload a file to storage. Returns a future"
  [remote-bucket remote-name local-name content-type]
  (future
    (when-not (some #(= remote-bucket %) (map :name (s3/list-buckets)))
      (println (str "Creating bucket " remote-bucket))
      (s3/create-bucket remote-bucket (. Region fromValue config/aws-media-bucket-region)))
    (println (str "Putting " local-name))
    (s3/put-object :bucket-name remote-bucket
                   :key remote-name
                   :file local-name
                   :meta-data {:content-type content-type})
    (println (str "Setting permissions " remote-name))
    (s3/set-object-acl remote-bucket remote-name (. CannedAccessControlList PublicRead))
    (println "Done")))

(defn delete
  "Delete a file in storage. Returns a future"
  [remote-bucket remote-name]
  (future
    (println (str "Deleting " remote-bucket "/" remote-name))
    (s3/delete-object :bucket-name remote-bucket
                      :key remote-name)))
