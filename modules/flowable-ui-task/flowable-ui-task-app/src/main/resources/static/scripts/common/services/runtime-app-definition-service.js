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

// User service
flowableApp.service('RuntimeAppDefinitionService', ['$http', '$q', '$location', 'appName',
    function ($http, $q, $location, appName) {

        var httpAsPromise = function(options) {
            var deferred = $q.defer();
            $http(options).
                success(function (response, status, headers, config) {
                    deferred.resolve(response);
                })
                .error(function (response, status, headers, config) {
                    deferred.reject(response);
                });
            return deferred.promise;
        };

        this.getApplications = function () {

            var defaultApps = [];

            // Determine the full url with a context root (if any)
            var baseUrl = $location.absUrl();
            var index = baseUrl.indexOf('/#');
            if (index >= 0) {
                baseUrl = baseUrl.substring(0, index);
            }
            index = baseUrl.indexOf('?');
            if (index >= 0) {
                baseUrl = baseUrl.substring(0, index);
            }
            if (baseUrl[baseUrl.length - 1] == '/') {
                baseUrl = baseUrl.substring(0, baseUrl.length - 1);
            }
            if (appName.length > 0 && baseUrl.substring(baseUrl.length - appName.length) == appName) {
                baseUrl = baseUrl.substring(0, baseUrl.length - appName.length - 1);
            }

            var transformAppsResponse = function(value, headersGetter, status) {
                if (status !== 200) {
                    return;
                }
                
                var response = JSON.parse(value);
                var customApps = [];
                for (var i = 0; i < response.data.length; i++) {

                    var app = response.data[i];
                    if (app.defaultAppId !== undefined && app.defaultAppId !== null) {

                        if (app.defaultAppId === 'tasks') {

                            defaultApps.push(
                                {
                                    id: 'tasks',
                                    titleKey: 'APP.TASKS.TITLE',
                                    descriptionKey: 'APP.TASKS.DESCRIPTION',
                                    defaultAppId : app.defaultAppId,
                                    theme: 'theme-2',
                                    icon: 'icon icon-clock',
                                    fixedBaseUrl: baseUrl + '/workflow/' + '/#/',
                                    fixedUrl: baseUrl + '/workflow/',
                                    pages: ['tasks', 'processes']
                                });
                        }

                    } else {

                        // Custom app
                        app.icon = 'glyphicon ' + app.icon;
                        app.fixedBaseUrl = baseUrl + '/workflow/#/apps/' + app.appDefinitionKey + '/';
                        app.fixedUrl = app.fixedBaseUrl + 'tasks';
                        app.pages = [ 'tasks', 'processes' ];
                        app.deletable = true;
                        customApps.push(app);
                    }

                }

                return {
                    defaultApps: defaultApps,
                    customApps: customApps
                };
            };

            return httpAsPromise({
                method: 'GET',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/runtime/app-definitions',
                transformResponse: transformAppsResponse
            });
        };

        this.deleteAppDefinition = function (appDefinitionKey) {
            var promise = httpAsPromise({
                method: 'DELETE',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/runtime/app-definitions/' + appDefinitionKey
            });

            return promise;
        };
    }]);