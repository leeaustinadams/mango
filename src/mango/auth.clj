(ns mango.auth
  (:require [mango.db :as db]
            [crypto.password.pbkdf2 :as password]))

(defn user
  "Authenticate the user with username and password"
  [username plaintext-password]
  (when-let [user (db/user-by-username username)]
    (let [encrypted-password (:password user)]
      (when (and (not (nil? encrypted-password)) (password/check plaintext-password encrypted-password)) user))))

(defn encrypt-user-password
  "Add/replace the :password field with its encrypted form of password"
  [user plaintext-password]
  (when user (assoc user :password (password/encrypt plaintext-password))))

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
