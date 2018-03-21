;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [xform-time-to-string xform-string-to-time url-encode]]
            [clojure.core.strint :refer [<<]]
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
   (javascript-tag (<< "var mango = (function() { return { googleAnalyticsId: \"~{config/google-analytics-id}\" }})();"))
   (include-js (str "/" config/app-js))))

(defn footer
  "Render the footer"
  []
  [:div.footer {:align "center"}
   [:small.copyright "&copy; 2014-2018 Lee Adams " (mail-to config/admin-email "contact")][:br]
   [:small.version config/version]])

(defn toolbar
  "Render the toolbar"
  [user & [article]]
  [:div.toolbar
   (unordered-list (filter #(not (nil? %)) (list (link-to "/" "Home")
                                                 (link-to "/blog" "Blog")
                                                 (when (auth/editor? user) (link-to "/blog/new" "New"))
                                                 (when (auth/editor? user) (link-to "/blog/drafts" "Drafts"))
                                                 (when (and article (auth/editor? user)) (link-to (str "/edit/" (:slug article)) "Edit"))
                                                 (if user
                                                   (link-to "/signout" "Sign out")
                                                   (link-to "/signin" "Sign in")))))])

(defn link-to-tag
  "Render a link to articles with a tag"
  [tag]
  (link-to (str "/blog/tagged/" (url-encode tag)) tag))

(defn tags
  "Render tags"
  [tags]
  (unordered-list {:class "tags"}
                  (let [divider [:span.divider "-"]]
                    (partition-all 2 (interpose divider (map link-to-tag tags))))))

(defn tweet-button
  "Render a Tweet button that will prepopulate with text"
  [url text]
  (link-to {:class "twitter-share-button"
            :data-url url}
           (hiccup.util/url "https://twitter.com/intent/tweet" {:text text})
           "Tweet"))

(defn follow-button
  "Render a Twitter Follow button for handle"
  [handle]
  (when handle
    (link-to {:class "twitter-follow-button"
              :data-show-count "true"}
             (str "https://twitter.com/" handle)
             (str "Follow @" handle))))

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
      [:div.mango
       [:div {:align "center"}
        [:ins {:class "adsbygoogle"
               :style "display:block"
               :data-ad-client "ca-pub-8004927313720287"
               :data-ad-slot "5968146561"
               :data-ad-format "auto"}]
        (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")]
       (toolbar user article)
       [:div.article-header
        [:h1.article-title (:title article)]
        [:h2.article-description (:description article)]
        [:div.article-byline "Posted By: " (get-in article [:user :displayName]) "(@" (get-in article [:user :twitterHandle]) ")"]
        [:div.article-infoline "On: " (xform-time-to-string (:created article))]
        [:div.article-tagsline "Tagged: " (tags (:tags article))]
        [:div.article-socialline
         (tweet-button url (:description article))
         (follow-button (get-in article [:user :twitterHandle]))]]
       [:div.article-content
        (:rendered-content article)]]
      (footer)])))

(defn page
  "Renders a page"
  [user title content & {:keys [show-toolbar show-footer]}]
  (html5 [:head (header title)]
         [:body
          [:div.mango
           (when show-toolbar (toolbar user))
           content]
          (when show-footer (footer))]))

(defn root
  "Render the root page"
  [user]
  (page user "4d4ms.com"
        (list [:h2 (link-to "/blog" "Blog")]
              [:h2 (link-to "/photography" "Photography")]
              [:h2 (link-to "/about" "About")])))

(defn articles
  "Render a list of articles"
  [user list-title articles]
  (page user list-title
        (list
         [:h1 list-title]
         (map (fn [article] [:div {:class "article-list-item"}
                             [:h2 (link-to (str "/blog/" (:slug article)) (:title article))]
                             [:p (:description article)]]) articles))
        :show-toolbar true
        :show-footer true))

(defn photography
  "Render the photography page"
  [user]
  (page user "Photography"
        (list
         [:div {:class "row"}
          [:h3 {:class "col-50"} (link-to "http://www.flickr.com/photos/beamjack/tags/animals/" "Animals")]
          [:h3 {:class "col-50"} (link-to "http://www.flickr.com/photos/beamjack/tags/buildings/" "Buildings")]]
         [:div {:class "row"}
          [:h3 {:class "col-50"} (link-to "http://www.flickr.com/photos/beamjack/tags/places/" "Places")]
          [:h3 {:class "col-50"} (link-to "http://www.flickr.com/photos/beamjack/" "Everything Else")]])))

