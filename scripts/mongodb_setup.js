// e.g. mongo mongodb_setup.js --username adminUsername --password
db.createUser({user: "mango", pwd: "mango", roles: [{role: "readWrite", db: "mango"}]})
Successfully added user: {
	"user" : "mango",
	"roles" : [
		{
			"role" : "readWrite",
			"db" : "mango"
		}
	]
}
