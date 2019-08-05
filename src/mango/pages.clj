;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.meta-tags :refer :all]
            [mango.util :refer [xform-time-to-string xform-string-to-time url-encode str-or-nil]]
            [mango.widgets :refer :all]
            [clojure.core.strint :refer [<<]]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]))

(def default-per-page 100)

(def site-meta-tags (-> (fn [options] '())
                        wrap-default
                        wrap-twitter
                        wrap-og))

(defn render-page-meta
  "Renders metadata for a page"
  [options]
  (site-meta-tags options))

(defn- render-page
  "Renders a page"
  [user title description url header image-url content & {:keys [show-toolbar
                                                                 show-footer
                                                                 show-social
                                                                 on-load
                                                                 on-unload]
                                                          :or { show-toolbar {:user user :redir url}
                                                               show-footer true
                                                               show-social false
                                                               on-load ""
                                                               on-unload ""}}]
  (let [description (or description config/site-description)
        twitter-handle config/twitter-site-handle]
    (html5 [:head (head title)
            (render-page-meta {:url url
                               :title title
                               :description description
                               :image-url image-url
                               :twitter-handle twitter-handle
                               :twitter-card "summary"
                               :og-type "article"})]
           [:body {:onload on-load :onunload on-unload}
            [:div.mango
             (when show-toolbar (toolbar show-toolbar))
             (when header header)
             (when show-social (socialbar show-social))
             content]
            (when show-footer (footer))])))

(defn article
  "Render an article. Expects media to have been hydrated"
  [user {:keys [title description tags media created rendered-content status] {author-user-name :username author-name :displayName author-twitter-handle :twitter-handle} :user :as article} url]
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
               (str-or-nil (get (first media) :src))
                 [:div.article-content
                (list rendered-content [:div.clearfix])]
               :show-toolbar {:user user :redir url :article article}
               :show-social {:title title :description description :url url :twitter-handle author-twitter-handle}
               :on-load "mango.article.on_load()"
               :on-unload "mango.article.on_unload()"))

(defn page
  "Render a page"
  [{:keys [author-twitter-handle] :as user} {:keys [title rendered-content] :as page} url]
  (render-page user
               title
               nil
               url
               nil
               nil
               (list rendered-content [:div.clearfix])
               :show-toolbar {:user user :redir url :page page}
               :show-social {:title title :url url :twitter-handle author-twitter-handle}
               :on-load "mango.page.on_load()"
               :on-unload "mango.page.on_unload()"))

(defn edit-page
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title content status media]}]]
  (render-page user
               "Edit"
               nil
               "url"
               nil
               nil
               (let [action (or _id "post")]
                 [:div.content-form
                  [:form.edit-form {:name "pageForm" :action (str "/pages/" action) :method "POST" :enctype "multipart/form-data"}
                   (hidden-field "__anti-forgery-token" anti-forgery-token)
                   (when _id (hidden-field "_id" _id))
                   (field-row required-text-field "title" "Title" title)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to "https://github.com/yogthos/markdown-clj#supported-syntax" "Markdown Syntax")]]
                   (field-row (partial text-area {:id "content"
                                                  :ondragover "mango.media.allowMediaDrop(event)"
                                                  :ondrop "mango.media.mediaDrop(event)"})
                              "content" "Content" content)
                   [:div.article-content.content-preview.hidden {:id "preview"}]
                   [:button {:id "preview-button"} "preview"]
                   (field-row thumb-bar "thumbs" "Media"  media)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to (str "/blog/media/new?article-id=" _id) "Add Media")]]
                   (when (not (= status "root"))
                     (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft")))
                   (submit-row "Submit")]])
               :on-load "mango.edit.on_load()"
               :on-unload "mango.edit.on_unload()"))

(defn- standard-page
  [user title items url]
  (render-page user
               title
               nil
               url
               nil
               nil
               (list [:h1 title] items)
               :on-load "mango.widgets.on_load()"))

(defn pages-list
  "Render a list of pages"
  [user list-title pages url]
  (standard-page user list-title (map page-list-item pages) url))

(defn articles-list
  "Render a list of articles"
  [user list-title articles url]
  (standard-page user list-title (map article-list-item articles) url))

(defn tags
  "Render a page with a list of tags and their counts"
  [user title tag-counts url]
  (standard-page user title (divided-list tag-counts tag-and-count "-" "tags") url))

(defn sign-in
  "Render the sign in page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign In"
               nil
               "url"
               nil
               nil
               (list
                [:h1 "Sign In"]
                [:form {:name "signin" :action (str "/auth/signin?redir=" redir) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (field-row required-username-field "username" "Username")
                 (field-row required-password-field "password" "Password")
                 (submit-row "Sign In")
                 (when message [:p message])])
               :show-toolbar false))

