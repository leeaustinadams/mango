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

(def default-per-page 100)

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
  [user & [article redir]]
  [:div.toolbar
   (unordered-list (filter #(not (nil? %)) (list (link-to "/" "Home")
                                                 (link-to "/blog" "Blog")
                                                 (when (auth/editor? user) (link-to "/blog/new" "New"))
                                                 (when (auth/editor? user) (link-to "/blog/drafts" "Drafts"))
                                                 (when (auth/editor? user) (link-to "/blog/media" "Media"))
                                                 (when (and article (auth/editor? user)) (link-to (str "/edit/" (:slug article)) "Edit"))
                                                 (if user
                                                   (link-to (str "/signout?redir=" redir) "Sign out")
                                                   (link-to (str "/signin?redir=" redir) "Sign in")))))])

(defn divided-list
  "Render a divided list"
  [items item-fn divider & [class]]
  (unordered-list (when class {:class class})
                  (let [divider [:span.divider divider]]
                    (partition-all 2 (interpose divider (map item-fn items))))))

(defn link-to-tag
  "Render a link to articles with a tag"
  [tag]
  (link-to (str "/blog/tagged/" (url-encode tag)) tag))

(defn tags-list
  "Render tags"
  [tags]
  (divided-list tags link-to-tag "-" "tags"))

(defn tweet-button
  "Render a Tweet button that will prepopulate with text"
  [url text]
  (when (and url text)
    (link-to {:class "twitter-share-button"
              :data-url url}
             (hiccup.util/url "https://twitter.com/intent/tweet" {:text text})
             "Tweet")))

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
  [user {:keys [title description tags media created rendered-content status] {author-user-name :username author-name :displayName author-twitter-handle :twitter-handle} :user :as article} url]
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
       (when (and config/ads-enabled (not (= "draft" status)))
         [:div {:align "center"}
          [:ins {:class "adsbygoogle"
                 :style "display:block"
                 :data-ad-client "ca-pub-8004927313720287"
                 :data-ad-slot "5968146561"
                 :data-ad-format "auto"}]
          (javascript-tag "(adsbygoogle = window.adsbygoogle || []).push({});")])
       (toolbar user article url)
       [:div.article-header
        [:h1.article-title title]
        [:h2.article-description description]
        [:div.article-byline "Posted By: " author-name " (" author-user-name ")"]
        [:div.article-infoline "On: " (xform-time-to-string created)]
        [:div.article-tagsline "Tagged: " (tags-list tags)]
        [:div.article-socialline
         (tweet-button url description)
         (follow-button author-twitter-handle)]]
       [:div.article-content rendered-content]]
      (footer)])))

(defn render-page
  "Renders a page"
  [user title content & {:keys [show-toolbar show-footer redir]}]
  (html5 [:head (header title)]
         [:body
          [:div.mango
           (when show-toolbar (toolbar user nil (or redir "/")))
           content]
          (when show-footer (footer))]))

(defn root
  "Render the root page"
  [user]
  (render-page user "4d4ms.com"
        (list [:h2 (link-to "/blog" "Blog")]
              [:h2 (link-to "/photography" "Photography")]
              [:h2 (link-to "/about" "About")])))

(defn article-list-item
  "Render an article list item"
  [{:keys [slug title description media created]}]
  [:div.article-list-item
   [:div.row
    [:div.col-100
     [:h2.article-list-item-title (link-to (str "/blog/" slug) title)]]]
   [:div.row
    [:div.col-25.article-list-item-byline "On: " (xform-time-to-string created)]]
   [:div.row
    (when-let [thumb (first media)]
      [:div.col-25-sm
       (image {:class "article-list-item-media"} (:src thumb))])
    [:div.col-75-sm description]]
   ])

(defn articles-list
  "Render a list of articles"
  [user list-title articles]
  (render-page user list-title
        (list
         [:h1 list-title]
         (map article-list-item articles))
        :show-toolbar true
        :show-footer true
        :redir "/blog"))

