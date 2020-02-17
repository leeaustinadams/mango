(ns mango.ads
  (:require [hiccup.element :refer :all]))

(defn google
  [ad-client ad-slot]
  [:div {:align "center"}
   [:ins {:class "adsbygoogle"
          :style "display:block"
          :data-ad-client ad-client
          :data-ad-slot ad-slot
          :data-ad-format "auto"}]
   (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")])
