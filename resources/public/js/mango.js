// https://docs.angularjs.org/api
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
		                          return $resource('/blog/articles.json?per-page=12');
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
                                     }
                                 });
	                         }
                            ])
    .factory('FamilyArticles', ['$resource',
	                            function($resource) {
		                            return $resource('/family/articles.json?per-page=12');
	                            }
                               ])
    .factory('FamilyArticle', ['$resource',
	                           function($resource) {
		                           return $resource('/family/articles/:articleId.json', {
			                           articleId: '@_id'
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
    .controller('BlogArticlesController', function($scope, $routeParams, BlogArticles) {
        $scope.name = 'BlogArticlesController';
        $scope.params = $routeParams;
        $scope.articles = BlogArticles.query();
    })
    .controller('BlogArticleController', function($scope, $routeParams, $location, BlogArticle) {
        $scope.name = 'BlogArticleController';
        $scope.params = $routeParams;

        $scope.resetArticle = function() {
            $scope.article = {};
            $scope.article.title = 'Put your title here';
            $scope.article.content = 'Put your content here';
            $scope.article.created = new Date();
            $scope.article.tags = [];
            $scope.article.status = 'draft';
        }

        if ($scope.params.id) {
            $scope.article = BlogArticle.get({
                articleId: $scope.params.id
            });
        } else {
            $scope.resetArticle();
        }
        
        $scope.post = function() {
            var article = new BlogArticle(this.article);
			article.$save(function(response) {
				$location.path('blog/' + response._id);

                $scope.resetArticle();
			}, function(errorResponse) {
				$scope.error = errorResponse.data.message;
			});
        }
    })
    .controller('FamilyArticlesController', function($scope, $routeParams, FamilyArticles) {
        $scope.name = 'FamilyArticlesController';
        $scope.params = $routeParams;
        $scope.articles = FamilyArticles.query(function() {}, function(){
            $scope.$location.path('/unauthorized');
        });
    })
    .controller('FamilyArticleController', function($scope, $routeParams, FamilyArticle) {
        $scope.name = 'FamilyArticleController';
        $scope.params = $routeParams;

        $scope.article = FamilyArticle.get({
            articleId: $scope.params.id
        });
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
        $locationProvider.hashPrefix('!');

        $mdThemingProvider.theme('default').primaryPalette('brown').accentPalette('red');
        
        $routeProvider.when('/blog', {
            templateUrl: '/html/blog_articles.html',
            controller: 'BlogArticlesController'
        }).when('/blog/post', {
            templateUrl: '/html/blog_post.html',
            controller: 'BlogArticleController'
        }).when('/blog/:id', {
            templateUrl: '/html/blog_article.html',
            controller: 'BlogArticleController'
        }).when('/family', {
            templateUrl: '/html/family_articles.html',
            controller: 'FamilyArticlesController'
        }).when('/family/:id', {
            templateUrl: '/html/family_article.html',
            controller: 'FamilyArticleController'
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
    }]);
