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
flowableModule.service('UserService', ['$http', '$q',
    function ($http, $q) {

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
        
        var userInfoHttpAsPromise = function(options, index) {
            var deferred = $q.defer();
            $http(options).
                success(function (response, status, headers, config) {
                    var userInfoFormObject = {
                        userData: response,
                        index: index
                    };
                    deferred.resolve(userInfoFormObject);
                })
                .error(function (response, status, headers, config) {
                    deferred.reject(response);
                });
                
            return deferred.promise;
        };
        
        /*
         * Get user info by id
         */
        this.getUserInfo = function (userId) {
            
            return httpAsPromise({
                method: 'GET',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/users/' + userId
            });
        };
        
        /*
         * Get user info by id
         */
        this.getUserInfoForForm = function (userId, index) {
        
            return userInfoHttpAsPromise({
                method: 'GET',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/users/' + userId
            }, index);
        };

        /*
         * Filter users based on a filter text, in the context of workflow: for tasks, processes, etc.
         */
        this.getFilteredUsers = function (filterText, taskId, processInstanceId, tenantId, group) {
            var params = {};
            if (typeof filterText === 'string') {
               params.filter = filterText;
            }
            else {
               // Could be i.e. { email: 'user@domain.com' } or { externalId: 'externalUserId' }
               params = filterText;
            }
            if(taskId) {
                params.excludeTaskId = taskId;
            }
            if (processInstanceId) {
                params.excludeProcessId = processInstanceId;
            }

            if (group && group.id) {
                params.groupId = group.id;
            }

            return httpAsPromise({
                method: 'GET',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/workflow-users',
                params: params
            });
        };

        /*
         * Filter users based on a filter text, in the context of IDM: use no context (contrary to the getFilteredUsers method).
         */
        this.getFilteredUsersStrict = function(filterText, tenantId, group) {

            var params = {};

            params.status = 'active';

            if (filterText !== null && filterText !== undefined) {
                params.filter = filterText;
            }

            if (group && group.id) {
                params.groupId = group.id;
            }

            return httpAsPromise(
                {
                    method: 'GET',
                    url: FLOWABLE.CONFIG.contextRoot + '/app/rest/admin/users',
                    params: params
                }
            )
        };

    }]);
