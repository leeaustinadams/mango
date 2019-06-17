# mango

A blog written in Clojure for learning Clojure... and blogging.

## To Cut a Version
`$ lein v update {patch|minor|major}`

## Configuring
You can configure three profiles: `dev`, `test`, and `prod`. Each profile can have separate urls, databases, and other fields configured to isolate them from each other.

### Database
Mango is currently strictly tied to mongoDB and you must have an instance running that you can connect to.

## Developing
Running locally for development: `$ lein with-profile dev run`.

## Testing
- To run the default tests: `$ lein test`
- To run integration tests (requires database connection): `$ lein test :integration`

## Deploying / Hosting
For now I just build an uberjar and run it on an instance somewhere: `$ lein with-profile prod uberjar`

## Changelog
**v0.1**
2019/02/19

* First commit in prep for open source. Hope all my secrets are scrubbed!

## Acknowledgements
Built with
* [Clojure](https://clojure.org)
  * [ring](http://ring-clojure.github.io/ring/)
  * [compojure](https://github.com/weavejester/compojure)
  * [crypto-password](https://github.com/weavejester/crypto-password)
  * [monger](http://clojuremongodb.info/)
  * [stencil](https://github.com/davidsantiago/stencil)
  * [cheshire](https://github.com/dakrone/cheshire)
  * [amazonica](https://github.com/mcohen01/amazonica)
  * [markdown-clj](https://github.com/yogthos/markdown-clj)
  * [leiningen](http://leiningen.org/)
* [Javascript](https://en.wikipedia.org/wiki/JavaScript)
  * [highlight-js](https://highlightjs.org/)

## License

See [LICENSE.txt](LICENSE.txt)
