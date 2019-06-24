(ns mango.ads
  (:require [mango.config :as config]
            [hiccup.element :refer :all]))

(defn google
  []
  [:div {:align "center"}
   [:ins {:class "adsbygoogle"
          :style "display:block"
          :data-ad-client "ca-pub-8004927313720287"
          :data-ad-slot "5968146561"
          :data-ad-format "auto"}]
   (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")])
