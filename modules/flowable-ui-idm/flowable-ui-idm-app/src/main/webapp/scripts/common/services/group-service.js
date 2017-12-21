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
flowableModule.service('GroupService', ['$http', '$q',
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

        this.getFilteredGroups = function(filterText, group, tenantId) {

            var params = {};

            if (filterText !== null && filterText !== undefined) {
                params.filter = filterText;
            }
            
            if (group && group.id) {
                params.groupId = group.id;
            }

            return httpAsPromise(
                {
                    method: 'GET',
                    url: FLOWABLE.CONFIG.contextRoot + '/app/rest/admin/groups',
                    params: params
                }
            )
        };
    }]);
