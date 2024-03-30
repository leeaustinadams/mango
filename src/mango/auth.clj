(ns mango.auth
  (:require [mango.dataprovider :as dp]
            [crypto.password.pbkdf2 :as password]
            [clj-time.core :refer [now]]))

(defn encrypt-user-password
  "Add/replace the :password field with its encrypted form of password"
  [user plaintext-password]
  (when user (assoc user :password (password/encrypt plaintext-password))))

(defn user
  "Authenticate the user with username and password"
  [data-provider username plaintext-password]
  (when-let [user (dp/user-by-username data-provider username)]
    (let [encrypted-password (:password user)]
      (when (and (not (nil? encrypted-password)) (password/check plaintext-password encrypted-password)) user))))

(defn check-password
  "Checks that plaintext-password is the user's password"
  [data-provider username plaintext-password]
  (when-let [user (dp/user-by-username data-provider username)]
    (let [encrypted-password (:password user)]
      (password/check plaintext-password encrypted-password))))

(defn set-password
  [data-provider user-id plaintext-password]
  (dp/update-user data-provider {:_id user-id :password (password/encrypt plaintext-password)}))

(defn new-user
  "Add a new user"
  ([data-provider username first-name last-name email plaintext-password roles]
   (let [user {:username username
               :first-name first-name
               :last-name last-name
               :email email
               :password plaintext-password
               :roles roles}]
     (new-user data-provider user)))
  ([data-provider user]
   (when (not (dp/user-by-username data-provider (:username user)))
     (dp/insert-user data-provider (assoc user :password (password/encrypt (:password user)) :created (now))))))

(defn private-user
  "Remove fields that should usually not be necessary internally"
  [user]
  (dissoc user :password :provider))

(defn public-user
  "Remove fields that should never be visible externally"
  [user]
  (dissoc (private-user user) :_id :email :resetPasswordToken :resetPasswordExpires))

(defn authorized?
  "Return true if the user has role specified by permission"
  [user permission]
  (some #(= permission %) (:roles user)))

(defn editor?
  "Returns true if the user has the editor role"
  [user]
  (authorized? user "editor"))

(defn admin?
  "Returns true if the user has the admin role"
  [user]
  (authorized? user "admin"))
