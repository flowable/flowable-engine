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
 * Task listeners
 */

angular.module('flowableModeler').controller('FlowableExceptionsCtrl',
    ['$scope', '$modal', '$timeout', '$translate', function ($scope, $modal, $timeout, $translate) {

        // Config for the modal window
        var opts = {
            template: 'editor-app/configuration/properties/exceptions-popup.html?version=' + Date.now(),
            scope: $scope
        };

        // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
    }]);


angular.module('flowableModeler').controller('FlowableExceptionsPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing form properties on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null
            && $scope.property.value.exceptions !== undefined
            && $scope.property.value.exceptions !== null) {

            // Note that we clone the json object rather then setting it directly,
            // this to cope with the fact that the user can click the cancel button and no changes should have happened
            $scope.exceptions = angular.copy($scope.property.value.exceptions);

        } else {
            $scope.exceptions = [];
        }

        $scope.translationsRetrieved = false;
        $scope.labels = {};

        var codePromise = $translate('PROPERTY.EXCEPTIONS.CODE');
        var classPromise = $translate('PROPERTY.EXCEPTIONS.CLASS');

        $q.all([codePromise, classPromise]).then(function (results) {
            $scope.labels.codeLabel = results[0];
            $scope.labels.classLabel = results[1];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.exceptions,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{field: 'code', displayName: $scope.labels.codeLabel},
                    {field: 'class', displayName: $scope.labels.classLabel}]
            };

            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedException = row.entity;
                });
            };
        });

        $scope.exceptionDetailsChanged = function () {
            if ($scope.selectedException.class != '') {
                $scope.selectedException.class = $scope.selectedException.class;
            }
       
        };

        // Click handler for add button
        $scope.addNewException = function () {
            var newException = {
                code: '',
                class: '',
                children: false
            };

            $scope.exceptions.push(newException);
            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newException);
            });
        };

        // Click handler for remove button
        $scope.removeException = function () {

            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.exceptions.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);
                $scope.exceptions.splice(index, 1);

                if ($scope.exceptions.length == 0) {
                    $scope.selectedException = undefined;
                }

                $timeout(function () {
                    if ($scope.exceptions.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.exceptions[0]);
                    }
                });
            }
        };

        // Click handler for up button
        $scope.moveExceptionUp = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.exceptions.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.exceptions[index];
                    $scope.exceptions.splice(index, 1);
                    $timeout(function () {
                        $scope.exceptions.splice(index + -1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for down button
        $scope.moveExceptionDown = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.exceptions.indexOf(selectedItems[0]);
                if (index != $scope.exceptions.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.exceptions[index];
                    $scope.exceptions.splice(index, 1);
                    $timeout(function () {
                        $scope.exceptions.splice(index + 1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.exceptions.length > 0) {
                $scope.property.value = {};
                $scope.property.value.exceptions = $scope.exceptions;
            } else {
                $scope.property.value = null;
            }

            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.$hide();
        };

        // Close button handler
        $scope.close = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };
    }]);
