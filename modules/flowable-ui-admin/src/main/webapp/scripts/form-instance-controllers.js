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

activitiAdminApp.controller('FormInstanceController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', '$routeParams',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, $routeParams) {

        $rootScope.navigation = {main: 'form-engine', sub: 'instances'};

        $scope.returnToList = function () {
            $location.path("/form-instances");
        };

        $scope.showSubmittedForm = function () {
            $modal.open({
                templateUrl: 'views/form-render-popup.html',
                windowClass: 'modal modal-full-width',
                controller: 'ShowFormRenderPopupCrtl',
                resolve: {
                    form: function () {
                        return $scope.submittedForm;
                    }
                }
            });
        };
        
        $scope.executeWhenReady(function () {
            if ($rootScope.formInstance) {
                $scope.formInstance = $rootScope.formInstance;
                $rootScope.formInstance = undefined;
                return;
            }
            
            // Load submitted form
            $http({method: 'GET', url: '/app/rest/activiti/form-instances/' + $routeParams.formInstanceId}).
            success(function (data, status, headers, config) {
                $scope.formInstance = data;
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

activitiAdminApp.controller('ShowFormRenderPopupCrtl',
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
                            url: '/app/rest/activiti/form-instances/' + form.id
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