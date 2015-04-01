var app = angular.module('messaging', ['ngRoute', 'emguo.poller', 'ngResource'])
    .config(function ($routeProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'home.html',
                controller: 'home'
            })
            .otherwise('/');
    })
    .controller('navigation', function ($rootScope, $scope, $http, $location, $route) {
        $scope.tab = function (route) {
            return $route.current && route === $route.current.controller;
        };
    })
    .controller('home', function ($scope, $http, $resource, poller) {
        $http.get('http://localhost:8080/resource/').success(function (data) {
            $scope.greeting = data;
        });
        $scope.message = function () {
            $http.get('http://localhost:8080/message');
        };
        $scope.sendRedisBackedMessage = function () {
            $http.get('http://localhost:8080/message-redis-backed');
        };
        $scope.sendFile = function () {
            $http.get('http://localhost:8080/message-file');
        };
        $scope.sendIso = function () {
            $http.get('http://localhost:8080/message-iso');
        };
        var messagePoller = poller.get($resource("http://localhost:8080/messages"), {
            delay: 1000
        });
        messagePoller.promise.then(null, null, function (data) {
            $scope.messages = data.messages;
        });
    });