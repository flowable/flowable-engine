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

flowableAdminApp.controller('BatchController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'process-engine', sub: 'batches'};

        $scope.tabData = {
            tabs: [
                {id: 'batchDocument', name: 'BATCH.TITLE.BATCH-DOCUMENT'},
                {id: 'successParts', name: 'BATCH.TITLE.SUCCESS-PARTS'},
                {id: 'failParts', name: 'BATCH.TITLE.FAIL-PARTS'}
            ]
        };

        $scope.tabData.activeTab = $scope.tabData.tabs[0].id;

        $scope.returnToList = function () {
            $location.path("/batches");
        };

        $scope.openBatchPart = function (part) {
            if (part && part.getProperty('id')) {
                $location.path("/batch-part/" + part.getProperty('id'));
            }
        };

        $scope.openProcessDefinition = function (processDefinitionId) {
            if (processDefinitionId) {
                $location.path("/process-definition/" + processDefinitionId);
            }
        };

        $scope.loadBatch = function () {
            $scope.batch = undefined;
            
            // Load batch
            $http({
                method: 'GET',
                url: '/app/rest/admin/batches/' + $routeParams.batchId
            }).success(function (data, status, headers, config) {
                $scope.batch = data;

                // Start loading children
                $scope.loadProcessDefinitions();
                $scope.loadBatchDocument();
                $scope.loadSuccessParts();
                $scope.loadFailParts();

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

        var dateTemplate = '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>';

        // Config for success parts grid
        $q.all([$translate('BATCH.HEADER.ID'),
            $translate('BATCH.HEADER.CREATE-TIME'),
            $translate('BATCH.HEADER.SCOPE-ID'),
            $translate('BATCH.HEADER.SCOPE-TYPE'),])
            .then(function (headers) {
                $scope.gridSuccessParts = {
                    data: 'successParts',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openBatchPart,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], width: 50},
                        {field: 'createTime', displayName: headers[1], cellTemplate: dateTemplate},
                        {field: 'scopeId', displayName: headers[2]},
                        {field: 'scopeType', displayName: headers[3]}
                    ]
                };
            });

        $q.all([$translate('BATCH.HEADER.ID'),
            $translate('BATCH.HEADER.CREATE-TIME'),
            $translate('BATCH.HEADER.SCOPE-ID'),
            $translate('BATCH.HEADER.SCOPE-TYPE'),])
            .then(function (headers) {
                $scope.gridFailParts = {
                    data: 'failParts',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openBatchPart,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], width: 50},
                        {field: 'createTime', displayName: headers[1], cellTemplate: dateTemplate},
                        {field: 'scopeId', displayName: headers[2]},
                        {field: 'scopeType', displayName: headers[3]}
                    ]
                };
            });
            
        $scope.loadProcessDefinitions = function () {
            if ($scope.batch.batchType == 'processMigration') {
                // Load definitions
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/process-definitions/' + $scope.batch.searchKey
                }).success(function (data, status, headers, config) {
                    $scope.sourceDefinition = data;
                }).error(function (data, status, headers, config) {
                });
                
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/process-definitions/' + $scope.batch.searchKey2
                }).success(function (data, status, headers, config) {
                    $scope.targetDefinition = data;
                }).error(function (data, status, headers, config) {
                });
            }
        };
            
        $scope.loadBatchDocument = function () {
            $scope.batchDocument = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/batches/' + $scope.batch.id + '/batch-document'
            }).success(function (data, status, headers, config) {
                $scope.batchDocument = data;
            });
        };

        $scope.loadSuccessParts = function () {
            $scope.successParts = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/batches/' + $scope.batch.id + '/batch-parts?status=success'
            }).success(function (data, status, headers, config) {
                $scope.successParts = data;
                $scope.tabData.tabs[1].info = data.length;
            });
        };

        $scope.loadFailParts = function () {
            $scope.failParts = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/batches/' + $scope.batch.id + '/batch-parts?status=fail'
            }).success(function (data, status, headers, config) {
                $scope.failParts = data;
                $scope.tabData.tabs[2].info = data.length;
            });
        };

        $scope.executeWhenReady(function () {
            $scope.loadBatch();
        });


        // Dialogs
        $scope.deleteBatch = function (action) {
            if (!action) {
                action = "delete";
            }
            var modalInstance = $modal.open({
                templateUrl: 'views/batch-delete-popup.html',
                controller: 'DeleteBatchModalInstanceCtrl',
                resolve: {
                    batch: function () {
                        return $scope.batch;
                    }
                }
            });

            modalInstance.result.then(function (deleteBatchInstance) {
                if (deleteBatchInstance) {
                    $scope.addAlert($translate.instant('ALERT.BATCH.DELETED', $scope.batch), 'info');
                    $scope.returnToList();
                }
            });
        };
    }]);

flowableAdminApp.controller('DeleteBatchModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'batch', function ($rootScope, $scope, $modalInstance, $http, batch) {

        $scope.batch = batch;
        $scope.status = {loading: false};
        $scope.model = {};
        $scope.ok = function () {
            $scope.status.loading = true;

            $http({
                method: 'POST', url: '/app/rest/admin/batches/' + $scope.batch.id
                
            }).success(function (data, status, headers, config) {
                $modalInstance.close(true);
                $scope.status.loading = false;
            }).error(function (data, status, headers, config) {
                $modalInstance.close(false);
                $scope.status.loading = false;
            });
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };
    }]);
