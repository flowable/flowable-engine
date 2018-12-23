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

angular.module('flowableModeler').controller('FlowableErrorDefinitionsCtrl', ['$scope', '$modal', function ($scope, $modal) {

    // Config for the modal window
    var opts = {
        template: 'editor-app/configuration/properties/error-definitions-popup.html?version=' + Date.now(),
        scope: $scope
    };

    // Open the dialog
    _internalCreateModal(opts, $modal, $scope);
}]);

//Need a separate controller for the modal window due to https://github.com/angular-ui/bootstrap/issues/259
// Will be fixed in a newer version of Angular UI
angular.module('flowableModeler').controller('FlowableErrorDefinitionsPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing mesage definitions on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null && $scope.property.value.length > 0) {

            if ($scope.property.value.constructor == String) {
                $scope.errorDefinitions = JSON.parse($scope.property.value);
            }
            else {
                // Note that we clone the json object rather then setting it directly,
                // this to cope with the fact that the user can click the cancel button and no changes should have happened
                $scope.errorDefinitions = angular.copy($scope.property.value);
            }

        } else {
            $scope.errorDefinitions = [];
        }

        // Array to contain selected mesage definitions (yes - we only can select one, but ng-grid isn't smart enough)
        $scope.selectedErrorDefinition = undefined;
        $scope.translationsRetrieved = false;

        $scope.labels = {};

        var idPromise = $translate('PROPERTY.ERRORDEFINITIONS.ID');
        var namePromise = $translate('PROPERTY.ERRORDEFINITIONS.ERRORCODE');

        $q.all([idPromise, namePromise]).then(function (results) {

            $scope.labels.idLabel = results[0];
            $scope.labels.nameLabel = results[1];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.errorDefinitions,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [
                    {field: 'id', displayName: $scope.labels.idLabel},
                    {field: 'errorcode', displayName: $scope.labels.nameLabel}]
            };

            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedErrorDefinition = row.entity;
                });
            };
        });

        // Click handler for add button
        $scope.addNewErrorDefinition = function () {
            var newErrorDefinition = {id: '', errorcode: ''};

            $scope.errorDefinitions.push(newErrorDefinition);
            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newErrorDefinition);
            });
        };

        // Click handler for remove button
        $scope.removeErrorDefinition = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.errorDefinitions.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);
                $scope.errorDefinitions.splice(index, 1);

                if ($scope.errorDefinitions.length == 0) {
                    $scope.selectedErrorDefinition = undefined;
                }

                $timeout(function () {
                    if ($scope.errorDefinitions.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.errorDefinitions[0]);
                    }
                });
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.errorDefinitions.length > 0) {
                $scope.property.value = $scope.errorDefinitions;
            } else {
                $scope.property.value = null;
            }

            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };

        // Close button handler
        $scope.close = function () {
            $scope.property.mode = 'read';
            $scope.$hide();
        };

    }]);