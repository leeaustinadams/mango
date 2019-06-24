;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [xform-time-to-string xform-string-to-time url-encode]]
            [mango.widgets :refer :all]
            [mango.ads :as ads]
            [clojure.core.strint :refer [<<]]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]))

(def default-per-page 100)

(defn- render-meta
  "Renders metadata for a page"
  [url title description image-url twitter-handle]
  (list
      [:meta {:name "twitter:card" :content "summary"}]
   [:meta {:name "twitter:site" :content twitter-handle}]
      [:meta {:name "twitter:title" :content title}]
   [:meta {:name "twitter:image" :content image-url}]
      [:meta {:name "twitter:description" :content description}]
      [:meta {:property "og:url" :content url}]
      [:meta {:property "og:type" :content "article"}]
      [:meta {:property "og:title" :content title}]
      [:meta {:property "og:description" :content description}]
   [:meta {:property "og:image" :content image-url}]))

(defn- render-page
  "Renders a page"
  [user title description url header content & {:keys [show-toolbar show-footer show-ad show-social] :or { show-toolbar true show-footer true show-ad true show-social true }}]
  (let [description (or description config/site-description)
        image-url config/logo-url
        twitter-handle config/twitter-site-handle]
    (html5 [:head (head title)
            (render-meta url title description image-url twitter-handle)]
           [:body
            [:div.mango
             (when show-ad (ads/google))
             (when show-toolbar (toolbar user nil url))
             (when header header)
             (when show-social
               [:div.article-socialline
                (when (or title description)
                  (tweet-button url (str title (when (and title description)" - ") description)))
                (follow-button twitter-handle)])
             content]
            (when show-footer (footer))])))

(defn article
  "Render an article. Expects media to have been hydrated"
  [user {:keys [title description tags media created rendered-content status] {author-user-name :username author-name :displayName author-twitter-handle :twitter-handle} :user :as article} url]
  (let [img (let [img_src (get (first media) :src)]
              (if (empty? img_src) config/logo-url img_src))]
    (render-page user
                 title
                 description
                 url
                 [:div.article-header
                  [:h1.article-title title]
                  [:h2.article-description description]
                  [:div.article-byline "Posted By: " author-name " (" author-user-name ")"]
                  [:div.article-infoline "On: " (xform-time-to-string created)]
                  [:div.article-tagsline "Tagged: " (tags-list tags)]]
                 [:div.article-content
                  (list rendered-content [:div.clearfix])]
                 :show-ad (and config/ads-enabled (not (= "draft" status))))))

(defn page
  "Render a page"
  [user {:keys [title rendered-content]} url]
  (render-page user
               title
               nil
               url
               nil
               (list rendered-content [:div.clearfix])))

(defn edit-page
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title content status media]}]]
  (render-page user
               "Edit"
               nil
               "url"
               nil
               (let [action (or _id "post")]
                 (list
                  [:form {:name "pageForm" :action (str "/pages/" action) :method "POST" :enctype "multipart/form-data"}
                   (hidden-field "__anti-forgery-token" anti-forgery-token)
                   (when _id (hidden-field "_id" _id))
                   (field-row text-field "title" "Title" title)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to "https://github.com/yogthos/markdown-clj#supported-syntax" "Markdown Syntax")]]
                   (field-row (partial text-area {:id "content" :ondragover "mango.allowDrop(event)" :ondrop "mango.drop(event)"}) "content" "Content" content)
                   (field-row thumb-bar "thumbs" "Media"  media)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to (str "/blog/media/new?article-id=" _id) "Add Media")]]
                   (when (not (= status "root"))
                     (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft")))
                   (submit-row "Submit")]
                  (include-js "/js/media-edit.js")))
               :show-social false))

(defn pages-list
  "Render a list of pages"
  [user list-title pages url]
  (render-page user
               list-title
               nil
               url
               nil
               (list
                [:h1 list-title]
                (map page-list-item pages))))

(defn articles-list
  "Render a list of articles"
  [user list-title articles url]
  (render-page user
               list-title
               nil
               url
               nil
               (list
                [:h1 list-title]
                (map article-list-item articles))))

