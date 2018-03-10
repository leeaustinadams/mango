;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [csv xform-time-to-string xform-string-to-time]]
            [clojure.data.json :as json]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]
            ))

(defn header
  "Render the header"
  [title]
  (list
   [:title title]
   [:meta {:charset="utf-8"}]
   [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
   [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
   (include-css "/css/styles/github-gist.css")
   (include-css (str "/" config/app-css))
   (include-js "/js/highlight.pack.js")
   (include-js "https://www.googletagmanager.com/gtag/js?id=UA-43198629-1")
   (include-js "//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")
   (include-js (str "/" config/app-js))))

(defn footer
  "Render the footer"
  []
  [:div {:class "footer" :align "center"}
   [:small {:class "copyright"} "&copy; 2014-2018 Lee Adams " (mail-to config/admin-email "contact")][:br]
   [:small {class "version"} config/version]])

(defn toolbar
  "Render the toolbar"
  [user & [article]]
  [:div {:class "header"}
   (unordered-list (list (link-to "/" "Home")
                         (link-to "/blog" "Blog")
                         (when (auth/editor? user) (link-to "/blog/drafts" "Drafts"))
                         (when (and article (auth/editor? user)) (link-to (str "/edit/" (:slug article)) "Edit"))
                         (if user
                           (link-to "/signout" "Sign out")
                           (link-to "/signin" "Sign in"))))])

(defn tags
  "Render tags"
  [tags]
  (unordered-list {:class "tags"}
                  (map (fn [tag]
                         (list
                          (link-to (str "/blog/tagged/" (hiccup.util/url-encode tag)) tag)
                          [:span {:class "divider"} "-"])) tags)))
(defn article
  "Render an article. Expects media to have been hydrated"
  [user article url]
  (let [img (let [img_src (get (first (:media article)) :src)]
              (if (empty? img_src)
                "https://cdn.4d4ms.com/img/A.jpg"
                img_src))]
    (html5
     [:head
      (header (:title article))
      [:meta {:name "twitter:card" :content "summary"}]
      [:meta {:name "twitter:site" :content config/twitter-site-handle}]
      [:meta {:name "twitter:title" :content (:title article)}]
      [:meta {:name "twitter:image" :content img}]
      [:meta {:name "twitter:description" :content (:description article)}]
      [:meta {:property "og:url" :content url}]
      [:meta {:property "og:type" :content "article"}]
      [:meta {:property "og:title" :content (:title article)}]
      [:meta {:property "og:description" :content (:description article)}]
      [:meta {:property "og:image" :content img}]]
     [:body
      [:div {:class "mango"}
       [:div {:align "center"}
        [:ins {:class "adsbygoogle"
               :style "display:block"
               :data-ad-client "ca-pub-8004927313720287"
               :data-ad-slot "5968146561"
               :data-ad-format "auto"}]
        (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")]
       (toolbar user article)
       [:div {:class "article-header"}
        [:h1 {:class "article-title"} (:title article)]
        [:h2 {:class "article-description"} (:description article)]
        [:div {:class "article-byline"} "Posted By: " (get-in article [:user :displayName]) "(@" (get-in article [:user :twitterHandle]) ")"]
        [:div {:class "article-infoline"} "On: " (xform-time-to-string (:created article))]
        [:div {:class "article-tagsline"} "Tagged: " (tags (:tags article))]
        [:div {:class "article-socialline"}
         (link-to {:class "twitter-share-button"
                   :data-url url}
                  (hiccup.util/url "https://twitter.com/intent/tweet" {:text (:description article)})
                  "Tweet")
         (let [handle (get-in article [:user :twitterHandle])]
           (link-to {:class "twitter-follow-button"
                     :data-show-count "true"}
                    (str "https://twitter.com/" handle)
                    (str "Follow @" handle)))]]
       [:div {:class "article-content"}
        (:rendered-content article)]]
      (footer)])))

(defn articles
  "Render a list of articles"
  [user list-title articles]
  (html5 [:head (header list-title)]
         [:body
          [:div {:class "mango"}
           (toolbar user)
           [:h1 list-title]
           (map (fn [article] [:div {:class "article-list-item"}
                               [:h2 (link-to (str "/blog/" (:slug article)) (:title article))]
                               [:p (:description article)]]) articles)]
          (footer)]))

(defn root
  "Render the root page"
  [user]
  (html5 [:head (header "4d4ms.com")]
         [:body
          [:div {:class "mango"}
           [:h2 (link-to "/blog" "Blog")]
           [:h2 (link-to "/photography" "Photography")]
           [:h2 (link-to "/about" "About")]]
          (footer)]))

(defn photography
  "Render the photography page"
  [user]
  (html5 [:head (header "Photography")]
         [:body
          [:div {:class "mango"}
           [:h3 (link-to "http://www.flickr.com/photos/beamjack/tags/animals/" "Animals")]
           [:h3 (link-to "http://www.flickr.com/photos/beamjack/tags/buildings/" "Buildings")]
           [:h3 (link-to "http://www.flickr.com/photos/beamjack/tags/places/" "Places")]
           [:h3 (link-to "http://www.flickr.com/photos/beamjack/" "Everything Else")]]
          (footer)]))

(defn about
  "Render the about page"
  [user]
  (html5 [:head (header "About")]
         [:body
          [:div {:class "mango"}
           [:h3 "Me"]
           [:p "I've been a professional software developer for 18 years. I live in beautiful San Francisco Bay area with my wife and our three children. I'm currently a Staff Software Engineer at " (link-to "https://twitter.com" "Twitter")]
           [:h3 "Interests"]
           [:p (link-to "http://www.github.com/leeaustinadams" "Code") ", Technology, Books, Movies, Hiking, Backpacking, Travel, " (link-to "/photography" "Photography") ", Motorcycles"]
           [:h3 "Crypto"]
           [:ul
            [:li (link-to "https://keybase.io/leeadams" "Keybase.io")]
            [:li (link-to "https://4d4ms.com/lee.asc" "PGP Key")]]]
          (footer)]))

(defn field-row
  "Render a form row with label"
  [field name label-content & [value]]
  (list [:div {:class "row"}
         [:div {:class "col-25"}
          (label name label-content)]
         [:div {:class "col-75"}
          (field name value)]]))

(defn submit-row
  "Render a form submit row"
  [content]
  [:div {:class "row"}
   (submit-button content)])

(defn sign-in
  "Render the sign in page"
  [user & [message]]
  (html5 [:head (header "Sign In")]
         [:body
          [:div {:class "mango"}
           [:h1 "Sign In"]
           [:form {:name "signin" :action "/auth/signin" :method "POST" :enctype "multipart/form-data"}
            (field-row text-field "username" "Username")
            (field-row password-field "password" "Password")
            (submit-row "Sign In")
            (when message [:p message])]]
          (footer)]))

(defn sign-out
  "Render the sign out page"
  [user & [message]]
  (html5 [:head (header "Sign Out")]
         [:body
          [:div {:class "mango"}
           [:h1 "Sign Out?"]
           [:form {:name "signout" :action "/auth/signout" :method "POST" :enctype "multipart/form-data"}
            (submit-row "Sign Out")
            (when message [:p message])]]
          (footer)]))

(defn sign-in-success
  "Render the sign in success page"
  [user]
  (html5 [:head (header "Success")]
         [:body
          [:div {:class "mango"}
           (toolbar user)
           [:h1 "Signed In"]
           [:p "Username: " (:username user)]
           [:p "Display Name: " (:displayName user)]
           [:p "First Name: " (:firstName user)]
           [:p "Last Name: " (:lastName user)]
           [:p "Email: " (:email user)]
           [:p "Twitter: " (:twitterHandle user)]
           [:p "Roles: " (unordered-list (:roles user))]
           [:p "Created: " (xform-time-to-string (:created user))]]
          (footer)]))

(defn edit-article
  "Render the editing in page"
  [user & [article url]]
  (html5 [:head (header "Edit")]
         [:body
          [:div {:class "mango"}
           (let [id (if article (:_id article) "post")]
             [:form {:name "articleForm" :action (str "/blog/articles/" id ".json") :method "POST" :enctype "multipart/form-data"}
              (when article (hidden-field "_id" (:_id article)))
              (field-row text-field "title" "Title" (when article (:title article)))
              (field-row text-field "description" "Description" (when article (:description article)))
              (field-row text-field "tags" "Tags" (when article (csv (:tags article))))
              (field-row text-area "content" "Content" (when article (:content article)))
              (field-row (fn [name value] [:input {:name name :type "date" :value value}]) "created" "Date" (when article (xform-time-to-string (:created article))))
              (field-row (fn [name value] (drop-down name
                                                     (list ["Draft" "draft"]
                                                           ["Published" "published"]
                                                           ["Trash" "trash"])
                                                     value))
                         "status"
                         "Status"
                         (if article (:status article) "draft"))
              (submit-row "Submit")])]
          (footer)]))

(defn not-found
  "Render a page for when a URI is not found"
  [user]
  (html5
   [:head (header "Not Found")]
   [:body
    [:div {:class "mango"}
     [:h1 "Not found!"]
     [:p "The page you are looking for could not be found"]]
    (footer)]))

(defn sitemap
  "Render a sitemap for indexing"
  [urls]
  (let [url-list (for [u urls] (hash-map :url u))]
    (stencil/render-file "templates/sitemap.txt" {:urls url-list})))
