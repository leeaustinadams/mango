;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [xform-time-to-string xform-string-to-time url-encode]]
            [clojure.core.strint :refer [<<]]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]))

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

(defn tags-list
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
  [user {:keys [title description tags media created rendered-content] {author-name :displayName author-twitter-handle :twitterHandle} :user :as article} url]
  (let [img (let [img_src (get (first media) :src)]
              (if (empty? img_src)
                "https://cdn.4d4ms.com/img/A.jpg"
                img_src))]
    (html5
     [:head
      (header title)
      [:meta {:name "twitter:card" :content "summary"}]
      [:meta {:name "twitter:site" :content config/twitter-site-handle}]
      [:meta {:name "twitter:title" :content title}]
      [:meta {:name "twitter:image" :content img}]
      [:meta {:name "twitter:description" :content description}]
      [:meta {:property "og:url" :content url}]
      [:meta {:property "og:type" :content "article"}]
      [:meta {:property "og:title" :content title}]
      [:meta {:property "og:description" :content description}]
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
        [:h1.article-title title]
        [:h2.article-description description]
        [:div.article-byline "Posted By: " author-name " (@" author-twitter-handle ")"]
        [:div.article-infoline "On: " (xform-time-to-string created)]
        [:div.article-tagsline "Tagged: " (tags-list tags)]
        [:div.article-socialline
         (tweet-button url description)
         (follow-button author-twitter-handle)]]
       [:div.article-content rendered-content]]
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

(defn article-list-item
  "Render an article list item"
  [{:keys [slug title description]}]
  [:div.article-list-item
   [:h2 (link-to (str "/blog/" slug) title)]
   [:p description]])

(defn articles-list
  "Render a list of articles"
  [user list-title articles]
  (page user list-title
        (list
         [:h1 list-title]
         (map article-list-item articles))
        :show-toolbar true
        :show-footer true))

(defn photography
  "Render the photography page"
  [user]
  (page user "Photography"
        (list
         [:div.row
          [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/animals/" "Animals")]
          [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/buildings/" "Buildings")]]
         [:div.row
          [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/places/" "Places")]
          [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/" "Everything Else")]])))

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
  (list [:div.field-row
         [:div.col-25 (label name label-content)]
         [:div.col-75 (apply field name rest)]]))

(defn submit-row
  "Render a form submit row"
  [content]
  [:div.row (submit-button content)])

(defn sign-in
  "Render the sign in page"
  [user anti-forgery-token & [message]]
  (page user "Sign In"
        (list
         [:h1 "Sign In"]
         [:form {:name "signin" :action "/auth/signin" :method "POST" :enctype "multipart/form-data"}
          (hidden-field "__anti-forgery-token" anti-forgery-token)
          (field-row text-field "username" "Username")
          (field-row password-field "password" "Password")
          (submit-row "Sign In")
          (when message [:p message])])))

(defn sign-out
  "Render the sign out page"
  [user anti-forgery-token & [message]]
  (page user "Sign Out"
        (list
         [:h1 "Sign Out?"]
         [:form {:name "signout" :action "/auth/signout" :method "POST" :enctype "multipart/form-data"}
          (hidden-field "__anti-forgery-token" anti-forgery-token)
          (submit-row "Sign Out")
          (when message [:p message])])))

(defn user-details
  "Render the user details page"
  [{:keys [username displayName firstName lastName email twitterHandle roles created] :as user}]
  (page user "Success"
        (list
         [:p "Username: " username]
         [:p "Display Name: " displayName]
         [:p "First Name: " firstName]
         [:p "Last Name: " lastName]
         [:p "Email: " (mail-to email email)]
         [:p "Twitter: " (link-to (str "https://twitter.com/" twitterHandle) twitterHandle)]
         [:p "Roles: " (unordered-list roles)]
         [:p "Created: " (xform-time-to-string created)])))

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
  [user anti-forgery-token & [{:keys [_id title description tags content created status]}]]
  (page user "Edit"
        (let [action (or _id "post")]
          [:form {:name "articleForm" :action (str "/blog/articles/" action) :method "POST" :enctype "multipart/form-data"}
           (hidden-field "__anti-forgery-token" anti-forgery-token)
           (when _id (hidden-field "_id" _id))
           (field-row text-field "title" "Title" title)
           (field-row text-field "description" "Description" description)
           (field-row text-field "tags" "Tags" (apply str (interpose ", " tags)))
           (field-row text-area "content" "Content" content)
           (field-row date-field "created" "Date" (xform-time-to-string created))
           (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft"))
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
