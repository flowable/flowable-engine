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
 * Input parameters for call activity
 */

angular.module('flowableModeler').controller('FlowableEventInParametersCtrl',
    ['$scope', '$modal', '$timeout', '$translate', function ($scope, $modal, $timeout, $translate) {

        // Config for the modal window
        var opts = {
            template: 'editor-app/configuration/properties/event-in-parameters-popup.html?version=' + Date.now(),
            scope: $scope
        };

        // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
    }]);

angular.module('flowableModeler').controller('FlowableEventInParametersPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing form properties on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null
            && $scope.property.value.inParameters !== undefined
            && $scope.property.value.inParameters !== null) {
            // Note that we clone the json object rather then setting it directly,
            // this to cope with the fact that the user can click the cancel button and no changes should have happened
            $scope.parameters = angular.copy($scope.property.value.inParameters);
        } else {
            $scope.parameters = [];
        }

        $scope.translationsRetrieved = false;
        $scope.labels = {};

        var variableNameLabel = $translate('PROPERTY.EVENTINPARAMETERS.VARIABLENAME');
        var eventNameLabel = $translate('PROPERTY.EVENTINPARAMETERS.EVENTNAME');

        $q.all([variableNameLabel, eventNameLabel]).then(function (results) {
            $scope.labels.variableNameLabel = results[1];
            $scope.labels.eventNameLabel = results[0];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.parameters,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [
                    {field: 'variableName', displayName: $scope.labels.variableNameLabel},
                    {field: 'eventName', displayName: $scope.labels.eventNameLabel}]
            };

            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedParameter = row.entity;
                });
            };
        });

        // Click handler for add button
        $scope.addNewParameter = function () {
            var newParameter = {
                variableName: '',
                eventName: '',
                eventType: 'string'
            };

            $scope.parameters.push(newParameter);

            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newParameter);
            });
        };

        // Click handler for remove button
        $scope.removeParameter = function () {

            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.parameters.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);
                $scope.parameters.splice(index, 1);

                if ($scope.parameters.length == 0) {
                    $scope.selectedParameter = undefined;
                }

                $timeout(function () {
                    if ($scope.parameters.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.parameters[0]);
                    }
                });
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.parameters.length > 0) {
                $scope.property.value = {};
                $scope.property.value.inParameters = $scope.parameters;
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