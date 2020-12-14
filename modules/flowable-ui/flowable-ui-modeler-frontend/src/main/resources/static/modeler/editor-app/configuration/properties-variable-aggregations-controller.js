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
 * Variable Aggregations
 */

angular.module('flowableModeler').controller('FlowableVariableAggregationsCtrl',
    ['$scope', '$modal', '$timeout', '$translate', function ($scope, $modal, $timeout, $translate) {

        // Config for the modal window
        var opts = {
            template: 'editor-app/configuration/properties/variable-aggregations-popup.html?version=' + Date.now(),
            scope: $scope
        };

        // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
    }]);

angular.module('flowableModeler').controller('FlowableVariableAggregationsPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing form properties on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null
            && $scope.property.value.aggregations !== undefined
            && $scope.property.value.aggregations !== null) {
            // Note that we clone the json object rather then setting it directly,
            // this to cope with the fact that the user can click the cancel button and no changes should have happened
            $scope.aggregations = angular.copy($scope.property.value.aggregations);
        } else {
            $scope.aggregations = [];
        }

        $scope.definitions = [];
        $scope.translationsRetrieved = false;
        $scope.labels = {};

        var sourcePromise = $translate('PROPERTY.PARAMETER.SOURCE');
        var sourceExpressionPromise = $translate('PROPERTY.PARAMETER.SOURCEEXPRESSION');
        var targetPromise = $translate('PROPERTY.PARAMETER.TARGET');
        var targetExpressionPromise = $translate('PROPERTY.PARAMETER.TARGETEXPRESSION');

        $q.all([sourcePromise, sourceExpressionPromise, targetPromise, targetExpressionPromise]).then(function (results) {
            $scope.labels.sourceLabel = results[0];
            $scope.labels.sourceExpressionLabel = results[1];
            $scope.labels.targetLabel = results[2];
            $scope.labels.targetExpressionLabel = results[3];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.aggregations,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{field: 'target', displayName: $scope.labels.targetLabel},
                    {field: 'targetExpression', displayName: $scope.labels.targetExpressionLabel}]
            };

            $scope.definitionGridOptions = {
                data: $scope.definitions,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{ field: 'source', displayName: $scope.labels.sourceLabel },
                    { field: 'sourceExpression', displayName: $scope.labels.sourceExpressionLabel},
                    { field: 'target', displayName: $scope.labels.targetLabel},
                    { field: 'targetExpression', displayName: $scope.labels.targetExpressionLabel}]
            }


            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedAggregation = row.entity;
                    $scope.selectedVariableDefinitionValue = undefined;
                    if ($scope.selectedAggregation && $scope.selectedAggregation.definitions) {
                        $scope.definitions.length = 0;
                        for (var i = 0; i < $scope.selectedAggregation.definitions.length; i++) {
                            $scope.definitions.push($scope.selectedAggregation.definitions[i]);
                        }
                    }
                });
            };

            $scope.definitionGridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.definitionGridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                        $scope.selectedVariableDefinitionValue = row.entity;
                });
            };
        });

        // Click handler for add button
        $scope.addNewAggregation = function () {
            var newAggregation = {
                source: '',
                sourceExpression: '',
                target: '',
                targetExpression: ''};

            $scope.aggregations.push(newAggregation);
            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newAggregation);
            });
        };

        // Click handler for remove button
        $scope.removeAggregation = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.aggregations.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);
                $scope.aggregations.splice(index, 1);

                if ($scope.aggregations.length == 0) {
                    $scope.selectedAggregation = undefined;
                }

                $timeout(function () {
                    if ($scope.aggregations.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.aggregations[0]);
                    }
                });
            }
        };

        // Click handler for up button
        $scope.moveAggregationUp = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.aggregations.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.aggregations[index];
                    $scope.aggregations.splice(index, 1);
                    $timeout(function () {
                        $scope.aggregations.splice(index + -1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for down button
        $scope.moveAggregationDown = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.aggregations.indexOf(selectedItems[0]);
                if (index != $scope.aggregations.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.aggregations[index];
                    $scope.aggregations.splice(index, 1);
                    $timeout(function () {
                        $scope.aggregations.splice(index + 1, 0, temp);
                        $timeout(function () {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        $scope.addNewVariableDefinitionValue = function() {
            if ($scope.selectedAggregation) {
                var newDefinitionValue = {
                    source : '',
                    sourceExpression : '',
                    target : '',
                    targetExpression : '',
                };
                if ($scope.selectedAggregation.definitions) {
                    $scope.selectedAggregation.definitions.push(newDefinitionValue);
                } else {
                    $scope.selectedAggregation.definitions = [newDefinitionValue];
                }
                $scope.definitions.push(newDefinitionValue);

                $timeout(function () {
                    $scope.definitionGridApi.selection.toggleRowSelection(newDefinitionValue);
                });
            }
        };

        // Click handler for remove button
        $scope.removeVariableDefinitionValue = function() {
            var selectedItems = $scope.definitionGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.definitions.indexOf(selectedItems[0]);
                $scope.definitionGridApi.selection.toggleRowSelection(selectedItems[0]);

                $scope.definitions.splice(index, 1);
                $scope.selectedAggregation.definitions.splice(index, 1);

                if ($scope.definitions.length == 0) {
                    $scope.selectedVariableDefinitionValue = undefined;
                }

                $timeout(function () {
                    if ($scope.definitions.length > 0) {
                        $scope.definitionGridApi.selection.toggleRowSelection($scope.definitions[0]);
                    }
                });
            }
        };

        // Click handler for up button
        $scope.moveVariableDefinitionValueUp = function() {
            var selectedItems = $scope.definitionGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.definitions.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.definitions[index];
                    $scope.definitions.splice(index, 1);
                    $scope.selectedAggregation.definitions.splice(index, 1);
                    $timeout(function () {
                        $scope.definitions.splice(index + -1, 0, temp);
                        $scope.selectedAggregation.definitions.splice(index + -1, 0, temp);
                        $timeout(function () {
                            $scope.definitionGridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for down button
        $scope.moveVariableDefinitionValueDown = function() {
            var selectedItems = $scope.enumGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.definitions.indexOf(selectedItems[0]);
                if (index != $scope.definitions.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.definitions[index];
                    $scope.definitionGridApi.splice(index, 1);
                    $scope.selectedAggregation.definitions.splice(index, 1);
                    $timeout(function () {
                        $scope.definitions.splice(index + 1, 0, temp);
                        $scope.selectedAggregation.definitions.splice(index + 1, 0, temp);
                        $timeout(function () {
                            $scope.definitionGridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.aggregations.length > 0) {
                $scope.property.value = {};
                $scope.property.value.aggregations = $scope.aggregations;
            } else {
                $scope.property.value = null;
            }

            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.close();
        };

        // Close button handler
        $scope.close = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };

    }]);