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

// Milestone service
angular.module('flowableApp').service('UserEventListenerService', ['$http', '$q', '$rootScope',
    function ($http, $q, $rootScope) {
        var httpAsPromise = function (options) {
            var deferred = $q.defer();
            $http(options).success(function (response, status, headers, config) {
                deferred.resolve(response);
            })
                .error(function (response, status, headers, config) {
                    deferred.reject(response);
                });
            return deferred.promise;
        };

        this.getCaseInstanceAvailableUserEventListeners = function (caseInstanceId) {

            var data = {
                caseInstanceId: caseInstanceId
            };

            return httpAsPromise(
                {
                    method: 'GET',
                    url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/available-user-event-listeners',
                    data: data
                }
            );
        };

        this.getCaseInstanceCompletedUserEventListeners = function (caseInstanceId) {

            var data = {
                caseInstanceId: caseInstanceId
            };

            return httpAsPromise(
                {
                    method: 'GET',
                    url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/completed-user-event-listeners',
                    data: data
                }
            );
        };

        this.triggerCaseInstanceUserEventListener = function (caseInstanceId, userEventListenerId) {

            var deferred = $q.defer();
            var data = {
                caseInstanceId: caseInstanceId,
                userEventListener: userEventListenerId
            };

            $http({
                method: 'POST',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/trigger-user-event-listener/' + userEventListenerId,
                data: data
            }).success(function (response, status, headers, config) {
                $rootScope.$broadcast('user-event-listener-triggered', response);
                deferred.resolve(response);
            }).error(function (response, status, headers, config) {
                $rootScope.addAlert(response.message, 'error');
                deferred.reject(response);
            });

            var promise = deferred.promise;
            return promise;
        };
    }]);

