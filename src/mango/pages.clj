;; https://github.com/davidsantiago/stencil
(ns mango.pages
  (:require [mango.auth :as auth]
            [mango.config :as config]
            [mango.util :refer [xform-time-to-string xform-string-to-time url-encode]]
            [mango.widgets :refer :all]
            [clojure.core.strint :refer [<<]]
            [stencil.core :as stencil]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer :all]
            [hiccup.form :refer :all]))

(def default-per-page 100)

(defn article
  "Render an article. Expects media to have been hydrated"
  [user {:keys [title description tags media created rendered-content status] {author-user-name :username author-name :displayName author-twitter-handle :twitter-handle} :user :as article} url]
  (let [img (let [img_src (get (first media) :src)]
              (if (empty? img_src) config/logo-url img_src))]
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
       [:div.article-content
        (list rendered-content [:div.clearfix])]]
      (footer)])))

(defn- render-page
  "Renders a page"
  [user title content & {:keys [show-toolbar show-footer redir] :or { show-toolbar true show-footer true redir "/" }}]
  (html5 [:head (header title)]
         [:body
          [:div.mango
           (when show-toolbar (toolbar user nil redir))
           content]
          (when show-footer (footer))]))

(defn page
  "Render a page"
  [user {:keys [title rendered-content]} redir]
  (render-page user title
               (list
                rendered-content
                [:div.clearfix])
               :show-toolbar true :show-footer true :redir redir))

(defn edit-page
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title content status media]}]]
  (render-page user "Edit"
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
            (field-row dropdown-field "status" "Status" (list ["Draft" "draft"] ["Published" "published"] ["Trash" "trash"]) (or status "draft"))
            (submit-row "Submit")]
           (include-js "/js/media-edit.js")))))

(defn pages-list
  "Render a list of pages"
  [user list-title pages]
  (render-page user list-title
        (list
         [:h1 list-title]
         (map page-list-item pages))
        :show-toolbar true
        :show-footer true
        :redir "/blog"))

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

(defn tags
  "Render a page with a list of tags and their counts"
  [user title tag-counts]
  (render-page user title
               (list
                [:h1 title]
                (divided-list tag-counts tag-and-count "-" "tags"))
               :show-toolbar true
               :show-footer true
               :redir "/tagged"))

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

(defn new-user
  "Render a user form"
  [user anti-forgery-token & [message]]
  (render-page user "New User"
               [:form {:name "newuser" :action (str "/users/new") :method "POST" :enctype "multipart/form-data"}
                (hidden-field "__anti-forgery-token" anti-forgery-token)
                (field-row text-field "username" "Username")
                (field-row text-field "first" "First")
                (field-row text-field "last" "Last")
                (field-row text-field "email" "Email Address")
                (field-row text-field "twitter-handle" "Twitter Handle")
                (field-row password-field "password" "Password")
                (field-row password-field "password2" "Confirm Password")
                (field-row dropdown-field "role" "Role" (list ["Editor" "editor"] ["Administrator" "admin"] ["User" "user"]) "user")
                (submit-row "Submit")
                (when message [:p.error message])]))

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

(defn edit-article
  "Render the editing in page"
  [user anti-forgery-token & [{:keys [_id title description tags content created status media]}]]
  (render-page user "Edit"
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
  [path articles]
  (let [article-list (for [a articles] (hash-map :article a))]
    (stencil/render-file "templates/sitemap.txt" {:path path :articles article-list})))

(defn root
  "Render the root page"
  [user & [article]]
  (render-page user config/site-title
        (list [:div.row
               [:h2.col-100 (link-to "/blog" "Blog")]]
              (when article
                (list [:div.row [:span.col-100 "Latest Article:"]] (article-list-item article))))))
