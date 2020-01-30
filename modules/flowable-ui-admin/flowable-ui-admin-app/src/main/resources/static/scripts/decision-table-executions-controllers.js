/* Copyright 2005-2015 Alfresco Software, Ltd.
 *
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

/* Controllers */

flowableAdminApp.controller('DecisionTableExecutionsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {main: 'dmn-engine', sub: 'executions'};
        
        $scope.filter = {};
        $scope.decisionTableExecutionsData = {};

        // Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedDecisionTables = [];

        var filterConfig = {
            url: '/app/rest/admin/decision-tables/history',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.decisionTableExecutionsData = data;
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
                {name: 'DECISION-TABLE-EXECUTIONS.SORT.START-TIME', id: 'startTime'},
                {name: 'DECISION-TABLE-EXECUTIONS.SORT.END-TIME', id: 'endTime'},
                {name: 'DECISION-TABLE-EXECUTIONS.SORT.TENANT-ID', id: 'tenantId'}
            ],

            supportedProperties: [
                {id: 'tenantIdLike', name: 'DECISION-TABLE-EXECUTIONS.FILTER.TENANTID', showByDefault: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.decisionTableExecutionsFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.decisionTableExecutionsFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.decisionTableExecutionsFilter = $scope.filter;
        }

        $scope.decisionTableExecutionSelected = function (decisionTableExecution) {
            if (decisionTableExecution && decisionTableExecution.getProperty('id')) {
                $location.path('/decision-table-execution/' + decisionTableExecution.getProperty('id'));
            }
        };

        $q.all([$translate('DECISION-TABLE-EXECUTIONS.HEADER.ID'),
                $translate('DECISION-TABLE-EXECUTIONS.HEADER.DECISION-DEFINITION-KEY'),
                $translate('DECISION-TABLE-EXECUTIONS.HEADER.DECISION-DEFINITION-NAME'),
                $translate('DECISION-TABLE-EXECUTIONS.HEADER.END-TIME'),
                $translate('DECISION-TABLE-EXECUTIONS.HEADER.FAILED'),
                $translate('DECISION-TABLE-EXECUTIONS.HEADER.TENANT-ID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridDecisionTableExecutions = {
                    data: 'decisionTableExecutionsData.data',
                    enableRowReordering: true,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.decisionTableExecutionSelected,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'decisionKey', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'decisionName', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'endTime', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'failed', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[5], cellTemplate: gridConstants.defaultTemplate}]
                };
            });

        $scope.executeWhenReady(function () {
            $scope.filter.refresh();
        });

    }]);
