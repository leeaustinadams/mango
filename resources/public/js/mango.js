// https://docs.angularjs.org/api
function formatArticle(article) {
    var cleanArticle = {};
    cleanArtucke._id = article._id;
    cleanArticle.title = article.title;
    cleanArticle.content = article.content;
    var created = new Date(article.created);
    cleanArticle.created = created;
    cleanArticle.created_string = created.getDate() + '/' + created.getMonth() + '/' + created.getFullYear();
    cleanArticle.tags = article.tags;
    cleanArticle.status = article.status;
    return cleanArticle;
}

angular.module('mango', ['ngRoute', 'ngMaterial', 'ngResource'])
    .config(['$httpProvider', function($httpProvider) {
        $httpProvider.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded";
        $httpProvider.defaults.transformRequest.unshift(function (data, headersGetter) {
            var key, result = [], response;
            if(typeof data == "string") { //$http support
                response = data;
            } else {
                for (key in data) {
                    if (data.hasOwnProperty(key)) {
                        result.push(encodeURIComponent(key) + "=" + encodeURIComponent(data[key]));
                    }
                }
                response = result.join("&");
            }
            return response;
        });
    }])
    .factory('BlogArticles', ['$resource',
	                          function($resource) {
		                          return $resource('/blog/articles.json?per-page=12', {
                                  }, {
                                      drafts: {
                                          method: 'GET',
                                          url: '/blog/drafts.json?per-page=12',
                                          isArray: true
                                      }
                                  });
                              }
                             ])
    .factory('BlogArticle', ['$resource',
	                         function($resource) {
		                         return $resource('/blog/articles/:articleId.json', {
                                     articleId: '@_id'
                                 }, {
                                     save: {
                                         method: 'POST',
                                         url: '/blog/articles/post.json'
                                     },
                                     edit: {
                                         method: 'POST',
                                         url: '/blog/articles/:articleId.json'
                                     }
                                 });
	                         }
                            ])
    .factory('User', ['$resource',
                      function($resource) {
                          return $resource('/users/:userId.json', {
                              userId: '@_id'
                          }, {
                              signin: {
                                  method: 'POST',
                                  url: '/auth/signin'
                              }
                          });
                      }])
    .controller('MainController', function($scope, $route, $routeParams, $location, $window) {
        $scope.name = 'MainController';
        $scope.$route = $route;
        $scope.$routeParams = $routeParams;
        $scope.$location = $location;

        $scope.go = function(url) {
            $window.location.href = url;
        };
    })
    .controller('LandingController', function($scope, $location) {
        $scope.$location = $location;
    })
    .controller('BlogArticlesController', function($scope, $routeParams, $location, BlogArticles, mode) {
        $scope.name = 'BlogArticlesController';
        $scope.params = $routeParams;

        if (mode == "drafts") {
            $scope.articles = BlogArticles.drafts();
            $scope.article_click = function(article_id) {
                $location.path('/edit/' + article_id);
            }
            $scope.subheader = "Drafts";
        } else {
            $scope.articles = BlogArticles.query(function(articles) {
                for(var i = 0; i < articles.length; i++) {
                    var article = articles[i];
                    article = formatArticle(article);
                }
            });
            $scope.article_click = function(article_id) {
                $location.path('/blog/' + article_id);
            }
            $scope.subheader = "Posts";
        }
    })
    .controller('BlogArticleController', function($scope, $routeParams, $location, BlogArticle, mode) {
        $scope.name = 'BlogArticleController';
        $scope.params = $routeParams;

        $scope.post = function() {
            var article = new BlogArticle({
                _id: this.article._id,
                title: this.article.title,
                content: this.article.content,
                created: this.article.created,
                tags: this.article.tags,
                status: this.article.status
            });
            if (mode == "edit") {
                article.$edit(function(response) {
                    $location.path('blog/' + response._id);

                    $scope.resetArticle();
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.message;
                });
            } else {
                article.$save(function(response) {
                    $location.path('blog/' + response._id);

                    $scope.resetArticle();
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.message;
                });
            }
        }

        $scope.resetArticle = function() {
            $scope.article = {};
            $scope.article.title = '';
            $scope.article.content = '';
            $scope.article.created = new Date();
            $scope.article.tags = '';
            $scope.article.status = 'draft';
        }

        if ($scope.params.id) {
            $scope.article = BlogArticle.get({
                articleId: $scope.params.id
            }, function(article) {
                article = formatArticle(article);
            });
        } else {
            $scope.resetArticle();
        }
    })
    .controller('UserController', function($scope, $routeParams, User) {
        $scope.name = 'UserController';
        $scope.params = $routeParams;

        $scope.user = {};

        $scope.signin = function() {
            var user = new User({username: this.user.name, password: this.user.password});
            user.$signin(function(response) {
            }, function(errorResponse) {
            })
        }
    })
    .config(['$locationProvider', '$routeProvider', '$mdThemingProvider', function($locationProvider, $routeProvider, $mdThemingProvider) {
        $mdThemingProvider.theme('default').primaryPalette('blue').accentPalette('red');
        
        $routeProvider.when('/blog', {
            templateUrl: '/html/blog_articles.html',
            controller: 'BlogArticlesController',
            resolve: {
                mode: function() {
                    return 'published';
                }
            }
        }).when('/blog/drafts', {
            templateUrl: 'html/blog_articles.html',
            controller: 'BlogArticlesController',
            resolve: {
                mode: function() {
                    return 'drafts';
                }
            }
        }).when('/blog/post', {
            templateUrl: '/html/blog_post.html',
            controller: 'BlogArticleController'
        }).when('/blog/:id', {
            templateUrl: '/html/blog_article.html',
            controller: 'BlogArticleController',
            resolve: {
                mode: function() {
                    return 'show';
                }
            }
        }).when('/edit/:id', {
            templateUrl: '/html/blog_post.html',
            controller: 'BlogArticleController',
            resolve: {
                mode: function() {
                    return 'edit';
                }
            }
        }).when('/about', {
            templateUrl: '/html/about.html'
        }).when('/photography', {
            templateUrl: '/html/photography.html'
        }).when('/signin', {
            templateUrl: '/html/signin.html',
            controller: 'UserController'
        }).when('/unauthorized', {
            templateUrl: '/html/unauthorized.html'
        }).otherwise({
            templateUrl: '/html/landing.html',
            controller: 'LandingController'
        });

        $locationProvider.html5Mode(true);
    }]);
