;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.meta-tags :refer :all]
            [mango.util :refer [xform-time-to-string
                                xform-string-to-time
                                author-name
                                url-encode
                                str-or-nil
                                load-edn]]
            [mango.widgets :refer :all]
            [clojure.string :as str]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]
            [mango.ads :as ads]))

(def default-per-page 100)

(def site-meta-tags (-> (fn [options] '())
                        wrap-default
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
                                                                 on-unload
                                                                 show-ad
                                                                 robots
                                                                 keywords]
                                                          :or { show-toolbar {:user user :redir url}
                                                               show-footer true
                                                               show-social false
                                                               on-load ""
                                                               on-unload ""
                                                               show-ad true}}]
  (let [description (or description config/site-description)]
    (html5 [:head (head title)
            (render-page-meta {:url (str config/site-url url)
                               :title title
                               :description description
                               :image-url image-url
                               :og-type "article"
                               :robots robots
                               :keywords keywords})]
           [:body {:onload on-load :onunload on-unload}
            [:div.mango
             (when (and config/ads-enabled config/header-ads-enabled show-ad) (ads/google config/google-ad-client config/google-ad-slot))
             (when show-toolbar (toolbar show-toolbar))
             (when header header)
             (when show-social (socialbar show-social))
             content
             (when (and config/ads-enabled config/footer-ads-enabled show-ad) (ads/google config/google-ad-client config/google-ad-slot))]
            (when show-footer (footer))])))

(defn article
  "Render an article. Expects media to have been hydrated"
  [user {:keys [title description tags media created rendered-content status] {author-user-name :username author-first-name :first-name author-last-name :last-name } :user :as article} url]
    (render-page user
                 title
                 description
                 url
                 [:div.article-header
                  [:h1.article-title title]
                  [:h2.article-description description]
                  [:div.article-byline [:span "Posted By: " (author-name author-first-name author-last-name) " (" author-user-name ")"]]
                  [:div.article-infoline [:span "On: " (xform-time-to-string created)]]
                  [:div.article-tagsline [:span "Tagged: " (tags-list tags)]]]
                 (str-or-nil (get (first media) :src))
                 [:div.article-content
                  (list rendered-content [:div.clearfix])]
                 :show-toolbar {:user user :redir url :article article}
                 :show-social {:title title :description description :url url }
                 :keywords tags
                 :on-load "mango.article.on_load()"
                 :on-unload "mango.article.on_unload()"))

(defn page
  "Render a page"
  [user {:keys [title rendered-content] :as page} url]
  (render-page user
               title
               nil
               url
               nil
               nil
               [:div.page-content
                (list rendered-content [:div.clearfix])]
               :show-toolbar {:user user :redir url :page page}
               :show-social {:title title :url url}
               :on-load "mango.page.on_load()"
               :on-unload "mango.page.on_unload()"))

(defn- syntax
  []
    [:div.syntax.hidden {:id "syntax"}
     (load-edn "syntax.edn")])

(def syntax-memo (memoize syntax))

(defn edit-page
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title content status media]}]]
  (render-page user
               "Edit"
               nil
               nil
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
                     [:button {:id "syntax-button"} "Syntax"]
                     [:button {:id "preview-button"} "Preview"]]]
                   (field-row (partial text-area {:id "content"
                                                  :ondragover "mango.media.allowMediaDrop(event)"
                                                  :ondrop "mango.media.mediaDrop(event)"})
                              "content" "Content" content)
                   [:div.article-content.content-preview.hidden {:id "preview"}]
                   (syntax-memo)
                   (field-row thumb-bar "thumbs" "Media"  media)
                   [:div.field-row
                    [:div.col-25
                     [:span "&nbsp;"]]
                    [:div.col-75
                     (link-to (str "/blog/media/new?page-id=" _id) "Add Media")]]
                   (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"] ["Root" "root"]) (or status "draft"))
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
  [user title tags url]
  (let [tag-counts (reverse (sort-by second (frequencies tags)))]
    (standard-page user title (divided-list tag-counts tag-and-count "-" "tags") url)))

(defn about
  "Render the about page"
  [user url]
  (render-page user
               "About"
               nil
               url
               nil
               nil
               (list
                [:h3 "About You"]
                [:p "Put something about yourself here"])))

(defn sign-in
  "Render the sign in page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign In"
               nil
               nil
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
               :show-toolbar false
               :robots "noindex"))

