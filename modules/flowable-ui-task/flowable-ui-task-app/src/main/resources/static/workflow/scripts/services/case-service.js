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

// Task service
angular.module('flowableApp').service('CaseService', ['$http', '$q', '$rootScope', 'RelatedContentService',
    function ($http, $q, $rootScope, RelatedContentService) {

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

        this.getCaseDefinitions = function(appDefinitionKey) {
            var url = FLOWABLE.CONFIG.contextRoot + '/app/rest/case-definitions?latest=true';
            if (appDefinitionKey) {
                url += '&appDefinitionKey=' + appDefinitionKey;
            }
            return httpAsPromise(
                {
                    method: 'GET',
                    url: url
                }
            );
        };

    this.getRelatedContent = function (caseId) {
        var deferred = $q.defer();
        $http({
            method: 'GET',
            url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseId + '/content'
        }).success(function (response, status, headers, config) {
            // Add raw URL property to all content
            if (response && response.data) {
                for (var i = 0; i < response.data.length; i++) {
                    RelatedContentService.addUrlToContent(response.data[i]);
                }
            }
            deferred.resolve(response);
        })
            .error(function (response, status, headers, config) {
                deferred.reject(response);
            });
        return deferred.promise;
    };


    this.createCase = function(caseData) {
            var deferred = $q.defer();
            $http({
                method: 'POST',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances',
                data: caseData
            }).success(function (response, status, headers, config) {
                $rootScope.$broadcast('new-case-created', response);
                deferred.resolve(response);
            }).error(function (response, status, headers, config) {
                $rootScope.addAlert(response.message, 'error');
                deferred.reject(response);
            });

            var promise = deferred.promise;
            return promise;
        };

        this.deleteCase = function(caseInstanceId) {
            var deferred = $q.defer();
            $http({
                method: 'DELETE',
                url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId
            }).success(function (response, status, headers, config) {
                $rootScope.$broadcast('caseinstance-deleted', response);
                deferred.resolve(response);
            }).error(function (response, status, headers, config) {
                $rootScope.addAlert(response.message, 'error');
                deferred.reject(response);
            });

            var promise = deferred.promise;
            return promise;
        }
    }]
);