(defn tags
  "Render a page with a list of tags and their counts"
  [user title tag-counts url]
  (render-page user
               title
               nil
               url
               nil
               (list
                [:h1 title]
                (divided-list tag-counts tag-and-count "-" "tags"))))

(defn photography
  "Render the photography page"
  [user url]
  (render-page user
               "Photography"
               nil
               url
               nil
               (list
                [:div.row
                 [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/animals/" "Animals")]
                 [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/buildings/" "Buildings")]]
                [:div.row
                 [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/tags/places/" "Places")]
                 [:h3.col-50 (link-to "http://www.flickr.com/photos/beamjack/" "Everything Else")]])))

(defn about
  "Render the about page"
  [user url]
  (render-page user
               "About"
               nil
               url
               nil
               (list
                [:h3 "Me"]
                [:p "I've been a professional software developer for almost 20 years. I live in beautiful San Francisco Bay area with my wife and our three children. I'm currently a Staff Software Engineer at " (link-to "https://twitter.com" "Twitter")]
                [:h3 "Interests"]
                [:p (link-to "http://www.github.com/leeaustinadams" "Code") ", Technology, Books, Movies, Hiking, Backpacking, Travel, " (link-to "/photography" "Photography") ", Motorcycles"]
                [:h3 "Crypto"]
                [:ul
                 [:li (link-to "https://keybase.io/leeadams" "Keybase.io")]
                 [:li (link-to "https://4d4ms.com/lee.asc" "PGP Key")]])))

(defn sign-in
  "Render the sign in page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign In"
               nil
               "url"
               nil
               (list
                [:h1 "Sign In"]
                [:form {:name "signin" :action (str "/auth/signin?redir=" redir) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (field-row (partial text-field {:autoFocus "autoFocus"}) "username" "Username")
                 (field-row password-field "password" "Password")
                 (submit-row "Sign In")
                 (when message [:p message])])
               :show-social false
               :show-toolbar false))

(defn sign-out
  "Render the sign out page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign Out"
               nil
               "url"
               nil
               (list
                [:h1 "Sign Out?"]
                [:form {:name "signout" :action (str "/auth/signout?redir=" redir) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (submit-row "Sign Out")
                 (when message [:p message])])
               :show-social false
               :show-toolbar false))

(defn new-user
  "Render a user form"
  [user anti-forgery-token & [message]]
  (render-page user
               "New User"
               nil
               "url"
               nil
               [:form {:name "newuser" :action (str "/users/new") :method "POST" :enctype "multipart/form-data"}
                (hidden-field "__anti-forgery-token" anti-forgery-token)
                (field-row (partial text-field {:autoFocus "autoFocus"}) "username" "Username")
                (field-row text-field "first" "First")
                (field-row text-field "last" "Last")
                (field-row text-field "email" "Email Address")
                (field-row text-field "twitter-handle" "Twitter Handle")
                (field-row password-field "password" "Password")
                (field-row password-field "password2" "Confirm Password")
                (field-row dropdown-field "role" "Role" (list ["Editor" "editor"] ["Administrator" "admin"] ["User" "user"]) "user")
                (submit-row "Submit")
                (when message [:p.error message])]
               :show-social false
               :show-toolbar false))

(defn change-password
  "Render a change password form"
  [user anti-forgery-token & [message]]
  (render-page user
               "Change Password"
               nil
               "url"
               nil
               (let [{:keys [username first-name last-name]} user]
                 (list
                  [:div.row
                   [:div.col-25 "Username: " username]]
                  [:div.row
                   [:div.col-50 "First Name: " first-name]
                   [:div.col-50 "Last Name: " last-name]]
                  [:form {:name "changepassword" :action (str "/users/password") :method "POST" :enctype "multipart/form-data"}
                   (hidden-field "__anti-forgery-token" anti-forgery-token)
                   (field-row (partial password-field {:autoFocus "autoFocus"}) "password" "Current Password")
                   (field-row password-field "new-password" "New Password")
                   (field-row password-field "new-password2" "Confirm New Password")
                   (submit-row "Submit")
                   (when message [:p.error message])]))
               :show-social false
               :show-toolbar false))

(defn user-details
  "Render the user details page"
  [auth-user {:keys [username] :as user} url]
  (render-page user
               username
               nil
               url
               nil
               (user-item user auth-user)
               :show-social false))

(defn admin-users
  "Render user administration page."
  [auth-user users url]
  (render-page auth-user
               "Admin - Users"
               nil
               url
               nil
               (interpose [:hr] (map #(user-item % auth-user) users))
               :show-social false))

(defn edit-article
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title description tags content created status media]}]]
  (render-page user
               "Edit"
               nil
               "url"
               nil
               (let [action (or _id "post")]
                 (list
                  [:form {:name "articleForm" :action (str "/blog/articles/" action) :method "POST" :enctype "multipart/form-data"}
                   (hidden-field "__anti-forgery-token" anti-forgery-token)
                   (when _id (hidden-field "_id" _id))
                   (field-row text-field "title" "Title" title)
                   (field-row text-field "description" "Description" description)
                   (field-row text-field "tags" "Tags" (apply str (interpose ", " tags)))
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to "https://github.com/yogthos/markdown-clj#supported-syntax" "Markdown Syntax")]]
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
                  (include-js "/js/media-edit.js")))
               :show-social false))

