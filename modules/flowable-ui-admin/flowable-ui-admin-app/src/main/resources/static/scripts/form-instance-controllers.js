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

flowableAdminApp.controller('FormInstanceController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', '$routeParams',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, $routeParams) {

        $rootScope.navigation = {main: 'form-engine', sub: 'instances'};

        $scope.returnToList = function () {
            $location.path("/form-instances");
        };

        $q.all([$translate('FORM-INSTANCE.FORM-FIELD-VALUES.ID'),
            $translate('FORM-INSTANCE.FORM-FIELD-VALUES.NAME'),
            $translate('FORM-INSTANCE.FORM-FIELD-VALUES.TYPE'),
            $translate('FORM-INSTANCE.FORM-FIELD-VALUES.VALUE')])
            .then(function (headers) {

                $scope.gridFormFieldValues = {
                    data: 'formFieldValues.data',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.showFormInstance,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'name', displayName: headers[1]},
                        {field: 'type', displayName: headers[2]},
                        {field: 'value', displayName: headers[3], cellFilter: 'empty'}
                    ]
                };
            });
        
        $scope.executeWhenReady(function () {
            if ($rootScope.formInstance) {
                $scope.formInstance = $rootScope.formInstance;
                $rootScope.formInstance = undefined;
                return;
            }
            
            // Load submitted form
            $http({method: 'GET', url: '/app/rest/admin/form-instances/' + $routeParams.formInstanceId}).
            success(function (data, status, headers, config) {
                $scope.formInstance = data;

                // Load form submitted forms
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/form-instances/' + $routeParams.formInstanceId + '/form-field-values/'
                }).
                success(function (formFieldValuesData, status, headers, config) {
                    $scope.formFieldValues = formFieldValuesData;
                });
            }).
            error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
        });
    }]);

flowableAdminApp.controller('ShowFormRenderPopupCtrl',
        ['$rootScope', '$scope', '$modalInstance', '$http', 'form', '$timeout', '$translate', 'uiGridConstants',
            function ($rootScope, $scope, $modalInstance, $http, form, $timeout, $translate, uiGridConstants) {

                $scope.status = {loading: false};

                $scope.cancel = function () {
                    if (!$scope.status.loading) {
                        $modalInstance.dismiss('cancel');
                    }
                };

                $scope.popup = {};
                
                $scope.formTabClicked = function(tab) {
                    $scope.activeFormTab = tab;
                };

                $scope.executeWhenReady(function () {
                    if (!form.form) {
                        // Load form
                        $http({
                            method: 'GET',
                            url: '/app/rest/admin/form-instances/' + form.id
                        }).
                        success(function (data, status, headers, config) {
                            $scope.popup.currentForm = data.form;
                            $scope.popup.formName = form.name || '';
                            resetActiveFormTab(data.form);
                        }).
                        error(function (data, status, headers, config) {
                            if (data && data.message) {
                                // Extract error-message
                                $rootScope.addAlert(data.message, 'error');
                            } else {
                                // Use default error-message
                                $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                            }
                        });
                    } else {
                        $scope.popup.currentForm = form.form;
                        $scope.popup.formName = form.name || '';
                        resetActiveFormTab(form.form);
                    }
                });

                function resetActiveFormTab(form) {
                    if (form.tabs && form.tabs.length > 0) {
                        $scope.activeFormTab = form.tabs[0];
                    } else {
                        $scope.activeFormTab = undefined;
                    }
                };
                
            }]);