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

activitiAdminApp.controller('ContentItemsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {main: 'content-engine', sub: 'content-items'};

        $scope.filter = {};
        $scope.contentItemsData = {};

        // Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedItem = [];

        var filterConfig = {
            url: '/app/rest/activiti/content-items',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.contentItemsData = data;
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
                {name: 'CONTENT-ITEMS.SORT.SUBMITTED-DATE', id: 'submittedDate'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'CONTENT-ITEMS.FILTER.NAME', showByDefault: true},
                {id: 'tenantId', name: 'CONTENT-ITEMS.FILTER.TENANTID', showByDefault: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.formInstanceFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.formInstanceFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new ActivitiAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.formInstanceFilter = $scope.filter;
        }

        $scope.itemSelected = function (contentItem) {
            if (contentItem && contentItem.getProperty('id')) {
                $location.path('/content-item/' + contentItem.getProperty('id'));
            }
        };

        $q.all([$translate('CONTENT-ITEMS.HEADER.ID'),
                $translate('CONTENT-ITEMS.HEADER.TASK-ID'),
                $translate('CONTENT-ITEMS.HEADER.PROCESS-INSTANCE-ID'),
                $translate('CONTENT-ITEMS.HEADER.SUBMITTED-ON'),
                $translate('CONTENT-ITEMS.HEADER.SUBMITTED-BY'),
                $translate('CONTENT-ITEMS.HEADER.TENANT-ID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridFormInstances = {
                    data: 'contentItemsData.data',
                    enableRowReordering: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.itemSelected,
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
