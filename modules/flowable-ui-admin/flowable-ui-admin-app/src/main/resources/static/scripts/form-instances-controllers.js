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

flowableAdminApp.controller('FormInstancesController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {main: 'form-engine', sub: 'instances'};
        
        $scope.filter = {};
        $scope.formInstancesData = {};

        // Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedForms = [];

        var filterConfig = {
            url: '/app/rest/admin/form-instances',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.formInstancesData = data;
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
                {name: 'FORM-INSTANCES.SORT.SUBMITTED-DATE', id: 'submittedDate'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'FORM-INSTANCES.FILTER.NAME', showByDefault: true},
                {id: 'tenantId', name: 'FORM-INSTANCES.FILTER.TENANTID', showByDefault: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.formInstanceFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.formInstanceFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.formInstanceFilter = $scope.filter;
        }

        $scope.formSelected = function (form) {
            if (form && form.getProperty('id')) {
                $location.path('/form-instance/' + form.getProperty('id'));
            }
        };

        $q.all([$translate('FORM-INSTANCES.HEADER.ID'),
                $translate('FORM-INSTANCES.HEADER.TASK-ID'),
                $translate('FORM-INSTANCES.HEADER.PROCESS-INSTANCE-ID'),
                $translate('FORM-INSTANCES.HEADER.SUBMITTED-ON'),
                $translate('FORM-INSTANCES.HEADER.SUBMITTED-BY'),
                $translate('FORM-INSTANCES.HEADER.TENANT-ID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridFormInstances = {
                    data: 'formInstancesData.data',
                    enableRowReordering: true,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.formSelected,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'taskId', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'processInstanceId', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'submittedDate', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'submittedBy', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[5], cellTemplate: gridConstants.defaultTemplate}]
                };
            });

        $scope.executeWhenReady(function () {
            $scope.filter.refresh();
        });

    }]);
