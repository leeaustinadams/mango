db = db.getSiblingDB("mango")
db.createUser({user: "mangomango", pwd: "mangopassword", roles: [{role: "readWrite", db: "mango"}]})
