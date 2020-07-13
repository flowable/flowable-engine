/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
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

flowableAdminApp.controller('BatchPartController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'process-engine', sub: 'batches'};

        $scope.tabData = {
            tabs: [
                {id: 'batchPartDocument', name: 'BATCH-PART.TITLE.BATCH-PART-DOCUMENT'}
            ]
        };

        $scope.tabData.activeTab = $scope.tabData.tabs[0].id;

        $scope.returnToList = function () {
            $location.path("/batch/" + $scope.batchPart.batchId);
        };

        $scope.openProcessDefinition = function (processDefinitionId) {
            if (processDefinitionId) {
                $location.path("/process-definition/" + processDefinitionId);
            }
        };
        
        $scope.openProcessInstance = function (processInstanceId) {
            if (processInstanceId) {
                $location.path("/process-instance/" + processInstanceId);
            }
        };

        $scope.loadBatchPart = function () {
            $scope.batchPart = undefined;
            
            // Load batch part
            $http({
                method: 'GET',
                url: '/app/rest/admin/batch-parts/' + $routeParams.batchPartId
            }).success(function (data, status, headers, config) {
                $scope.batchPart = data;

                // Start loading children
                $scope.loadProcessDefinitions();
                $scope.loadBatchPartDocument();

            }).error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
        };
            
        $scope.loadProcessDefinitions = function () {
            if ($scope.batchPart.batchType == 'processMigration') {
                // Load definitions
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/process-definitions/' + $scope.batchPart.searchKey
                }).success(function (data, status, headers, config) {
                    $scope.sourceDefinition = data;
                }).error(function (data, status, headers, config) {
                });
                
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/process-definitions/' + $scope.batchPart.searchKey2
                }).success(function (data, status, headers, config) {
                    $scope.targetDefinition = data;
                }).error(function (data, status, headers, config) {
                });
            }
        };
            
        $scope.loadBatchPartDocument = function () {
            $scope.batchPartDocument = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/batch-parts/' + $scope.batchPart.id + '/batch-part-document'
            }).success(function (data, status, headers, config) {
                $scope.batchPartDocument = data;
            });
        };

        $scope.executeWhenReady(function () {
            $scope.loadBatchPart();
        });
        
    }]);
