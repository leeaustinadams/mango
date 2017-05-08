;; https://github.com/weavejester/crypto-password
(ns mango.auth
  (:require [mango.db :as db]
            [crypto.random :as random]
            [crypto.password.pbkdf2 :as password]))

(defn user
  "Authenticate the user with username and password"
  [username password]
  (when-let [user (db/user-by-username username)]
    (let [encrypted-password (:password user)]
      (when (and (not (nil? encrypted-password)) (password/check password encrypted-password)) user))))

(defn update-user-password
  "Add/replace the :password field with its encrypted form of password"
  [user password]
  (when user (assoc user :password (password/encrypt password)))) 

(defn public-user
  "Remove fields that should never be visible externally"
  [user]
  (dissoc user :_id :password :salt :provider :email :resetPasswordToken :resetPasswordExpires))

(defn private-user
  "Remove fields that should usually not be necessary internally"
  [user]
  (dissoc user :password :salt))

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
