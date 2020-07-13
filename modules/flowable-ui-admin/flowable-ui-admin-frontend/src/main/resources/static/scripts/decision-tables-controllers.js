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

flowableAdminApp.controller('DecisionTablesController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {main: 'dmn-engine', sub: 'decision-tables'};
        
        $scope.filter = {};
        $scope.decisionTablesData = {};

        // Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedDecisionTables = [];

        var filterConfig = {
            url: '/app/rest/admin/decision-tables',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.decisionTablesData = data;
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
                {name: 'DECISION-TABLES.SORT.ID', id: 'id'},
                {name: 'DECISION-TABLES.SORT.NAME', id: 'name'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'DECISION-TABLES.FILTER.NAME', showByDefault: true},
                {id: 'keyLike', name: 'DECISION-TABLES.FILTER.KEY', showByDefault: true},
                {id: 'tenantIdLike', name: 'DECISION-TABLES.FILTER.TENANTID', showByDefault: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.decisionTableFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.decisionTableFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.decisionTableFilter = $scope.filter;
        }

        $scope.decisionTableSelected = function (decisionTable) {
            if (decisionTable && decisionTable.getProperty('id')) {
                $location.path('/decision-table/' + decisionTable.getProperty('id'));
            }
        };

        $q.all([$translate('DECISION-TABLES.HEADER.ID'),
                $translate('DECISION-TABLES.HEADER.NAME'),
                $translate('DECISION-TABLES.HEADER.KEY'),
                $translate('DECISION-TABLES.HEADER.VERSION'),
                $translate('DECISION-TABLES.HEADER.TENANT-ID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridDecisionTables = {
                    data: 'decisionTablesData.data',
                    enableRowReordering: true,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.decisionTableSelected,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'key', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'version', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate}]
                };
            });

        $scope.executeWhenReady(function () {
            $scope.filter.refresh();
        });

    }]);
