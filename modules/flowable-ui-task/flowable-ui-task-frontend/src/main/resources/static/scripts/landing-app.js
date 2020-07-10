/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

var flowableApp = angular.module('flowableLanding', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'mgcrea.ngStrap',
  'ngAnimate',
  'pascalprecht.translate'
]);

var flowableModule = flowableApp;
flowableApp
  // Initialize routes
  .config(['$provide', '$routeProvider', '$selectProvider', '$datepickerProvider', '$translateProvider', function ($provide, $routeProvider, $selectProvider, $datepickerProvider, $translateProvider) {

    var appName = '';
    $provide.value('appName', appName);

    var ctx = FLOWABLE.CONFIG.webContextRoot;
    var appResourceRoot = ctx + (ctx && ctx.charAt(ctx.length - 1) !== '/' ? '/' : '');
    $provide.value('appResourceRoot', appResourceRoot);

    // Override caret for bs-select directive
    angular.extend($selectProvider.defaults, {
        caretHtml: '&nbsp;<i class="icon icon-caret-down"></i>'
    });

    // Override carets for bs-datepicker directive
    angular.extend($datepickerProvider.defaults, {
        iconLeft: 'icon icon-caret-left',
        iconRight: 'icon icon-caret-right'
    });

    $routeProvider
        .when('/', {
            templateUrl: 'views/landing.html',
            controller: 'LandingController'
        })
        .otherwise({
            redirectTo: FLOWABLE.CONFIG.appDefaultRoute || '/'
        });

        // Initialize angular-translate
        $translateProvider.useStaticFilesLoader({
          prefix: './i18n/',
          suffix: '.json'
        })
        /*
        This can be used to map multiple browser language keys to a
        angular translate language key.
        */
        // .registerAvailableLanguageKeys(['en'], {
        //     'en-*': 'en'
        // })
        .useSanitizeValueStrategy('escapeParameters')
        .uniformLanguageTag('bcp47')
        .determinePreferredLanguage();
    }])
    .run(['$rootScope', '$timeout', '$translate', '$location', '$http', '$window', '$popover', 'appResourceRoot', 'RuntimeAppDefinitionService',
        function($rootScope, $timeout, $translate, $location, $http, $window, $popover, appResourceRoot, RuntimeAppDefinitionService) {

        // set angular translate fallback language
        $translate.fallbackLanguage(['en']);

        $rootScope.appResourceRoot = appResourceRoot;

        // Alerts
        $rootScope.alerts = {
            queue: []
        };
        
        $rootScope.webRootUrl = function() {
            return FLOWABLE.CONFIG.webContextRoot;
        };
        
        $rootScope.restRootUrl = function() {
            return FLOWABLE.CONFIG.contextRoot;
        };

        $rootScope.showAlert = function(alert) {
            if(alert.queue.length > 0) {
                alert.current = alert.queue.shift();
                // Start timout for message-pruning
                alert.timeout = $timeout(function() {
                    if(alert.queue.length == 0) {
                        alert.current = undefined;
                        alert.timeout = undefined;
                    } else {
                        $rootScope.showAlert(alert);
                    }
                }, (alert.current.type == 'error' ? 5000 : 1000));
            } else {
                $rootScope.alerts.current = undefined;
            }
        };

        $rootScope.addAlert = function(message, type) {
            var newAlert = {message: message, type: type};
            if(!$rootScope.alerts.timeout) {
                // Timeout for message queue is not running, start one
                $rootScope.alerts.queue.push(newAlert);
                $rootScope.showAlert($rootScope.alerts);
            } else {
                $rootScope.alerts.queue.push(newAlert);
            }
        };

        $rootScope.dismissAlert = function() {
            if(!$rootScope.alerts.timeout) {
                $rootScope.alerts.current = undefined;
            } else {
                $timeout.cancel($rootScope.alerts.timeout);
                $rootScope.alerts.timeout = undefined;
                $rootScope.showAlert($rootScope.alerts);
            }
        };

        $rootScope.addAlertPromise = function(promise, type) {
            if(promise) {
                promise.then(function(data) {
                    $rootScope.addAlert(data, type);
                });
            }
        };

        $rootScope.logout = function () {
            $rootScope.authenticated = false;
            $rootScope.authenticationError = false;
            $http.get(FLOWABLE.CONFIG.contextRoot + '/app/logout')
                .success(function (data, status, headers, config) {
                    $rootScope.login = null;
                    $rootScope.authenticated = false;
                    $window.location.href = '/';
                    $window.location.reload();
                });
        };

        $http.get(FLOWABLE.CONFIG.contextRoot + '/app/rest/account')
        	.success(function (data, status, headers, config) {
              	$rootScope.account = data;
               	$rootScope.invalidCredentials = false;
 				$rootScope.authenticated = true;
          	});

     }])
     .run(['$rootScope', '$location', '$window', '$translate', '$modal',
        function($rootScope, $location, $window, $translate, $modal) {
         
        /* Auto-height */

        $rootScope.window = {};
        var updateWindowSize = function() {
            $rootScope.window.width = $window.innerWidth;
            $rootScope.window.height  = $window.innerHeight;
        };

        // Window resize hook
        angular.element($window).bind('resize', function() {
            $rootScope.$apply(updateWindowSize());
        });

        $rootScope.$watch('window.forceRefresh', function(newValue) {
            if(newValue) {
                $timeout(function() {
                    updateWindowSize();
                    $rootScope.window.forceRefresh = false;
                });
            }
        });

        updateWindowSize();

        /* Capabilities */

        $rootScope.backToLanding = function() {
            var baseUrl = $location.absUrl();
            var index = baseUrl.indexOf('/#');
            if (index >= 0) {
                baseUrl = baseUrl.substring(0, index);
                baseUrl += '/';
            }
            $window.location.href = baseUrl;
        };
}]);
