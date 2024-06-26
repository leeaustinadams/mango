# mango

```
    __  ___
   /  |/  /___ _____  ____ _____
  / /|_/ / __ `/ __ \/ __ `/ __ \
 / /  / / /_/ / / / / /_/ / /_/ /
/_/  /_/\__,_/_/ /_/\__, /\____/
                   /____/
```

A blog written in Clojure for learning Clojure... and blogging.

## Configuring
You can configure three profiles: `dev`, `test`, and `prod`. Each profile can have separate urls, databases, and other fields configured to isolate them from each other.

### config.edn
Each profile, `dev`, `test`, and `prod` can be configured by editing its corresponding `config.edn` file. You'll probably want to specify different article, page, media, and users collections among the different configurations in order to separate the data you develop and test with, from your production data.

```
{
 :db-name "blog"
 :db-article-collection "articles"
 :db-page-collection "pages"
 :db-media-collection "media"
 :db-users-collection "users"
 :site-url "http://localhost"
 :site-title "My Mango Site"
 :site-description "A site I made with mango"
 :site-copyright "My copyright message"
 :admin-email "your-email@your-domain.com"
 :port 8080
 :logo-url "https://s3-us-west-1.amazonaws.com/{your bucket}/logo.png"
 :aws-media-bucket "{your bucket}"
 :aws-region "us-west-1"
 :cdn-url "https://s3-us-west-1.amazonaws.com/{your bucket}/blog/"
 :app-css "/css/mango.css"
 :app-js "/cljs-out/dev-main.js"}
 :analytics-enabled false
 :google-analytics-id "{your google analytics id}"
 :ads-enabled false
 :google-ad-client "{your google client id}"
 :google-ad-slot "{your google ad slot id}"
```

#### CDN / Media Storage
You'll need to supply your Amazon credentials and S3 bucket

#### Database
Mango is currently strictly tied to mongoDB and you must have an instance running that you can connect to.

## Developing
Running locally for development:

### Server
Run the server from the command line with `$ lein with-profile dev run`. If you're only developing the back end you can stop here and simply open a browser window to the url of your server (http://localhost:8080 by default).

### Client
In order to develop the front end with [clojurescript](https://clojurescript.org), you can fire up [figwheel](https://figwheel.org) and have all the interactive development (REPL, live code reloading, etc) you could wish for.

You can run the client code with figwheel watching for live updates with `$ lein fig`. If you want to use the command line REPL you can use `$ lein fig-repl`

I use the CIDER REPL from [Emacs](https://www.gnu.org/s/emacs).

The `dev.cljs.edn` file configures the development build and figwheel-main to connect to the running server above:

```
^{:open-url "http://localhost:8080"}
{:main mango.core}
```
To start CIDER:

- `M-x cider-jack-in-cljs`
- When asked what type of ClojureScript REPL to start, choose `figwheel-main`
- When asked the name of the build choose `dev`. NOT  `:dev` and NOT the default!

The REPL will eventually come up, and load the server url above. Happy hacking!

## Testing
- To run the default tests: `$ lein test`
- To run integration tests (requires database connection): `$ lein test :integration`

## To Cut a Version
`$ lein v update {patch|minor|major}`

## Deploying / Hosting
### Without Docker
Run `$ lein build-server-uberjar` and deploy the resulting `mango-{version}-standalone.jar` file. I just `scp` it to my Amazon EC2 instance and run it with `$ java -jar mango-{version}-standalone.jar`. You could so set it up to run automatically for your environment like [in this example](scripts/mango).

### Building with Docker
`$ lein build-prod-client`
`$ lein build-prod-uberjar`
`$ cp target/mango-{version}-standalone.jar target/mango-standalone.jar`
`$ lein docker build`

I haven't provided the configuration to push Mango to a Docker registry, I'll leave that up to you

### Running with Docker
1. Create secrets/aws-access-key.txt
1. Create secrets/aws-secret-key.txt
1. Create db-root-password.txt
1. Create db-root-username.txt
1. Create mango-db-password.txt
1. Create mango-db-username.txt

The values in these files will be used in configuring the Docker containers and Mango and should never be checked in to source control

## Changelog
2021/11/26

* Provide Dockerfile, docker-compose, and associates scripts and configuration to run Mango and MongoDB in containers

2019/07/16

* Integrate figwheel-main for developing the front end in clojurescript

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
  * [lein-docker](https://github.com/sarnowski/lein-docker)
* [Clojurescript](https://clojurescript.org)
* [Javascript](https://en.wikipedia.org/wiki/JavaScript)
  * [highlight-js](https://highlightjs.org/)

## License

See [LICENSE.txt](LICENSE.txt)
