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

/* Controllers */

activitiAdminApp.controller('AppDeploymentsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {selection: 'apps'};
        
        $scope.filter = {};
        $scope.appsData = {};

        var filterConfig = {
            url: '/app/rest/activiti/apps',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.appsData = data;
            },
            error: function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            },

            sortObjects: [
                {name: 'APP-DEPLOYMENTS.SORT.ID', id: 'id'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'APP-DEPLOYMENTS.FILTER.NAME', showByDefault: true},
                {id: 'tenantId', name: 'APP-DEPLOYMENTS.FILTER.TENANTID', showByDefault: true},
                {id: 'latest', name: 'APP-DEPLOYMENTS.FILTER.TENANTID', showByDefault: true, defaultValue: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.appsFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.appsFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new ActivitiAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.appsFilter = $scope.filter;
        }

        $scope.appSelected = function (app) {
            if (app && app.getProperty('id')) {
                $location.path('/app/' + app.getProperty('id'));
            }
        };

        $q.all([$translate('APP-DEPLOYMENTS.HEADER.ID'),
                $translate('APP-DEPLOYMENTS.HEADER.APP-DEFINITION-ID'),
                $translate('APP-DEPLOYMENTS.HEADER.NAME'),
                $translate('APP-DEPLOYMENTS.HEADER.DEPLOY-TIME'),
                $translate('APP-DEPLOYMENTS.HEADER.DEPLOYED-BY'),
                $translate('APP-DEPLOYMENTS.HEADER.TENANT')])
            .then(function (headers) {

                // Config for grid
                $scope.gridApps = {
                    data: 'appsData.data',
                    enableRowReordering: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.appSelected,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'appDefinition.id', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'appDefinition.name', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'created', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'createdBy', displayName: headers[4], cellTemplate: gridConstants.userObjectTemplate},
                        {field: 'appDefinition.tenantId', displayName: headers[5], cellTemplate: gridConstants.defaultTemplate}]
                };
            });

        $scope.executeWhenReady(function () {
            $scope.filter.refresh();
        });

        /*
         * ACTIONS
         */
        $scope.uploadApp = function () {
            var modalInstance = $modal.open({
                templateUrl: 'views/upload-app.html',
                controller: 'UploadAppCrtl'
            });
            modalInstance.result.then(function (result) {
                // Refresh page if closed successfully
                if (result) {
                    $scope.appsData = {};
                    $scope.filter.refresh();
                }
            });
        };
    }]);


/**\
 * Controller for the upload a model from the process Modeler.
 */
activitiAdminApp.controller('UploadAppCrtl',
    ['$scope', '$modalInstance', '$http', '$upload', function ($scope, $modalInstance, $http, $upload) {

        $scope.status = {loading: false};

        $scope.model = {};

        $scope.onFileSelect = function ($files) {

            for (var i = 0; i < $files.length; i++) {
                var file = $files[i];
                $upload.upload({
                    url: '/app/rest/activiti/apps',
                    method: 'POST',
                    file: file
                }).progress(function (evt) {
                    $scope.status.loading = true;
                    $scope.model.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
                }).success(function (data, status, headers, config) {
                        $scope.status.loading = false;
                        if (data.error) {
                            $scope.model.errorMessage = data.errorDescription;
                            $scope.model.error = true;
                        } else {
                            $modalInstance.close(true);
                        }
                }).error(function (data, status, headers, config) {

                    if (data && data.message) {
                        $scope.model.errorMessage = data.message;
                    }

                    $scope.model.error = true;
                    $scope.status.loading = false;
                });
            }
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };

    }]);
