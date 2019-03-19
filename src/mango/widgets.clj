;; https://github.com/davidsantiago/stencil
(ns mango.widgets
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
   (include-js (str "/" config/app-js))))

(defn footer
  "Render the footer"
  []
  [:div.footer {:align "center"}
   [:small.copyright "Content " config/site-copyright " " (mail-to config/admin-email "contact")][:br]
   [:small.copyright "Powered by " (link-to "https://4d4ms.com/mango" "Mango")][:br]
   [:small.version config/version]])

(defn toolbar
  "Render the toolbar"
  [user & [article redir]]
  [:div.toolbar
   (unordered-list (filter #(not (nil? %)) (list (link-to "/" "Home")
                                                 (link-to "/blog" "Blog")
                                                 (link-to "/blog/tagged" "Tags")
                                                 (when (auth/editor? user) (link-to "/blog/new" "New"))
                                                 (when (auth/editor? user) (link-to "/blog/drafts" "Drafts"))
                                                 (when (auth/editor? user) (link-to "/blog/media" "Media"))
                                                 (when (auth/admin? user) (link-to "/admin/users" "Users"))
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

(defn article-list-item
  "Render an article list item"
  [{:keys [slug title description media created]}]
  [:div.article-list-item
   [:div.row
    [:div.col-75
     [:h2.article-list-item-title (link-to (str "/blog/" slug) title)]]
    [:div.col-25.article-list-item-byline (xform-time-to-string created)]]
   [:div.row
    (if-let [thumb (first media)]
      (list
        [:div.col-25-sm
         (image {:class "article-list-item-media"} (:src thumb))]
        [:div.col-75-sm description])
      [:div.col-100 description])]
   ])

(defn tag-and-count
  [tag]
  [:span
   (link-to-tag (first tag))
   (str "(" (second tag) ")")])

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

(defn dropdown-field
  "Renders a dropdown select input"
  [name values default-value]
  (drop-down name values default-value))

(defn roles-list
  "Render a list of roles"
  [roles]
  (divided-list roles identity "·" "roles"))


(defn user-item
  "Render a user's details"
  [{:keys [username first-name last-name email twitter-handle roles created]}]
  (list
   [:div.row
    [:div.col-25 "Username: " username]]
   [:div.row
    [:div.col-50 "First Name: " first-name]
    [:div.col-50 "Last Name: " last-name]]
   [:div.row
    [:div.col-50 "Email: " (mail-to email email)]
    (when twitter-handle [:div.col-50 "Twitter: " (link-to (str "https://twitter.com/" twitter-handle) twitter-handle)])]
   [:div.row
    [:div.col-25 "Roles: " (roles-list roles)]]
   [:div.row
    [:div.col-25 "Created: " (xform-time-to-string created)]]))

(defn date-field
  "Renders a date input"
  [name value]
  [:input {:name name :type "date" :value value}])

(defn thumb-bar
  "Renders an image bar of thumbnails"
  [name media]
  [:div.imagebar
   (map (fn [{:keys [filename src]}] (image {:draggable true :ondragstart "mango.drag(event)" :filename filename} src filename)) media)])


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
   (when (and prev-page (> prev-page 0)) (link-to (str "/blog/media?page=" prev-page "&per-page=" per-page) "< Previous "))
   (when (and media (> (count media) 0)) (link-to (str "/blog/media?page=" next-page "&per-page=" per-page) " Next >"))))
