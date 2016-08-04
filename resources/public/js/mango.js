angular.module('mango', ['ngRoute', 'ngMaterial', 'ngResource'])
.factory('BlogArticles', ['$resource',
	function($resource) {
		return $resource('/blog/articles.json?per-page=12');
	}
])
.factory('BlogArticle', ['$resource',
	function($resource) {
		return $resource('/blog/articles/:articleId.json', {
			articleId: '@_id'
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
.config(function($routeProvider, $locationProvider, $mdThemingProvider) {
    $routeProvider.when('/blog', {
        templateUrl: '/html/family_articles.html',
        controller: 'BlogArticlesController'
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
    }).when('/unauthorized', {
        templateUrl: '/html/unauthorized.html'
    }).otherwise({
        templateUrl: '/html/landing.html',
        controller: 'LandingController'
    });

    $locationProvider.html5Mode(true);

    $mdThemingProvider.theme('default').primaryPalette('brown').accentPalette('red');
})