(defn photography
  "Render the photography page"
  [user]
  (render-page user "Photography"
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
  (render-page user "About"
        (list
         [:h3 "Me"]
         [:p "I've been a professional software developer for almost 20 years. I live in beautiful San Francisco Bay area with my wife and our three children. I'm currently a Staff Software Engineer at " (link-to "https://twitter.com" "Twitter")]
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
  [content & [attr-map]]
  [:div.field-row
   [:div.col-100
    (if attr-map
      (submit-button attr-map content)
      (submit-button content))]])

(defn file-select-row
  "Render a file select row"
  [content & [attr-map]]
  [:div.field-row
   [:div.col-100
    (if attr-map
      (file-upload attr-map content)
      (file-upload content))]])

(defn sign-in
  "Render the sign in page"
  [user anti-forgery-token & [message redir]]
  (render-page user "Sign In"
        (list
         [:h1 "Sign In"]
         [:form {:name "signin" :action (str "/auth/signin?redir=" redir) :method "POST" :enctype "multipart/form-data"}
          (hidden-field "__anti-forgery-token" anti-forgery-token)
          (field-row text-field "username" "Username")
          (field-row password-field "password" "Password")
          (submit-row "Sign In")
          (when message [:p message])])))

(defn sign-out
  "Render the sign out page"
  [user anti-forgery-token & [message redir]]
  (render-page user "Sign Out"
        (list
         [:h1 "Sign Out?"]
         [:form {:name "signout" :action (str "/auth/signout?redir=" redir) :method "POST" :enctype "multipart/form-data"}
          (hidden-field "__anti-forgery-token" anti-forgery-token)
          (submit-row "Sign Out")
          (when message [:p message])])))

(defn roles-list
  "Render a list of roles"
  [roles]
  (divided-list roles identity "·" "roles"))

(defn user-item
  "Render a user's details"
  [{:keys [username displayName firstName lastName email twitter-handle roles created]}]
  (list
   [:div.row "Username: " username]
   [:div.row
    [:div.col-25 "Display Name: " displayName]
    [:div.col-25 "First Name: " firstName]
    [:div.col-25 "Last Name: " lastName]]
   [:div.row
    [:div.col-50 "Email: " (mail-to email email)]
    (when twitter-handle [:div.col-50 "Twitter: " (link-to (str "https://twitter.com/" twitter-handle) twitter-handle)])]
   [:div.row "Roles: " (roles-list roles)]
   [:div.row "Created: " (xform-time-to-string created)]))

(defn user-details
  "Render the user details page"
  [{:keys [username] :as user}]
  (render-page user username
        (user-item user)
        :show-footer true))

(defn admin-users
  "Render user administration page."
  [user users]
  (render-page user "Admin - Users"
        (interpose [:hr] (map user-item users))
        :show-toolbar true :show-footer true))

(defn date-field
  "Renders a date input"
  [name value]
  [:input {:name name :type "date" :value value}])

(defn dropdown-field
  "Renders a dropdown select input"
  [name values default-value]
  (drop-down name values default-value))

(defn thumb-bar
  "Renders an image bar of thumbnails"
  [name media]
  [:div.imagebar
   (map (fn [{:keys [filename src]}] (image {:draggable true :ondragstart "mango.drag(event)" :filename filename} src filename)) media)])

(defn edit-article
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title description tags content created status media]}]]
  (render-page user "Edit"
        (let [action (or _id "post")]
          (list
           [:form {:name "articleForm" :action (str "/blog/articles/" action) :method "POST" :enctype "multipart/form-data"}
            (hidden-field "__anti-forgery-token" anti-forgery-token)
            (hidden-field "media" (apply str (interpose ", " (map #(str (:_id %)) media))))
            (when _id (hidden-field "_id" _id))
            (field-row text-field "title" "Title" title)
            (field-row text-field "description" "Description" description)
            (field-row text-field "tags" "Tags" (apply str (interpose ", " tags)))
            (field-row (partial text-area {:id "content" :ondragover "mango.allowDrop(event)" :ondrop "mango.drop(event)"}) "content" "Content" content)
            (field-row thumb-bar "thumbs" "Media"  media)
            [:div.field-row
             [:div.col-25
              [:span "&nbsp;"]]
             [:div.col-75
              (link-to (str "/blog/media/new?article-id=" _id) "Add Media")]]
            (field-row date-field "created" "Date" (xform-time-to-string created))
            (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft"))
            (submit-row "Submit")]
           (include-js "/js/media-edit.js")
           ))))

(defn upload-media
  "Render the media upload page"
  [user anti-forgery-token & [{:keys [article-id]}]]
  (render-page user "Upload Media"
               (list
                [:p article-id]
                [:form {:id "upload-form" :name "uploadForm" :action "/blog/media/post" :method "POST" :enctype "multipart/form-data"}
                 (hidden-field {:id "anti-forgery-token"} "__anti-forgery-token" anti-forgery-token)
                 (when article-id (hidden-field {:id "article-id"} "article-id" article-id))
                 (file-select-row "Choose Files..." {:id "file-select" :name "files" :multiple true})
                 [:p {:id "upload-status"}]
                 (submit-row "Upload" {:id "file-upload"})]
                (include-js "/js/upload.js"))))


(defn media-list-item
  "Render a media list item"
  [{:keys [src filename user] :as media-item}]
  [:div.media-list-item
   (image (or src filename))
   [:p (or src filename)]
   [:p (:username user)]
   [:p (:_id media-item)]
   (link-to (str "/blog/media/delete?id=" (:_id media-item)) "Delete")])

(defn prev-next-media
  [media prev-page next-page per-page]
  (list
   (when (and prev-page (> prev-page 0)) (link-to (str "/blog/media?page=" prev-page "&per-page=" per-page) "Previous"))
   (when (and media (> (count media) 0)) (link-to (str "/blog/media?page=" next-page "&per-page=" per-page) "Next"))))

(defn media-list
  "Render a list of media"
  [user media {:keys [page per-page]}]
  (let [cur-page (if page (Integer. page) 1)
        next-page (when cur-page (inc cur-page))
        prev-page (when (> cur-page 0) (dec cur-page))
        per-page (if per-page (Integer. per-page) default-per-page)]
    (render-page user "Media"
          (list
           [:h1 "Media"]
           (prev-next-media media prev-page next-page per-page)
           (map media-list-item media)
           (prev-next-media media prev-page next-page per-page))
          :show-toolbar true
          :show-footer true
          :redir "/media")))

(defn not-found
  "Render a page for when a URI is not found"
  [user]
  (render-page user "Not Found"
        (list
         [:h1 "Not found!"]
         [:p "The page you are looking for could not be found"])))

(defn sitemap
  "Render a sitemap for indexing"
  [urls]
  (let [url-list (for [u urls] (hash-map :url u))]
    (stencil/render-file "templates/sitemap.txt" {:urls url-list})))
