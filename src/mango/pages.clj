;; https://github.com/davidsantiago/stencil
(ns mango.pages
(:require [stencil.core :as stencil]))

(defn not-found
  "Render a page for when a URI is not found"
  []
  (stencil/render-file "templates/simple_error.html"
                       { :title "The page you are looking for could not be found."}))

(defn index
  "Render the root html"
  []
  (stencil/render-file "templates/index.html"
                       {
                        :title "Lee Austin Adams"
                        :description "Lee's Website"
                        :adminEmail "admin@4d4ms.com"
                        :version "0.1"}))