(defn sign-out
  "Render the sign out page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign Out"
               nil
               "url"
               nil
               nil
               (list
                [:h1 "Sign Out?"]
                [:form {:name "signout" :action (str "/auth/signout?redir=" redir) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (submit-row "Sign Out")
                 (when message [:p message])])
               :show-toolbar false))

(defn new-user
  "Render a user form"
  [user anti-forgery-token & [message]]
  (render-page user
               "New User"
               nil
               "url"
               nil
               nil
               [:form {:name "newuser" :action (str "/users/new") :method "POST" :enctype "multipart/form-data"}
                (hidden-field "__anti-forgery-token" anti-forgery-token)
                (field-row (partial text-field {:autoFocus "autoFocus" :autocomplete "username" :required "required"}) "username" "Username")
                (field-row text-field "first" "First")
                (field-row text-field "last" "Last")
                (field-row required-text-field "email" "Email Address")
                (field-row text-field "twitter-handle" "Twitter Handle")
                (field-row required-new-password-field "password" "Password")
                (field-row required-new-password-field "password2" "Confirm Password")
                (field-row dropdown-field "role" "Role" (list ["Editor" "editor"] ["Administrator" "admin"] ["User" "user"]) "user")
                (submit-row "Submit")
                (when message [:p.error message])]
               :show-toolbar false))

(defn change-password
  "Render a change password form"
  [user anti-forgery-token & [message]]
  (render-page user
               "Change Password"
               nil
               "url"
               nil
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
                   (field-row (partial password-field {:autocomplete "current-password" :autoFocus "" :required ""}) "password" "Current Password")
                   (field-row required-new-password-field "new-password" "New Password")
                   (field-row required-new-password-field "new-password2" "Confirm New Password")
                   (submit-row "Submit")
                   (when message [:p.error message])]))
               :show-toolbar false))

(defn user-details
  "Render the user details page"
  [auth-user {:keys [username] :as user} url]
  (standard-page auth-user username (user-item user auth-user) url))

(defn admin-users
  "Render user administration page."
  [auth-user users url]
  (standard-page auth-user "Admin - Users" (interpose [:hr] (map #(user-item % auth-user) users)) url))

(defn edit-article
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title description tags content created status media]}]]
  (render-page user
               "Edit"
               nil
               "url"
               nil
               nil
               (let [action (or _id "post")]
                 [:div.content-form
                  [:form.edit-form {:name "articleForm" :action (str "/blog/articles/" action) :method "POST" :enctype "multipart/form-data"}
                   (hidden-field "__anti-forgery-token" anti-forgery-token)
                   (when _id (hidden-field "_id" _id))
                   (field-row required-text-field "title" "Title" title)
                   (field-row text-field "description" "Description" description)
                   (field-row text-field "tags" "Tags" (apply str (interpose ", " tags)))
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to "https://github.com/yogthos/markdown-clj#supported-syntax" "Markdown Syntax")]]
                   (field-row (partial text-area {:id "content"
                                                  :ondragover "mango.media.allowMediaDrop(event)"
                                                  :ondrop "mango.media.mediaDrop(event)"})
                              "content" "Content" content)
                   [:div.article-content.content-preview.hidden {:id "preview"}]
                   [:button {:id "preview-button"} "preview"]
                   (field-row thumb-bar "thumbs" "Media"  media)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to (str "/blog/media/new?article-id=" _id) "Add Media")]]
                   (field-row date-field "created" "Date" (xform-time-to-string (or created (clj-time.core/now))))
                   (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft"))
                   (submit-row "Submit")]])
               :on-load "mango.edit.on_load()"
               :on-unload "mango.edit.on_unload()"))

(defn upload-media
  "Render the media upload page"
  [user anti-forgery-token & [{:keys [article-id]}]]
  (render-page user
               "Upload Media"
               nil
               "url"
               nil
               nil
               (list
                [:p article-id]
                [:form {:id "upload-form" :name "uploadForm" :action "/blog/media/post" :method "POST" :enctype "multipart/form-data"}
                 (hidden-field {:id "anti-forgery-token"} "__anti-forgery-token" anti-forgery-token)
                 (when article-id (hidden-field {:id "article-id"} "article-id" article-id))
                 (file-select-row "Choose Files..." {:id "file-select" :name "files" :multiple true})
                 [:p {:id "upload-status"}]
                 (submit-row "Upload" {:id "file-upload"})])
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
                 nil
                 (list
                  [:h1 "Media"]
                  (prev-next-media media prev-page next-page per-page)
                  (map media-list-item media)
                  (prev-next-media media prev-page next-page per-page)))))

(defn error
  "Render an error page"
  [user headline body request-url]
  (render-page user
               headline
               nil
               request-url
               nil
               nil
               (list
                [:h1.error headline]
                [:p.error body])))

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
               nil
               (list [:div.row
                      [:h2.col-100 (link-to "/blog" "Blog")]]
                     (when article
                       [:div.row [:span.col-100 "Latest Article:"]] (article-list-item article)))))
