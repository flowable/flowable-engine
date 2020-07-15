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
 * Data Properties
 */

angular.module('flowableModeler').controller('FlowableDataPropertiesCtrl',
    ['$scope', '$modal', '$timeout', '$translate', function ($scope, $modal, $timeout, $translate) {

        // Config for the modal window
        var opts = {
            template: 'editor-app/configuration/properties/data-properties-popup.html?version=' + Date.now(),
            scope: $scope
        };

        // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
    }]);

angular.module('flowableModeler').controller('FlowableDataPropertiesPopupCtrl',
    ['$scope', '$q', '$translate', '$timeout', function ($scope, $q, $translate, $timeout) {

        // Put json representing data properties on scope
        if ($scope.property.value !== undefined && $scope.property.value !== null
            && $scope.property.value.items !== undefined
            && $scope.property.value.items !== null) {
            // Note that we clone the json object rather then setting it directly,
            // this to cope with the fact that the user can click the cancel button and no changes should have happened
            $scope.dataProperties = angular.copy($scope.property.value.items);
            
            for (var i = 0; i < $scope.dataProperties.length; i++) {
			    var dataProperty = $scope.dataProperties[i];
			    if (dataProperty.enumValues && dataProperty.enumValues.length > 0) {
				    for (var j = 0; j < dataProperty.enumValues.length; j++) {
					    var enumValue = dataProperty.enumValues[j];
					    if (!enumValue.id && !enumValue.name && enumValue.value) {
						    enumValue.id = enumValue.value;
						    enumValue.name = enumValue.value;
					    }
				    }
			    }
			}
            
        } else {
            $scope.dataProperties = [];
        }
        
        $scope.enumValues = [];

        $scope.translationsRetrieved = false;

        $scope.labels = {};

        var idPromise = $translate('PROPERTY.DATAPROPERTIES.ID');
        var namePromise = $translate('PROPERTY.DATAPROPERTIES.NAME');
        var typePromise = $translate('PROPERTY.DATAPROPERTIES.TYPE');
        var valuePromise = $translate('PROPERTY.DATAPROPERTIES.VALUE');

        $q.all([idPromise, namePromise, typePromise, valuePromise]).then(function (results) {
            $scope.labels.idLabel = results[0];
            $scope.labels.nameLabel = results[1];
            $scope.labels.typeLabel = results[2];
            $scope.labels.valueLabel = results[3];
            $scope.translationsRetrieved = true;

            // Config for grid
            $scope.gridOptions = {
                data: $scope.dataProperties,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{field: 'dataproperty_id', displayName: $scope.labels.idLabel},
                    {field: 'dataproperty_name', displayName: $scope.labels.nameLabel},
                    {field: 'dataproperty_type', displayName: $scope.labels.typeLabel},
                    {field: 'dataproperty_value', displayName: $scope.labels.valueLabel}]
            };
            
            $scope.enumGridOptions = {
    		    data: $scope.enumValues,
                headerRowHeight: 28,
                enableRowSelection: true,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 0,
                enableColumnMenus: false,
                enableSorting: false,
                columnDefs: [{ field: 'id', displayName: $scope.labels.idLabel },
                { field: 'name', displayName: $scope.labels.nameLabel}]
            }

            $scope.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.gridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedProperty = row.entity;
                    $scope.selectedEnumValue = undefined;
                    if ($scope.selectedProperty && $scope.selectedProperty.enumValues) {
                        $scope.enumValues.length = 0;
                        for (var i = 0; i < $scope.selectedProperty.enumValues.length; i++) {
                            $scope.enumValues.push($scope.selectedProperty.enumValues[i]);
                        }
                    }
                });
            };
            
            $scope.enumGridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.enumGridApi = gridApi;
                gridApi.selection.on.rowSelectionChanged($scope, function (row) {
                    $scope.selectedEnumValue = row.entity;
                });
            };
        });

        // Handler for when the value of the type dropdown changes
        $scope.propertyTypeChanged = function () {

            // Check date. If date, show date pattern
            if ($scope.selectedProperty.type === 'date') {
                $scope.selectedProperty.datePattern = 'MM-dd-yyyy hh:mm';
            } else {
                delete $scope.selectedProperty.datePattern;
            }

            // Check enum. If enum, show list of options
            if ($scope.selectedProperty.type === 'enum') {
                $scope.selectedProperty.enumValues = [ {id: 'value1', name: 'Value 1'}, {id: 'value2', name: 'Value 2'}];
                $scope.enumValues.length = 0;
                for (var i = 0; i < $scope.selectedProperty.enumValues.length; i++) {
                    $scope.enumValues.push($scope.selectedProperty.enumValues[i]);
                }
                
            } else {
                delete $scope.selectedProperty.enumValues;
                $scope.enumValues.length = 0;
            }
        };

        // Click handler for add button
        var propertyIndex = 1;
        $scope.addNewProperty = function () {
            var newProperty = {
                dataproperty_id: 'new_data_object_' + propertyIndex++,
                dataproperty_name: '',
                dataproperty_type: 'string',
                readable: true,
                writable: true
            };

            $scope.dataProperties.push(newProperty);

            $timeout(function () {
                $scope.gridApi.selection.toggleRowSelection(newProperty);
            });
        };

        // Click handler for remove button
        $scope.removeProperty = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.dataProperties.indexOf(selectedItems[0]);
                $scope.gridApi.selection.toggleRowSelection(selectedItems[0]);
                $scope.dataProperties.splice(index, 1);

                if ($scope.dataProperties.length == 0) {
                    $scope.selectedProperty = undefined;
                }

                $timeout(function() {
                    if ($scope.dataProperties.length > 0) {
                        $scope.gridApi.selection.toggleRowSelection($scope.dataProperties[0]);
                    }
                });
            }
        };

        // Click handler for up button
        $scope.movePropertyUp = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.dataProperties.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.dataProperties[index];
                    $scope.dataProperties.splice(index, 1);
                    $timeout(function(){
                        $scope.dataProperties.splice(index + -1, 0, temp);
                        $timeout(function() {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for down button
        $scope.movePropertyDown = function () {
            var selectedItems = $scope.gridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.dataProperties.indexOf(selectedItems[0]);
                if (index != $scope.dataProperties.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.dataProperties[index];
                    $scope.dataProperties.splice(index, 1);
                    $timeout(function(){
                        $scope.dataProperties.splice(index + 1, 0, temp);
                        $timeout(function() {
                            $scope.gridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };
        
        $scope.addNewEnumValue = function() {
            if ($scope.selectedProperty) {
        	    var newEnumValue = { id : '', name : ''};
        	    $scope.selectedProperty.enumValues.push(newEnumValue);
        	    $scope.enumValues.push(newEnumValue);
    	       
    	        $timeout(function () {
                    $scope.enumGridApi.selection.toggleRowSelection(newEnumValue);
                });
        	}
        };

        // Click handler for remove button
        $scope.removeEnumValue = function() {
            var selectedItems = $scope.enumGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.enumValues.indexOf(selectedItems[0]);
                $scope.enumGridApi.selection.toggleRowSelection(selectedItems[0]);

                $scope.enumValues.splice(index, 1);
                $scope.selectedProperty.enumValues.splice(index, 1);

                if ($scope.enumValues.length == 0) {
                    $scope.selectedEnumValue = undefined;
                }

                $timeout(function () {
                    if ($scope.enumValues.length > 0) {
                        $scope.enumGridApi.selection.toggleRowSelection($scope.enumValues[0]);
                    }
                });
            }
        };
    
        // Click handler for up button
        $scope.moveEnumValueUp = function() {
            var selectedItems = $scope.enumGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.enumValues.indexOf(selectedItems[0]);
                if (index != 0) { // If it's the first, no moving up of course
                    var temp = $scope.enumValues[index];
                    $scope.enumValues.splice(index, 1);
                    $scope.selectedProperty.enumValues.splice(index, 1);
                    $timeout(function () {
                        $scope.enumValues.splice(index + -1, 0, temp);
                        $scope.selectedProperty.enumValues.splice(index + -1, 0, temp);
                        $timeout(function () {
                            $scope.enumGridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };
    
        // Click handler for down button
        $scope.moveEnumValueDown = function() {
            var selectedItems = $scope.enumGridApi.selection.getSelectedRows();
            if (selectedItems && selectedItems.length > 0) {
                var index = $scope.enumValues.indexOf(selectedItems[0]);
                if (index != $scope.enumValues.length - 1) { // If it's the last element, no moving down of course
                    var temp = $scope.enumValues[index];
                    $scope.enumValues.splice(index, 1);
                    $scope.selectedProperty.enumValues.splice(index, 1);
                    $timeout(function () {
                        $scope.enumValues.splice(index + 1, 0, temp);
                        $scope.selectedProperty.enumValues.splice(index + 1, 0, temp);
                        $timeout(function () {
                            $scope.enumGridApi.selection.toggleRowSelection(temp);
                        });
                    });
                }
            }
        };

        // Click handler for save button
        $scope.save = function () {

            if ($scope.dataProperties.length > 0) {
                $scope.property.value = {};
                $scope.property.value.items = $scope.dataProperties;
            } else {
                $scope.property.value = null;
            }

            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        $scope.cancel = function () {
            $scope.$hide();
            $scope.property.mode = 'read';
        };

        // Close button handler
        $scope.close = function () {
            $scope.$hide();
            $scope.property.mode = 'read';
        };

    }])
;