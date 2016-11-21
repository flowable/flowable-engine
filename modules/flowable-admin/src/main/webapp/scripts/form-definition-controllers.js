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

activitiAdminApp.controller('FormDefinitionController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {
        $rootScope.navigation = {selection: 'process-definitions'};
        
        $scope.returnToList = function () {
            $location.path("/form-definitions");
        };

        $scope.showForm = function () {
            $modal.open({
                templateUrl: 'views/form-popup.html',
                windowClass: 'modal modal-full-width',
                controller: 'ShowFormPopupCrtl',
                resolve: {
                    form: function () {
                        return $scope.form;
                    }
                }
            });
        };

        $scope.showSubmittedForm = function (submittedForm) {
            if (submittedForm && submittedForm.getProperty('id')) {
                $location.path("/form-instance/"+submittedForm.getProperty('id'));
            }
        };

       $scope.openDeployment = function(deploymentId) {
            if (deploymentId) {
                $location.path("/deployment/" + deploymentId);
            }
        };

        $q.all([$translate('SUBMITTED-FORM.HEADER.ID'),
                $translate('SUBMITTED-FORM.HEADER.TASK-ID'),
                $translate('SUBMITTED-FORM.HEADER.PROCESS-ID'),
                $translate('SUBMITTED-FORM.HEADER.SUBMITTED'),
                $translate('SUBMITTED-FORM.HEADER.SUBMITTED-BY')])
            .then(function (headers) {

                $scope.gridSubmittedForms = {
                    data: 'submittedForms.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.showSubmittedForm,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'taskId', displayName: headers[1]},
                        {field: 'processInstanceId', displayName: headers[2]},
                        {field: 'submittedDate', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'submittedBy', displayName: headers[4], cellTemplate: gridConstants.userObjectTemplate}
                    ]
                };
            });

        $scope.executeWhenReady(function () {
            // Load form
            $http({method: 'GET', url: '/app/rest/activiti/form-definitions/' + $routeParams.formId}).
            success(function (data, status, headers, config) {
                $scope.form = data;

                // Load form submitted forms
                $http({
                    method: 'GET',
                    url: '/app/rest/activiti/form-definition-form-instances/' + $routeParams.formId
                }).
                success(function (submittedFormsData, status, headers, config) {
                    $scope.submittedForms = submittedFormsData;
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

activitiAdminApp.controller('ShowFormPopupCrtl',
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
                // Load form definition
                $http({
                    method: 'GET',
                    url: '/app/rest/activiti/form-definitions/' + form.id + '/editorJson'
                }).
                success(function (data, status, headers, config) {
                    $scope.popup.currentForm = data;
                    $scope.popup.formName = form.name || '';
                    resetActiveFormTab(data);
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

            function resetActiveFormTab(form) {
                if (form.tabs && form.tabs.length > 0) {
                    $scope.activeFormTab = form.tabs[0];
                } else {
                    $scope.activeFormTab = undefined;
                }
            };
            
        }]);
