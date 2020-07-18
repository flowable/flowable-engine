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

// Plan item instance service
angular.module('flowableApp').service('PlanItemInstanceService', ['$http', '$q', '$rootScope',
    function ($http, $q, $rootScope) {
        var httpAsPromise = function (options) {
            var deferred = $q.defer();
            $http(options)
                .success(function (response, status, headers, config) {
                    deferred.resolve(response);
                })
                .error(function (response, status, headers, config) {
                    deferred.reject(response);
                });
            return deferred.promise;
        };

        this.getCaseInstanceEnabledPlanItemInstances = function (caseInstanceId) {
            return httpAsPromise(
                {
                    method: 'GET',
                    url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/enabled-planitem-instances'
                }
            );
        };

        this.startPlanItemInstance = function (caseInstanceId, planItemInstanceId) {
            var deferred = $q.defer();
            $http({
                method: 'POST',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/enabled-planitem-instances/' + planItemInstanceId
            }).success(function (response, status, headers, config) {
                $rootScope.$broadcast('user-event-listener-triggered', response);
                deferred.resolve(response);
            }).error(function (response, status, headers, config) {
                $rootScope.addAlert(response.message, 'error');
                deferred.reject(response);
            });

            return deferred.promise;
        };
    }]);

