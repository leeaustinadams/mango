// License(s)
// Portions of this code are:
// Copyright (c) djd4rce@gmail.com
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// Portions of this code are:
// Copyright (c) lee@4d4ms.com
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// https://docs.angularjs.org/api
angular.module('mango', ['ngRoute', 'ngMaterial', 'ngResource', 'ngSanitize', 'angulartics', 'angulartics.google.analytics', 'angular-google-adsense', 'ngFileUpload'])
    .directive('highlight', function () {
        return {
            replace: false,
            scope: {
                'ngBindHtml': '='
            },
            link: function (scope, element, attrs) {
                scope.$watch('ngBindHtml', function(newValue, oldValue) {
                    element.html(newValue);
                    var items = element[0].querySelectorAll('code,pre');
                        angular.forEach(items, function (item) {
                            hljs.highlightBlock(item);
                        });
                    }
                );
            },
        };
    })
    .directive('tweet', ['$timeout', function($timeout) {
        return {
            link: function(scope, element, attr) {
				var renderTwitterButton = debounce(function() {
					if (attr.url) {
						$timeout(function() {
							element[0].innerHTML = '';
							twttr.widgets.createShareButton(
								attr.url,
								element[0],
								function() {}, {
									count: attr.count,
									text: attr.text,
									via: attr.via,
									size: attr.size
								}
							);
						});
					}
				}, 75);
				attr.$observe('url', renderTwitterButton);
				attr.$observe('text', renderTwitterButton);
			}
        }
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
                              },
                              signout: {
                                  method: 'POST',
                                  url: '/auth/signout'
                              }
                          });
                      }])
    .factory('Authentication', [function() {
		var _this = this;

		_this._data = {
			user: window.user
		};

		return _this._data;
	}])
    .factory('Authorization', [function() {
        var _this = this;

        _this._data = {
            canEdit: function(user, item, role) {
                var itemUserId = item && item.user && item.user._id;

                return user && (user._id === itemUserId) ||
                    (user.roles && user.roles.indexOf(role) >= 0);
            }
        };

        return _this._data;
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
    .controller('BlogArticlesController', function($scope, $routeParams, $location, BlogArticles, Authentication, Authorization,  mode) {
        $scope.name = 'BlogArticlesController';
        $scope.params = $routeParams;

        $scope.authentication = Authentication;
        $scope.authorization = Authorization;

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
                    article.created = new Date(article.created);
                    article.createdString = article.created.toLocaleDateString();
                    article.renderedContent = article["rendered-content"];
                }
            });
            $scope.article_click = function(article_id) {
                $location.path('/blog/' + article_id);
            }
            $scope.subheader = "Posts";
        }
    })
    .controller('BlogArticleController', function($scope, $routeParams, $location, BlogArticle, Upload, mode) {
        $scope.name = 'BlogArticleController';
        $scope.params = $routeParams;

        $scope.post = function() {
            var article = new BlogArticle({
                _id: this.article._id,
                title: this.article.title,
                description: this.article.description,
                content: this.article.content,
                created: this.article.created,
                media: this.article.media,
                tags: this.article.tags,
                status: this.article.status
            });
            $scope.uploadFiles(article, $scope.files);
        }

        $scope.uploadFiles = function (article, files) {
            if (files && files.length) {
                if (mode != 'edit') {
                    delete article._id;
                }
                article.files = files;
                Upload.upload({
                    url: mode == 'edit' ? 'blog/articles/' + article._id + '.json' : 'blog/articles/post.json',
                    data: article
                }).then(function (response) {
                    console.log('Success Response: ' + response.data);
                    $scope.resetArticle();
                    $location.path('blog/' + response.data._id);
                }, function (response) {
                    $scope.error = response.status;
                    console.log('Error status: ' + response.status);
                }, function (event) {
                    var progressPercentage = parseInt(100.0 * event.loaded / event.total);
                    console.log('progress: ' + progressPercentage + '%');
                });
            } else {
            if (mode == "edit") {
                article.$edit(function(response) {
                        $scope.resetArticle();
                    $location.path('blog/' + response._id);
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.msg;
                });
            } else {
                delete article._id;
                article.$save(function(response) {
                        $scope.resetArticle();
                    $location.path('blog/' + response._id);
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.msg;
                });
            }
        }
        }
        
        $scope.resetArticle = function() {
            $scope.article = {};
            $scope.article.title = '';
            $scope.article.description = '';
            $scope.article.content = '';
            $scope.article.created = new Date();
            $scope.article.tags = '';
            $scope.article.status = 'draft';
        }

        if ($scope.params.id) {
            $scope.article = BlogArticle.get({
                articleId: $scope.params.id
            }, function(article) {
                article.created = new Date(article.created);
                article.createdString = article.created.toLocaleDateString();
                article.renderedContent = article["rendered-content"];
            });
        } else {
            $scope.resetArticle();
        }
    })
    .controller('UserController', function($scope, $routeParams, $location, User, Authentication, mode) {
        $scope.name = 'UserController';
        $scope.params = $routeParams;

        $scope.authentication = Authentication;
        if (mode == 'me') {
            $scope.user = Authentication.user;
        }

        $scope.signin = function() {
            var user = new User({username: this.user.name, password: this.user.password});
            user.$signin(function(response) {
                $scope.authentication.user = response;
                $location.path('/');
            }, function(errorResponse) {
                $scope.error = errorResponse.data.msg;
            });
        }

        $scope.signout = function() {
            var user = new User($scope.authentication.user);
            user.$signout(function(response) {
                $scope.authentication.user = null;
                $location.path('/');
            }, function(errorResponse) {
                $scope.error = errorResponse.data.msg;
            });
        }
    })
    .config(['$locationProvider', '$routeProvider', '$mdThemingProvider', '$httpProvider', function($locationProvider, $routeProvider, $mdThemingProvider, $httpProvider) {
        $mdThemingProvider.theme('default').primaryPalette('blue').accentPalette('red');

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
            controller: 'BlogArticleController',
            resolve: {
                mode: function() {
                    return 'new';
                }
            }
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
        }).when('/me', {
            templateUrl: '/html/user.html',
            controller: 'UserController',
            resolve: {
                mode: function() {
                    return 'me';
                }
            }
        }).when('/signin', {
            templateUrl: '/html/signin.html',
            controller: 'UserController',
            resolve: {
                mode: function() {
                    return 'signin';
                }
            }
        }).when('/signout', {
            templateUrl: '/html/signout.html',
            controller: 'UserController',
            resolve: {
                mode: function() {
                    return 'signout';
                }
            }
        }).when('/unauthorized', {
            templateUrl: '/html/unauthorized.html'
        }).otherwise({
            templateUrl: '/html/landing.html',
            controller: 'LandingController'
        });

        $locationProvider.html5Mode(true);
    }]);

//Simple Debounce Implementation
//http://davidwalsh.name/javascript-debounce-function
function debounce(func, wait, immediate) {
	var timeout;
	return function() {
		var context = this,
			args = arguments;
		var later = function() {
			timeout = null;
			if (!immediate) func.apply(context, args);
		};
		var callNow = immediate && !timeout;
		clearTimeout(timeout);
		timeout = setTimeout(later, wait);
		if (callNow) func.apply(context, args);
	};
};
