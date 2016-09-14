(ns mango.dehydrate
  (:require [mango.db :as db])
  (:gen-class))

(defn media
  "Dehydrates the media collection of x"
  [x]
  (assoc x :media (loop [media (:media x)
                         acc '()]
                    (let [item (first media)]
                      (if (nil? item)
                        acc
                        (recur (rest media) (conj acc (:_id item))))))))
