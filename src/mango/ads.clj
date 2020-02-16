(ns mango.ads
  (:require [mango.config :as config]
            [hiccup.element :refer :all]))

(defn google
  []
  [:div {:align "center"}
   [:ins {:class "adsbygoogle"
          :style "display:block"
          :data-ad-client config/google-ad-client
          :data-ad-slot config/google-ad-slot
          :data-ad-format "auto"}]
   (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")])
