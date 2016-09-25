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
angular.module('mango', ['ui.router',
                         'ngMaterial',
                         'ngResource',
                         'ngSanitize',
                         'angulartics',
                         'angulartics.google.analytics',
                         'angular-google-adsense',
                         'ngFileUpload',
                         'ncy-angular-breadcrumb'])
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
    .directive('pageTitle', function() {
        return {
            restrict: 'EA',
            link: function($scope, $element) {
                var el = $element[0];
                el.hidden = true; // So the text not actually visible on the page

                var text = function() {
                    return el.innerHTML;
                };
                var setTitle = function(title) {
                    document.title = title;
                };
                $scope.$watch(text, setTitle);
            }
        };
    })
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
    .controller('MainController', function($scope, $location, $window) {
        $scope.name = 'MainController';
        $scope.$location = $location;

        $scope.go = function(url) {
            $window.location.href = url;
        };
    })
    .controller('LandingController', function($scope, $state) {
        $scope.$state = $state;
    })
    .controller('BlogArticlesController', function($scope, $state, $location, BlogArticles, Authentication, Authorization, params) {
        $scope.$state = $state;

        $scope.authentication = Authentication;
        $scope.authorization = Authorization;

        if (params.mode == "drafts") {
            $scope.articles = BlogArticles.drafts();
            $scope.article_click = function(article_id) {
                $state.go('edit', {id: article_id});
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
                $state.go('article', {id: article_id});
            }
            $scope.subheader = "Posts";
        }
    })
    .controller('BlogArticleController', function($scope, $location, BlogArticle, Upload, params) {
        $scope.name = 'BlogArticleController';

        $scope.post = function() {
            var article = new BlogArticle({
                _id: this.article._id,
                title: this.article.title,
                description: this.article.description,
                content: this.article.content,
                created: this.article.created.toISOString(),
                media: this.article.media,
                tags: this.article.tags,
                status: this.article.status
            });
            $scope.uploadFiles(article, $scope.files);
        }

        $scope.uploadFiles = function (article, files) {
            if (files && files.length) {
                if (params.mode != 'edit') {
                    delete article._id;
                }
                article.files = files;
                Upload.upload({
                    url: params.mode == 'edit' ? 'blog/articles/' + article._id + '.json' : 'blog/articles/post.json',
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
            if (params.mode == "edit") {
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

        if (params.id) {
            $scope.article = BlogArticle.get({
                articleId: params.id
            }, function(article) {
                article.created = new Date(article.created);
                article.createdString = article.created.toLocaleDateString();
                article.renderedContent = article["rendered-content"];
            });
        } else {
            $scope.resetArticle();
        }
    })
    .controller('UserController', function($scope, $location, User, Authentication, params) {
        $scope.name = 'UserController';

        $scope.authentication = Authentication;
        if (params.mode == 'me') {
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
    .config(['$locationProvider', '$urlRouterProvider', '$stateProvider', '$mdThemingProvider', '$httpProvider', '$breadcrumbProvider',
             function($locationProvider, $urlRouterProvider, $stateProvider, $mdThemingProvider, $httpProvider, $breadcrumbProvider) {
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

        $urlRouterProvider.otherwise('/')

        $stateProvider.state({
            name: 'landing',
            url: '/',
            templateUrl: '/html/landing.html',
            controller: 'LandingController',
            ncyBreadcrumb: {
                label: 'Home'
            }
        }).state({
            name: 'blog',
            url: '/blog',
            templateUrl: '/html/blog_articles.html',
            controller: 'BlogArticlesController',
            resolve: {
                params: function() {
                    return {
                        mode: 'published'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Blog',
                parent: 'landing'
            }
        }).state({
            name: 'drafts',
            url: '/blog/drafts',
            templateUrl: 'html/blog_articles.html',
            controller: 'BlogArticlesController',
            resolve: {
                params: function() {
                    return {
                        mode: 'drafts'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Drafts',
                parent: 'blog'
            }
        }).state({
            name: 'post',
            url: '/blog/post',
            templateUrl: '/html/blog_post.html',
            controller: 'BlogArticleController',
            resolve: {
                params: function() {
                    return {
                        mode: 'new'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'New Article',
                parent: 'blog'
            }
        }).state({
            name: 'article',
            url: '/blog/{id}',
            templateUrl: '/html/blog_article.html',
            controller: 'BlogArticleController',
            resolve: {
                params: function($stateParams) {
                    return {
                        id: $stateParams.id,
                        mode: 'show'
                    };
                }
            },
            ncyBreadcrumb: {
                label: '{{article.title}}',
                parent: 'blog'
            }
        }).state({
            name: 'edit',
            url: '/edit/{id}',
            templateUrl: '/html/blog_post.html',
            controller: 'BlogArticleController',
            resolve: {
                params: function($stateParams) {
                    return {
                        id: $stateParams.id,
                        mode: 'edit'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Edit Article',
                parent: 'article'
            }
        }).state({
            name: 'about',
            url: '/about',
            templateUrl: '/html/about.html',
            ncyBreadcrumb: {
                label: 'About',
                parent: 'landing'
            }
        }).state({
            name: 'photography',
            url:'/photography',
            templateUrl: '/html/photography.html',
            ncyBreadcrumb: {
                label: 'Photography',
                parent: 'landing'
            }
        }).state({
            name: 'me',
            url: '/me',
            templateUrl: '/html/user.html',
            controller: 'UserController',
            resolve: {
                params: function() {
                    return {
                        mode: 'me'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Me',
                parent: 'landing'
            }
        }).state({
            name: 'signin',
            url: '/signin',
            templateUrl: '/html/signin.html',
            controller: 'UserController',
            resolve: {
                params: function() {
                    return {
                        mode: 'signin'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Sign In',
                parent: 'landing'
            }
        }).state({
            name: 'signout',
            url: '/signout',
            templateUrl: '/html/signout.html',
            controller: 'UserController',
            resolve: {
                params: function() {
                    return {
                        mode: 'signout'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Sign Out',
                parent: 'landing'
            }
        }).state({
            name: 'unauthorized',
            url: '/unauthorized',
            templateUrl: '/html/unauthorized.html'
        });

        $locationProvider.html5Mode(true);

        $breadcrumbProvider.setOptions({
            template: 'bootstrap2'
        });
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
