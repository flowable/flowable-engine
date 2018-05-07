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
    'ngCookies',
    'ngResource',
    'ngSanitize',
    'ngRoute',
    'ngDragDrop',
    'mgcrea.ngStrap',
    'ngFileUpload',
    'ngAnimate',
    'pascalprecht.translate',
    'ui.grid',
    'ui.grid.edit',
    'ui.grid.selection',
    'ui.grid.autoResize',
    'angular-loading-bar',
    'cfp.hotkeys',
    'ui.mask'	 
]);

var flowableModule = flowableApp;

flowableApp

  // Initialize routes
   .config(['$provide', '$routeProvider', '$selectProvider', '$datepickerProvider', '$translateProvider', 'cfpLoadingBarProvider',
   function ($provide, $routeProvider, $selectProvider, $datepickerProvider, $translateProvider, cfpLoadingBarProvider) {

   var appName = 'workflow';
   $provide.value('appName', appName);
   var appResourceRoot = FLOWABLE.CONFIG.webContextRoot + (FLOWABLE.CONFIG.webContextRoot ? '/' + appName + '/' : '');
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
        .when('/start-process', {
            templateUrl: appResourceRoot + 'views/start-process.html',
            controller: 'StartProcessController'
        })
        .when('/apps/:appDefinitionKey/start-process', {
            templateUrl: appResourceRoot + 'views/start-process.html',
            controller: 'StartProcessController'
        })
        .when('/tasks', {
            templateUrl: appResourceRoot + 'views/tasks.html',
            controller: 'TasksController'
        })
        .when('/apps/:appDefinitionKey/tasks', {
            templateUrl: appResourceRoot + 'views/tasks.html',
            controller: 'TasksController'
        })
        .when('/task/:taskId', {
            templateUrl: appResourceRoot + 'views/task.html',
            controller: 'TaskController'
        })
        .when('/apps/:appDefinitionKey/task/:taskId', {
            templateUrl: appResourceRoot + 'views/task.html',
            controller: 'TaskController'
        })
        .when('/processes', {
            templateUrl: appResourceRoot + 'views/processes.html',
            controller: 'ProcessesController'
        })
        .when('/apps/:appDefinitionKey/processes', {
            templateUrl: appResourceRoot + 'views/processes.html',
            controller: 'ProcessesController'
        })
        .when('/process/:processId', {
            templateUrl: appResourceRoot + 'views/process.html',
            controller: 'ProcessController'
        })
        .when('/apps/:appDefinitionKey/process/:processId', {
            templateUrl: appResourceRoot + 'views/process.html',
            controller: 'ProcessController'
        })
        .when('/cases', {
            templateUrl: appResourceRoot + 'views/cases.html',
            controller: 'CasesController'
        })
        .when('/apps/:appDefinitionKey/cases', {
            templateUrl: appResourceRoot + 'views/cases.html',
            controller: 'CasesController'
        })
        .when('/apps/:appDefinitionKey/case/:caseId', {
            templateUrl: appResourceRoot + 'views/case.html',
            controller: 'CaseController'
        })
        .otherwise({
            redirectTo: FLOWABLE.CONFIG.appDefaultRoute || '/tasks'
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

       // turn loading bar spinner off (angular-loading-bar lib)
       cfpLoadingBarProvider.includeSpinner = false;
    }])
    .run(['$rootScope', '$routeParams', '$timeout', '$translate', '$location', '$http', '$window', 'appResourceRoot', 'AppDefinitionService', 'ProcessService', 'CaseService',
        function($rootScope, $routeParams, $timeout, $translate, $location, $http, $window, appResourceRoot, AppDefinitionService, ProcessService, CaseService) {

        // set angular translate fallback language
        $translate.fallbackLanguage(['en']);

        // setting Moment-JS (global) locale
        if (FLOWABLE.CONFIG.datesLocalization) {
            moment.locale($translate.proposedLanguage());
        }

        $rootScope.restRootUrl = function() {
            return FLOWABLE.CONFIG.contextRoot;
        };

        $rootScope.config = FLOWABLE.CONFIG;
        $rootScope.appResourceRoot = appResourceRoot;
        $rootScope.activitiFieldIdPrefix = 'activiti-';

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

        // Main navigation
        $rootScope.mainNavigation = [
            {
                'id': 'tasks',
                'title': 'GENERAL.NAVIGATION.TASKS',
                'path': '/tasks'
            },
            {
                'id': 'processes',
                'title': 'GENERAL.NAVIGATION.PROCESSES',
                'path': '/processes'
            },
            {
                'id': 'cases',
                'title': 'GENERAL.NAVIGATION.CASES',
                'path': '/cases'
            }
        ];

        $rootScope.mainPage = $rootScope.mainNavigation[0];

        // Empty object to hold cached app-definitions
        $rootScope.appDefinitions = {

        };

        /*
         * Set the current main page, using the page object. If the page is already active,
         * this is a no-op.
         */
        $rootScope.setMainPage = function(mainPage) {
            $rootScope.mainPage = mainPage;

            var path;
            if($rootScope.activeAppDefinition) {
                path = "/apps/" + $rootScope.activeAppDefinition.id + mainPage.path;
            } else {
                path = $rootScope.mainPage.path;
            }
            $location.path(path);
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

        // Alerts
        $rootScope.alerts = {
            queue: []
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
        
        $http.get(FLOWABLE.CONFIG.contextRoot + '/app/rest/account')
        	.success(function (data, status, headers, config) {
              	$rootScope.account = data;
               	$rootScope.invalidCredentials = false;
 				$rootScope.authenticated = true;
          	});

        $rootScope.model = {};
        // TODO: remove proc-def from rootscope or make smarter
        $rootScope.root = {};

        $rootScope.loadProcessDefinitions = function(appDefinitionKey) {
        	ProcessService.getProcessDefinitions(appDefinitionKey).then(function(response) {
        		$rootScope.root.processDefinitions = response.data;
        	});
        };
        
        $rootScope.loadCaseDefinitions = function(appDefinitionKey) {
            CaseService.getCaseDefinitions(appDefinitionKey).then(function(response) {
                $rootScope.root.caseDefinitions = response.data;
            });
        };

        $rootScope.$on("$locationChangeStart",
            function (event, newUrl, oldUrl) {
                if(newUrl.indexOf("headless") >= 0) {
                    $rootScope.root.headless = true;
                } else {
                    $rootScope.root.headless = false;
                }
            }
        );

        /**
         * A 'safer' apply that avoids concurrent updates (which $apply allows).
         */
        $rootScope.safeApply = function(fn) {
            var phase = this.$root.$$phase;
            if(phase == '$apply' || phase == '$digest') {
                if(fn && (typeof(fn) === 'function')) {
                    fn();
                }
            } else {
                this.$apply(fn);
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

    }
  ])
  .run(['$rootScope', '$location', '$window', '$translate', 'appName', '$modal',
        function($rootScope, $location, $window, $translate, appName , $modal) {

        var fixedUrlPart = '/' + appName + '/';

        $rootScope.backToLanding = function() {
            var baseUrl = $location.absUrl();
            var index = baseUrl.indexOf(fixedUrlPart);
            if (index >= 0) {
                baseUrl = baseUrl.substring(0, index) + '/';
            }
            $window.location.href = baseUrl;
        };
    }])

    // Moment-JS date-formatting filter
    .filter('dateformat', function() {
        return function(date, format) {
            if (date) {
                if(format == 'fromNow') {
                    return moment(date).fromNow();
                } else if(format == 'fromNowFull') {
                    return moment(date).fromNow() + ' (' + moment(date).format('MMMM Do YYYY') + ')';
                } else if (format) {
                    return moment(date).format(format);
                } else {
                    return moment(date).calendar();
                }
            }
            return '';
        };
    })
    .filter('duration', ['$translate', function($translate) {
        return function(millis) {
            if (millis) {
                var duration = moment.duration(millis);
                var result = '';
                var hours = duration.hours();
                if (hours > 0) {
                    result = result + hours + ' ' + $translate.instant('GENERAL.TIME.HOURS') + ' ';
                }
                var mins = duration.minutes();
                if (mins > 0) {
                    result = result + mins + ' ' + $translate.instant('GENERAL.TIME.MINUTES');
                }

                if (hours == 0 && mins == 0) {
                    result = duration.seconds() + ' ' + $translate.instant('GENERAL.TIME.SECONDS');

                }

                return result;
            }
            return millis;
        };
    }])
    .filter('username', function() {
        return function(user) {
            if (user) {
               if(user.firstName) {
                   return user.firstName + " " + user.lastName;
               } else if(user.lastName) {
                   return user.lastName;
               } else {
			       if (user != undefined && user != null){
				       var _user = user.split(".");
					   if (_user.length > 1){
					       user = _user[0].charAt(0).toUpperCase() + _user[0].slice(1) +" "+ _user[1].charAt(0).toUpperCase() + _user[1].slice(1);
					   } else {
						   user = _user[0].charAt(0).toUpperCase() + _user[0].slice(1);
					   }
					   return user;
					   
				   } else {
					   return "??";
			       }
			   }
            }
            return '';
        };
    });
