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

activitiAdminApp.controller('FormsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

        $rootScope.navigation = {selection: 'forms'};
        
        $scope.filter = {};
        $scope.formsData = {};

        // Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedForms = [];

        var filterConfig = {
            url: '/app/rest/activiti/forms',
            method: 'GET',
            success: function (data, status, headers, config) {
                $scope.formsData = data;
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
                {name: 'FORMS.SORT.ID', id: 'id'},
                {name: 'FORMS.SORT.NAME', id: 'name'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'FORMS.FILTER.NAME', showByDefault: true},
                {id: 'appId', name: 'FORMS.FILTER.APPID', showByDefault: true},
                {id: 'tenantId', name: 'FORMS.FILTER.TENANTID', showByDefault: true}
            ]
        };

        if ($rootScope.filters && $rootScope.filters.formFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.formFilter;
            $scope.filter.config = filterConfig;
        } else {
            $scope.filter = new ActivitiAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.formFilter = $scope.filter;
        }

        $scope.formSelected = function (form) {
            if (form && form.getProperty('id')) {
                $location.path('/form/' + form.getProperty('id'));
            }
        };

        $q.all([$translate('FORMS.HEADER.ID'),
                $translate('FORMS.HEADER.NAME'),
                $translate('FORMS.HEADER.APPID'),
                $translate('FORMS.HEADER.TENANTID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridForms = {
                    data: 'formsData.data',
                    enableRowReordering: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.formSelected,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'appDeploymentId', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate}]
                };
            });

        $scope.executeWhenReady(function () {
            $scope.filter.refresh();
        });

    }]);
