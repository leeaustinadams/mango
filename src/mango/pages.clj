;; https://github.com/weavejester/hiccup
;; https://github.com/davidsantiago/stencil
(ns mango.pages
(:require [hiccup.core :refer :all]
          [hiccup.form :as form]
          [hiccup.page :only html5]
          [stencil.core :as stencil]))

(defn not-allowed
  "Render a page for 403 responses"
  []
  (hiccup.page/html5 (html [:h1 "Not Allowed"])))

(defn sign-in
  "Render a page with a a sign in form"
  []
  (hiccup.page/html5 (html 
                      [:h1 "Sign In!"]
                      (form/form-to [:post "/auth/signin"]
                                    [:div {:class "form-group"}
                                     (form/label {:class "control-label"} "username" "User Name")
                                     (form/text-field {:class "form-control" :placeholder "username"} "username")]
                                    [:div {:class "form-group"}
                                     (form/label {:class "control-label"} "password" "Password")
                                     (form/password-field {:class "form-control" :placeholder "Password" :ng-model "password"} "password")]
                                    (form/submit-button "log in")))))

(defn sign-out
  "Render a page to allow users to sign out"
  []
  (html
   [:h1 "Sign Out!"]
   (form/form-to [:post "/auth/signout"]
                 (form/submit-button "log out"))))

(defn signed-out
  "Render a page indicating the user has been signed out"
  [username]
  (html [:div [:h1 "Logged Out"] (when username [:p username])]))

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
                        :title "Mango"
                        :description "Mango Website"
                        :adminEmail "admin@4d4ms.com"
                        :version "0.1"}))