(defn sign-out
  "Render the sign out page"
  [user anti-forgery-token & [message redir]]
  (render-page user
               "Sign Out"
               nil
               nil
               nil
               nil
               (list
                [:h1 "Sign Out?"]
                [:form {:name "signout" :action (str "/auth/signout?redir=" redir) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (submit-row "Sign Out")
                 (when message [:p message])])
               :show-toolbar false
               :robots "noindex"))

(defn new-user
  "Render a user form"
  [user anti-forgery-token & [message]]
  (render-page user
               "New User"
               nil
               nil
               nil
               nil
               [:form {:name "newuser" :action (str "/users/new") :method "POST" :enctype "multipart/form-data"}
                (hidden-field "__anti-forgery-token" anti-forgery-token)
                (field-row (partial text-field {:autoFocus "autoFocus" :autocomplete "username" :required "required"}) "username" "Username")
                (field-row text-field "first-name" "First")
                (field-row text-field "last-name" "Last")
                (field-row required-text-field "email" "Email Address")
                (field-row required-new-password-field "password" "Password")
                (field-row required-new-password-field "password2" "Confirm Password")
                (field-row dropdown-field "role" "Role" (list ["Editor" "editor"] ["Administrator" "admin"] ["User" "user"]) "user")
                (submit-row "Submit")
                (when message [:p.error message])]
               :show-toolbar false
               :robots "noindex"))

(defn edit-user
  "Render editing form for a user"
  [user anti-forgery-token & [{:keys [_id username first-name last-name email]}]]
  (render-page user
               "Edit"
               nil
               nil
               nil
               nil
               [:div.content-form
                [:form.edit-form {:name "pageForm" :action (str "/users/edit/" _id) :method "POST" :enctype "multipart/form-data"}
                 (hidden-field "__anti-forgery-token" anti-forgery-token)
                 (hidden-field "_id" _id)
                 (field-row (partial text-field {:autoFocus "autoFocus" :autocomplete "username" :required "required"}) "username" "Username" username)
                 (field-row text-field "first-name" "First" first-name)
                 (field-row text-field "last-name" "Last" last-name)
                 (field-row required-text-field "email" "Email Address" email)
                 (submit-row "Submit")]]))

(defn change-password
  "Render a change password form"
  [user anti-forgery-token & [message]]
  (render-page user
               "Change Password"
               nil
               nil
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
               :show-toolbar false
               :robots "noindex"))

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
               nil
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
                     [:button {:id "syntax-button"} "Syntax"]
                     [:button {:id "preview-button"} "Preview"]]]
                   (field-row (partial text-area {:id "content"
                                                  :ondragover "mango.media.allowMediaDrop(event)"
                                                  :ondrop "mango.media.mediaDrop(event)"})
                              "content" "Content" content)
                   [:div.article-content.content-preview.hidden {:id "preview"}]
                   (syntax-memo)
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
  [user anti-forgery-token & [{:keys [article-id page-id]}]]
  (let [[param id] (cond
                     (not (str/blank? article-id)) ["article-id" article-id]
                     (not (str/blank? page-id)) ["page-id" page-id])]
    (render-page user
                 "Upload Media"
                 nil
                 nil
                 nil
                 nil
                 (list
                  [:p (or article-id page-id)]
                  [:form {:id "upload-form" :name "uploadForm" :action "/blog/media/post" :method "POST" :enctype "multipart/form-data"}
                   (hidden-field {:id "anti-forgery-token"} "__anti-forgery-token" anti-forgery-token)
                   (hidden-field {:id param} param id)
                   (file-select-row "Choose Files..." {:id "file-select" :name "files" :multiple true})
                   [:p {:id "upload-status"}]
                   (submit-row "Upload" {:id "file-upload"})])
                  :show-toolbar false)))

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
                [:p.error body])
               :robots "noindex"))

(defn not-found
  "Render a page for when a URI is not found"
  [user request-url]
  (error user "Not Found" "The page you are looking for could not be found" request-url))

(defn sitemap
  "Render a sitemap for indexing"
  [path articles pages]
  (let [article-list (for [a articles] (hash-map :article a))
        page-list (for [p pages] (hash-map :page p))]
    (stencil/render-file "templates/sitemap.txt" {:path path :articles article-list :pages page-list})))

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
                      [:h1.col-100 "Configure Your Root Page"]
                      [:p "You should " (link-to "/signin" "sign in") " and add a page, choosing the \"Root\" option to publish it as the page shown when a user fetches the base URL of your site"]])
               :on-load "mango.widgets.on_load()"))
