// License(s)
// Portions of this code are:
// Copyright (c) djd4rce@gmail.com
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
// Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// Portions of this code are:
// Copyright (c) lee@4d4ms.com
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
// Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

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
    .directive('articleContent', function () {
        return {
            replace: false,
            scope: {
                'ngBindHtml': '='
            },
            link: function (scope, element, attrs) {
                scope.$watch('ngBindHtml', function(newValue, oldValue) {
                    element.html(newValue);
                    var items = element[0].querySelectorAll('code');
                    angular.forEach(items, function (item) {
                        hljs.highlightBlock(item);
                    });
                    window.twttr.ready(function() {
                        twttr.widgets.load(element);
                    });
                });
            }
        };
    })
    .directive('tweet', ['$timeout', function($timeout) {
        return {
            link: function(scope, element, attr) {
                var renderTwitterButton = _.debounce(function() {
                    if (attr.url && attr.text) {
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
                }, 150);
                attr.$observe('url', renderTwitterButton);
                attr.$observe('text', renderTwitterButton);
            }
        };
    }])
    .directive('follow', ['$timeout', function($timeout) {
        return {
            link: function(scope, element, attr) {
                var renderTwitterFollow = _.debounce(function() {
                    if (attr.text) {
                        $timeout(function() {
                            element[0].innerHTML = '';
                            twttr.widgets.createFollowButton(
                                attr.text,
                                element[0],
                                {});
                        });
                    }
                }, 150);
                attr.$observe('text', renderTwitterFollow);
            }
        };
    }])
    .directive('pageTitle', function() {
        return {
            restrict: 'EA',
            link: function($scope, $element) {
                var el = $element[0];
                el.hidden = true;

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
    .factory('Util', [function() {
        var _this = this;

        _this._data = {
            cleanArticle: function(article) {
                article.created = new Date(article.created);
                article.createdString = article.created.toLocaleDateString();
                article.renderedContent = article["rendered-content"];
            }
        };

        return _this._data;
    }])
    .factory('LogEvent', [
        '$resource', function($resource) {
            return $resource('/log/event');
        }
    ])
    .factory('$exceptionHandler', [
        '$log', '$window', '$injector',
        function($log, $window, $injector) {
            return function myExceptionHandler(exception, cause) {
                $log.warn(exception, cause);
                $injector.get('$http').post("/log/event", {
                    errorUrl: $window.location.href,
                    category: "exception",
                    event: exception,
                    cause: ( cause || "" )
                });
            };
        }
    ])
    .factory('BlogArticles', [
        '$resource', function($resource) {
            return $resource('/blog/articles.json?per-page=20', {
            }, {
                drafts: {
                    method: 'GET',
                    url: '/blog/drafts/articles.json?per-page=20',
                    isArray: true
                },
                tagged: {
                    method: 'GET',
                    url: '/blog/articles.json?tagged=:tag&per-page=20',
                    isArray: true
                }
            });
        }
    ])
    .factory('BlogArticle', [
        '$resource', function($resource) {
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
    .factory('User', [
        '$resource', function($resource) {
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
    .controller('MainController', [
        '$scope', '$window', function($scope, $window) {
            $scope.navigate = function(url) {
                $window.location.href = url;
            };
        }
    ])
    .controller('LandingController', [
        '$scope', '$state',
        function($scope, $state) {
            $scope.$state = $state;
        }
    ])
    .controller('BlogArticlesController', [
        '$scope', '$state', '$timeout', '$resource', 'BlogArticles', 'Authentication', 'Authorization', 'Util', 'params',
        function($scope, $state, $timeout, $resource, BlogArticles, Authentication, Authorization, Util, params) {
            $scope.$state = $state;
            $scope.mode = params.mode;

            $scope.authentication = Authentication;
            $scope.authorization = Authorization;

            var errorHandler = function(errorResponse) {
                $state.go('landing');
            };

            if ($scope.mode == "drafts") {
                $scope.articles = BlogArticles.drafts(function(articles) { }, errorHandler);
                $scope.article_click = function(article_id) {
                    $state.go('edit', {id: article_id});
                };
                $scope.subheader = "Drafts";
            } else {
                if ($scope.mode == "tagged") {
                    $scope.articles = BlogArticles.tagged({tag: params.tag}, function(articles) {
                    }, errorHandler);
                    $scope.subheader = "Tagged \"" + params.tag + "\"";
                } else {
                    $scope.articles = BlogArticles.query(function(articles) {
                        return _.map(articles, Util.cleanArticle);
                    }, errorHandler);
                    $scope.subheader = "Posts";
                }

                $scope.article_click = function(article_id) {
                    $state.go('article', {id: article_id});
                };
            }
        }
    ])
    .controller('BlogArticleController', [
        '$scope', '$state', 'Authentication', 'BlogArticle', 'Upload', 'Util', 'params',
        function($scope, $state, Authentication, BlogArticle, Upload, Util, params) {
            $scope.$state = $state;
            $scope.authentication = Authentication;
            $scope.mode = params.mode;

            $scope.edit = function() {
                $state.go('edit', {id: params.id});
            };

            $scope.tag_click = function(tag) {
                $state.go('tagged', {tag: tag});
            };

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

                if (!article.media) { delete article.media; }
                if (!article.tags) { delete article.tags; }

                if (article.media) {
                    for (m = 0; m < article.media.length; m++) {
                        article.media[m] = article.media[m]._id;
                    }
                }

                if ($scope.files && $scope.files.length) {
                    Upload.upload({
                        url: 'blog/media.json',
                        data: {files: $scope.files}
                    }).then(function (response) {
                        console.log('Success Response: ' + response.data);
                        article.media = article.media || [];
                        for (i = 0; i < response.data.length; i++) {
                            article.media[article.media.length] = response.data[i];
                        }
                        $scope.postArticle(article);
                    }, function (response) {
                        $scope.error = response.status + " " + response.statusText;
                        console.log('Error status: ' + response.status);
                    }, function (event) {
                        var progressPercentage = parseInt(100.0 * event.loaded / event.total);
                        console.log('progress: ' + progressPercentage + '%');
                    });
                } else {
                    $scope.postArticle(article);
                }
            };

            $scope.postArticle = function(article) {
                if ($scope.mode == "edit") {
                    article.$edit(function(response) {
                        $scope.resetArticle();
                        $state.go('article', {id: response._id});
                    }, function(errorResponse) {
                        $scope.error = errorResponse.data.msg;
                    });
                } else {
                    delete article._id;
                    article.$save(function(response) {
                        $scope.resetArticle();
                        $state.go('article', {id: response._id});
                    }, function(errorResponse) {
                        $scope.error = errorResponse.data.msg;
                    });
                }

            };

            $scope.resetArticle = function() {
                $scope.article = {};
                $scope.article.title = '';
                $scope.article.description = '';
                $scope.article.content = '';
                $scope.article.created = new Date();
                $scope.article.tags = '';
                $scope.article.status = 'draft';
            };

            if (params.id) {
                $scope.article = BlogArticle.get({
                    articleId: params.id
                }, function(article) {
                    return Util.cleanArticle(article);
                }, function(error) {
                    $state.go('blog');
                });
            } else {
                $scope.resetArticle();
            }
        }
    ])
    .controller('UserController', [
        '$scope', '$state', 'User', 'Authentication', 'params',
        function($scope, $state, User, Authentication, params) {
            $scope.name = 'UserController';
            $scope.mode = params.mode;

            $scope.authentication = Authentication;
            if ($scope.mode == 'me') {
                $scope.user = Authentication.user;
            }

            $scope.signin = function() {
                var user = new User({username: this.user.name, password: this.user.password});
                user.$signin(function(response) {
                    $scope.authentication.user = response;
                    $state.go('landing');
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.msg;
                });
            };

            $scope.signout = function() {
                var user = new User($scope.authentication.user);
                user.$signout(function(response) {
                    $scope.authentication.user = null;
                    $state.go('landing');
                }, function(errorResponse) {
                    $scope.error = errorResponse.data.msg;
                });
            };
        }
    ])
    .config(['$locationProvider', '$urlRouterProvider', '$stateProvider', '$mdThemingProvider', '$httpProvider', '$breadcrumbProvider',
             function($locationProvider, $urlRouterProvider, $stateProvider, $mdThemingProvider, $httpProvider, $breadcrumbProvider) {
        $mdThemingProvider.theme('default').primaryPalette('blue').accentPalette('red');

        hljs.configure({languages: ["clj", "c", "java", "html", "js"]});

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

        $urlRouterProvider.otherwise('/');

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
            name: 'tagged',
            url: '/blog/tagged/{tag}',
            templateUrl: 'html/blog_articles.html',
            controller: 'BlogArticlesController',
            resolve: {
                params: function($stateParams) {
                    return {
                        tag: $stateParams.tag,
                        mode: 'tagged'
                    };
                }
            },
            ncyBreadcrumb: {
                label: 'Tagged',
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
            controller: 'MainController',
            ncyBreadcrumb: {
                label: 'Photography',
                parent: 'landing'
            }
        }).state({
            name: 'me',
            url: '/me',
            templateUrl: '/html/user_view.html',
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