(defn about
  "Render the about page"
  [user]
  (page user "About"
        (list
         [:h3 "Me"]
         [:p "I've been a professional software developer for 18 years. I live in beautiful San Francisco Bay area with my wife and our three children. I'm currently a Staff Software Engineer at " (link-to "https://twitter.com" "Twitter")]
         [:h3 "Interests"]
         [:p (link-to "http://www.github.com/leeaustinadams" "Code") ", Technology, Books, Movies, Hiking, Backpacking, Travel, " (link-to "/photography" "Photography") ", Motorcycles"]
         [:h3 "Crypto"]
         [:ul
          [:li (link-to "https://keybase.io/leeadams" "Keybase.io")]
          [:li (link-to "https://4d4ms.com/lee.asc" "PGP Key")]])))

(defn field-row
  "Render a form row with label"
  [field name label-content & rest]
  (list [:div {:class "field-row"}
         [:div {:class "col-25"}
          (label name label-content)]
         [:div {:class "col-75"}
          (apply field name rest)]]))

(defn submit-row
  "Render a form submit row"
  [content]
  [:div {:class "row"}
   (submit-button content)])

(defn sign-in
  "Render the sign in page"
  [user & [message]]
  (page user "Sign In"
        (list
         [:h1 "Sign In"]
         [:form {:name "signin" :action "/auth/signin" :method "POST" :enctype "multipart/form-data"}
          (field-row text-field "username" "Username")
          (field-row password-field "password" "Password")
          (submit-row "Sign In")
          (when message [:p message])])))

(defn sign-out
  "Render the sign out page"
  [user & [message]]
  (page user "Sign Out"
        (list
         [:h1 "Sign Out?"]
         [:form {:name "signout" :action "/auth/signout" :method "POST" :enctype "multipart/form-data"}
          (submit-row "Sign Out")
          (when message [:p message])])))

(defn user-details
  "Render the user details page"
  [user]
  (page user "Success"
        (list
         [:p "Username: " (:username user)]
         [:p "Display Name: " (:displayName user)]
         [:p "First Name: " (:firstName user)]
         [:p "Last Name: " (:lastName user)]
         [:p "Email: " (let [email (:email user)] (mail-to email email))]
         [:p "Twitter: " (let [handle (:twitterHandle user)] (link-to (str "https://twitter.com/" handle) handle))]
         [:p "Roles: " (unordered-list (:roles user))]
         [:p "Created: " (xform-time-to-string (:created user))])))

(defn date-field
  "Renders a date input"
  [name value]
  [:input {:name name :type "date" :value value}])

(defn dropdown-field
  "Renders a dropdown select input"
  [name values default-value]
  (drop-down name values default-value))

(defn edit-article
  "Render the editing in page"
  [user & [article url]]
  (page user "Edit"
        (let [id (if article (:_id article) "post")]
          [:form {:name "articleForm" :action (str "/blog/articles/" id ".json") :method "POST" :enctype "multipart/form-data"}
           (when article (hidden-field "_id" (:_id article)))
           (field-row text-field "title" "Title" (when article (:title article)))
           (field-row text-field "description" "Description" (when article (:description article)))
           (field-row text-field "tags" "Tags" (when article (apply str (interpose ", " (:tags article)))))
           (field-row text-area "content" "Content" (when article (:content article)))
           (field-row date-field "created" "Date" (when article (xform-time-to-string (:created article))))
           (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (if article (:status article) "draft"))
           (submit-row "Submit")])))

(defn not-found
  "Render a page for when a URI is not found"
  [user]
  (page user "Not Found"
        (list
         [:h1 "Not found!"]
         [:p "The page you are looking for could not be found"])))

(defn sitemap
  "Render a sitemap for indexing"
  [urls]
  (let [url-list (for [u urls] (hash-map :url u))]
    (stencil/render-file "templates/sitemap.txt" {:urls url-list})))