(defn upload-media
  "Render the media upload page"
  [user anti-forgery-token & [{:keys [article-id]}]]
  (render-page user
               "Upload Media"
               nil
               "url"
               nil
               (list
                [:p article-id]
                [:form {:id "upload-form" :name "uploadForm" :action "/blog/media/post" :method "POST" :enctype "multipart/form-data"}
                 (hidden-field {:id "anti-forgery-token"} "__anti-forgery-token" anti-forgery-token)
                 (when article-id (hidden-field {:id "article-id"} "article-id" article-id))
                 (file-select-row "Choose Files..." {:id "file-select" :name "files" :multiple true})
                 [:p {:id "upload-status"}]
                 (submit-row "Upload" {:id "file-upload"})]
                (include-js "/js/upload.js"))
               :show-social false
               :show-toolbar false))

(defn media-list
  "Render a list of media"
  [user media {:keys [page per-page]} url]
  (let [cur-page (if page (Integer. page) 1)
        next-page (when cur-page (inc cur-page))
        prev-page (when (> cur-page 0) (dec cur-page))
        per-page (if per-page (Integer. per-page) default-per-page)]
    (render-page user
                 "Media"
                 nil
                 url
                 nil
                 (list
                  [:h1 "Media"]
                  (prev-next-media media prev-page next-page per-page)
                  (map media-list-item media)
                  (prev-next-media media prev-page next-page per-page))
                 :show-social false)))

(defn error
  "Render an error page"
  [user headline body request-url]
  (render-page user
               headline
               nil
               request-url
               nil
               (list
                [:h1.error headline]
                [:p.error body])
               :show-social false))

(defn not-found
  "Render a page for when a URI is not found"
  [user request-url]
  (error user "Not Found" "The page you are looking for could not be found" request-url))

(defn sitemap
  "Render a sitemap for indexing"
  [path articles]
  (let [article-list (for [a articles] (hash-map :article a))]
    (stencil/render-file "templates/sitemap.txt" {:path path :articles article-list})))

(defn root
  "Render the root page"
  [user url & [article]]
  (render-page user
               config/site-title
               config/site-description
               url
               [:div.row
                [:h1.col-100 config/site-title]]
               (list [:div.row
                      [:h2.col-100 (link-to "/blog" "Blog")]]
                     (when article
                       (list [:div.row [:span.col-100 "Latest Article:"]] (article-list-item article)))
                     [:div.row
                      [:h2.col-100 (link-to "/photography" "Photography")]]
                     [:div.row
                      [:h2.col-100 (link-to "/about" "About")]])
               :show-ad false))
