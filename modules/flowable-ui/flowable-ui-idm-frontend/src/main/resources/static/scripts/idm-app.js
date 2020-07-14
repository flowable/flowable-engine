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

var flowableApp = angular.module('flowableApp', [
  	'http-auth-interceptor',
  	'ngCookies',
  	'ngResource',
  	'ngSanitize',
  	'ngRoute',
  	'mgcrea.ngStrap',
  	'ngAnimate',
  	'ngFileUpload',
  	'pascalprecht.translate',
  	'ui.grid',
    'ui.grid.edit',
    'ui.grid.selection',
    'ui.grid.autoResize',
    'ui.grid.cellNav'
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

    /*
     * Route resolver for all authenticated routes
     */
    var authRouteResolver = ['$rootScope', 'AuthenticationSharedService', function($rootScope, AuthenticationSharedService) {

        if(!$rootScope.authenticated) {
          // Return auth-promise. On success, the promise resolves and user is assumed authenticated from now on. If
          // promise is rejected, route will not be followed (no unneeded HTTP-calls will be done, which case a 401 in the end, anyway)
          return AuthenticationSharedService.authenticate();

        } else {
          // Authentication done on rootscope, no need to call service again. Any unauthenticated access to REST will result in
          // a 401 and will redirect to login anyway. Done to prevent additional call to authenticate every route-change
          $rootScope.authenticated = true;
          return true;
        }
    }];

    /*
     * Route resolver for all unauthenticated routes
     */
    var unauthRouteResolver = ['$rootScope', function($rootScope) {
      $rootScope.authenticationChecked = true;
    }];

    $routeProvider
        .when('/login', {
            templateUrl: 'views/login.html',
            controller: 'LoginController',
            resolve: {
                verify: unauthRouteResolver
            }
        })
        .when('/user-mgmt', {
            controller: 'IdmUserMgmtController',
            templateUrl: appResourceRoot + 'views/idm-user-mgmt.html',
            resolve: {
                verify: authRouteResolver
            }
        })
        .when('/group-mgmt', {
            controller: 'GroupMgmtController',
            templateUrl: appResourceRoot + 'views/idm-group-mgmt.html',
            resolve: {
                verify: authRouteResolver
            }
        })
        .when('/privilege-mgmt', {
            controller: 'PrivilegeMgmtController',
            templateUrl: appResourceRoot + 'views/idm-privilege-mgmt.html',
            resolve: {
                verify: authRouteResolver
            }
        })
        .when('/logout', {
            templateUrl: appResourceRoot + 'views/empty.html',
            controller: 'LogoutController'
        })
        .otherwise({
            redirectTo: FLOWABLE.CONFIG.appDefaultRoute || '/user-mgmt'
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
    .run(['$rootScope', function($rootScope) {
        $rootScope.$on( "$routeChangeStart", function(event, next, current) {
            if (next !== null && next !== undefined) {
                $rootScope.onLogin = next.templateUrl === 'views/login.html';
            }
        });
    }])
    .run(['$rootScope', '$timeout', '$translate', '$location', '$window', 'AuthenticationSharedService',
        function($rootScope, $timeout, $translate, $location, $window, AuthenticationSharedService) {

            // set angular translate fallback language
            $translate.fallbackLanguage(['en']);

            // setting Moment-JS (global) locale
            if (FLOWABLE.CONFIG.datesLocalization) {
                moment.locale($translate.proposedLanguage());
            }

            // Common model (eg selected tenant id)
            $rootScope.common = {};

            $rootScope.webRootUrl = function() {
                return FLOWABLE.CONFIG.webContextRoot;
            };

            $rootScope.restRootUrl = function() {
                return FLOWABLE.CONFIG.contextRoot;
            };

            // Needed for auto-height
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

            // Main navigation depends on the account being fetched
            $rootScope.$watch('account', function() {
                $rootScope.mainNavigation = [
                    {
                        id: 'userMgmt',
                        title: 'IDM.GENERAL.NAVIGATION.USER-MGMT',
                        path: '/user-mgmt'
                    },
                    {
                        id: 'groupMgmt',
                        title: 'IDM.GENERAL.NAVIGATION.GROUP-MGMT',
                        path: '/group-mgmt'
                    },
                    {
                        id: 'privilegeMgmt',
                        title: 'IDM.GENERAL.NAVIGATION.PRIVILEGE-MGMT',
                        path: '/privilege-mgmt'
                    }
                ];


                /*
                 * Set the current main page, using the page object. If the page is already active,
                 * this is a no-op.
                 */
                $rootScope.setMainPage = function(mainPage) {
                    $rootScope.mainPage = mainPage;
                    $location.path($rootScope.mainPage.path);
                };

                /*
                 * Set the current main page, using the page ID. If the page is already active,
                 * this is a no-op.
                 */
                $rootScope.setMainPageById = function(mainPageId) {
                    for (var i=0; i<$rootScope.mainNavigation.length; i++) {
                        if (mainPageId == $rootScope.mainNavigation[i].id) {
                            $rootScope.mainPage = $rootScope.mainNavigation[i];
                            break;
                        }
                    }
                };
            });
        }
    ])
    .run(['$rootScope', '$timeout', '$translate', '$location', '$http', '$window', '$popover', 'appResourceRoot',
        function($rootScope, $timeout, $translate, $location, $http, $window, $popover, appResourceRoot) {

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
     }])
     .run(['$rootScope', '$location', '$window', 'AuthenticationSharedService', '$translate', '$modal',
        function($rootScope, $location, $window, AuthenticationSharedService, $translate, $modal) {

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

        $rootScope.logout = function() {
            AuthenticationSharedService.logout();
        };

        // Call when the 401 response is returned by the client
        $rootScope.$on('event:auth-loginRequired', function(rejection) {
            $rootScope.authenticated = false;
            $rootScope.authenticationChecked = true;
            if (FLOWABLE.CONFIG.loginUrl) {
                $window.location.href = FLOWABLE.CONFIG.loginUrl.replace("{url}", $location.absUrl());
                $window.reload();
            }
            else {
                $location.path('/login').replace();
            }
        });

        // Call when the user is authenticated
        $rootScope.$on('event:auth-authConfirmed', function(event, data) {
        
            $rootScope.authenticated = true;
            $rootScope.authenticationChecked = true;

            var redirectUrl = $location.search().redirectUrl;
            if (redirectUrl !== null && redirectUrl !== undefined && redirectUrl.length > 0) {
                $window.location.href = redirectUrl;
            } else {
                var locationPath = $location.path();
                if (locationPath == '' || locationPath == '#' || locationPath == '/login'
                    || locationPath.indexOf('/account/activate/') >= 0 || locationPath.indexOf('/account/reset-password/') >= 0) {
                      
                    $location.path('/');
                }
            }
        });

        // Call when the user logs in
        $rootScope.$on('event:auth-loginConfirmed', function() {
            AuthenticationSharedService.authenticate();
        });

        // Call when the user logs out
        $rootScope.$on('event:auth-loginCancelled', function() {
            $rootScope.authenticated = false;
            $location.path('/login');
        });

        // Call when login fails
        $rootScope.$on('event:auth-loginFailed', function() {
            $rootScope.addAlertPromise($translate('LOGIN.MESSAGES.ERROR.AUTHENTICATION'), 'error'); 
        });

        $rootScope.backToLanding = function() {
            var baseUrl = $location.absUrl();
            var index = baseUrl.indexOf('/#');
            if (index >= 0) {
                baseUrl = baseUrl.substring(0, index);
                baseUrl += '/';
            }
            $window.location.href = baseUrl;
        };
}])
	
	// Moment-JS date-formatting filter
    .filter('dateformat', function() {
        return function(date, format) {
            if (date) {
                if (format) {
                    return moment(date).format(format);
                } else {
                    return moment(date).calendar();
                }
            }
            return '';
        };
    });
