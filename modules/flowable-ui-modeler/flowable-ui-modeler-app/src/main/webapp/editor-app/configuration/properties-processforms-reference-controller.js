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

/*
 * Execution listeners
 */

angular.module('flowableModeler').controller('FlowableProcessFormsReferenceCtrl',
    ['$scope', '$modal', '$timeout', '$translate', function ($scope, $modal, $timeout, $translate) {

        // Config for the modal window
        var opts = {
            template: 'editor-app/configuration/properties/processforms-reference-popup.html?version=' + Date.now(),
            scope: $scope
        };

        // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
    }]);

angular.module('flowableModeler').controller('FlowableProcessFormsReferencePopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing form properties on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null
            ) {

            if ($scope.property.value.constructor == String) {
                $scope.viewProcessForms = JSON.parse($scope.property.value);
            }
            else {
                // Note that we clone the json object rather then setting it directly,
                // this to cope with the fact that the user can click the cancel button and no changes should have happened
                $scope.viewProcessForms = angular.copy($scope.property.value);
            }
        } else {
            $scope.viewProcessForms = [];
        }

        $scope.selectedForm = undefined;

        $scope.fields = [];
        $scope.translationsRetrieved = false;

        $scope.labels = {};

        var userPromise = $translate('PROPERTY.PROCESS.FORMREFERENCE.USERS');
        var rolePromise = $translate('PROPERTY.PROCESS.FORMREFERENCE.ROLES');
        var formPromise = $translate('PROPERTY.PROCESS.FORMREFERENCE.FORM');

        $q.all([userPromise, rolePromise, formPromise]).then(function (results) {
            $scope.labels.usersLabel = results[0];
            $scope.labels.rolesLabel = results[1];
            $scope.labels.formreferenceLabel = results[2];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.viewProcessForms,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{field: 'users', displayName: $scope.labels.usersLabel},
                    {field: 'roles', displayName: $scope.labels.rolesLabel},
                    {field: 'formreference.key', displayName: $scope.labels.formreferenceLabel}]
            };

            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedForm = row.entity;
                });
            };
        });


        $scope.formDetailsChanged = function () {
            if ($scope.selectedForm.className !== '') {
                $scope.selectedForm.implementation = $scope.selectedForm.className;
            } else if ($scope.selectedForm.expression !== '') {
                $scope.selectedForm.implementation = $scope.selectedForm.expression;
            } else if ($scope.selectedForm.delegateExpression !== '') {
                $scope.selectedForm.implementation = $scope.selectedForm.delegateExpression;
            } else {
                $scope.selectedForm.implementation = '';
            }
        };

        $scope.propertyClicked = function (propertyName) {
            $scope.property = {};
            $scope.property.value = $scope.selectedForm.formreference;
            $scope.property.mode = 'write'
            $scope.property.name = propertyName;
        };

        $scope.updatePropertyInModel = function (property, shapeId) {

            $scope.selectedForm.formreference = property.value;
            $scope.property.mode='read';

            if ($scope.viewProcessForms.length > 0) {
                $scope.$parent.property.value = $scope.viewProcessForms;
            } else {
                $scope.$parent.property.value = null;
            }

            $scope.$parent.updatePropertyInModel($scope.$parent.property);


        };

        $scope.updatePropertyInModelForDialog = function (property) {
            $scope.selectedForm.formreference = property.value;
        };

        // Click handler for add button
        $scope.addNewForm = function () {
            var newForm = {
                formreference: '',
                roles: '',
                users: ''
            };
            $scope.viewProcessForms.push(newForm);

            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newForm);
            });
        };

        // Click handler for remove button
        $scope.removeForm = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.viewProcessForms.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);

                $scope.viewProcessForms.splice(index, 1);

                if ($scope.viewProcessForms.length == 0) {
                    $scope.selectedForm = undefined;
                }

                $timeout(function () {
                    if ($scope.viewProcessForms.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.viewProcessForms[0]);
                    }
                });
            }
        };

        // Click handler for up button
        $scope.moveFormUp = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.viewProcessForms.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.viewProcessForms[index];
                    $scope.viewProcessForms.splice(index, 1);
                    $timeout(function () {
                        $scope.viewProcessForms.splice(index + -1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for down button
        $scope.moveFormDown = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.viewProcessForms.indexOf(selectedItems[0]);
                if (index != $scope.viewProcessForms.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.viewProcessForms[index];
                    $scope.viewProcessForms.splice(index, 1);
                    $timeout(function () {
                        $scope.viewProcessForms.splice(index + 1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.viewProcessForms.length > 0) {
                var valid = true;
                $scope.viewProcessForms.forEach(function(form) {
                   if(!((form.users||form.roles)&&form.formreference))  {
                       alert("Please define at least users or roles and form");
                       valid = false;
                   }
                });
                if(!valid) {
                    return;
                }
                $scope.$parent.property.value = $scope.viewProcessForms;
            } else {
                $scope.$parent.property.value = [];
            }

            $scope.$parent.updatePropertyInModel($scope.$parent.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.$hide();
            $scope.$parent.property.mode = 'read';
        };

        // Close button handler
        $scope.close = function () {
            $scope.$hide();
            $scope.$parent.property.mode = 'read';
        };

    }]);
