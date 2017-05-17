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

var extScope;

angular.module('flowableModeler')
    .controller('DecisionTableEditorNewController', ['$rootScope', '$scope', '$q', '$translate', '$http', '$timeout', '$location', '$modal', '$route', '$routeParams', 'DecisionTableService',
        'UtilityService', 'uiGridConstants', 'appResourceRoot',
        function ($rootScope, $scope, $q, $translate, $http, $timeout, $location, $modal, $route, $routeParams, DecisionTableService,
                  UtilityService, uiGridConstants, appResourceRoot) {

            var MIN_COLUMN_WIDTH = 200;

            extScope = $scope;

            // Export name to grid's scope
            $scope.appResourceRoot = appResourceRoot;

            // Model init
            $scope.status = {loading: true};
            $scope.hotModel = {
                data: [],
                columnDefs: [],
                columnVariableIdMap: {}
            };


            var tmpRowData = [
                ['a1', 'b1', 'c1'],
                ['a2', 'b2', 'c2']
            ];

            var tmpColumnData = [

            ];


            $scope.alert = function () {
                console.log('!!!ALERT!!!');
            };

            $rootScope.decisionTableChanges = false;

            var hitPolicies = ['UNIQUE', 'FIRST', 'PRIORITY', 'ANY', 'RULE_ORDER', 'OUTPUT_ORDER', 'COLLECT'];

            var initHitPolicies = function () {
                $scope.hitPolicies = [];

                hitPolicies.forEach(function (id) {
                    $scope.hitPolicies.push({
                        id: id,
                        label: 'DECISION-TABLE.HIT-POLICIES.' + id
                    });
                });
            };

            $scope.ctrl = {
                settings: {},
                rowHeaders: true,
                columnHeader: true,
                columns: {},
                data: {}
            };

            initHitPolicies();


            $rootScope.currentDecisionTableRules = [];

            $scope.availableVariableTypes = ['string', 'number', 'boolean', 'date'];

            var columnIdCounter = 0;

            $scope.$on('$locationChangeStart', function (event, next, current) {
                var handleResponseFunction = function (discard) {
                    $scope.unsavedDecisionTableChangesModalInstance = undefined;
                    if (discard) {
                        $rootScope.ignoreChanges = true;
                        $location.url(next.substring(next.indexOf('/#') + 2));
                    } else {
                        $rootScope.ignoreChanges = false;
                        $rootScope.setMainPageById('decision-tables');
                    }
                };
                $scope.confirmNavigation(handleResponseFunction, event);
            });

            $scope.confirmNavigation = function (handleResponseFunction, event) {
                if (!$rootScope.ignoreChanges && $rootScope.decisionTableChanges) {

                    if (event) {
                        // Always prevent location from changing. We'll use a popup to determine the action we want to take
                        event.preventDefault();
                    }

                    $scope.handleResponseFunction = handleResponseFunction;
                    $scope.unsavedDecisionTableChangesModalInstance = _internalCreateModal({
                        template: 'editor-app/popups/unsaved-changes.html',
                        scope: $scope
                    }, $modal, $scope);
                } else {
                    // Clear marker
                    $rootScope.ignoreChanges = false;
                }

            };

            $scope.editInputExpression = function (column) {

                if (!column) {
                    return;
                }

                $scope.model.selectedColumn = column;
                var editTemplate = 'views/popup/decision-table-edit-input-expression.html';

                // get expression for selected column
                $scope.currentDecisionTable.inputExpressions.forEach(function (inputExpression) {
                    if (inputExpression.id === column.name) {
                        $scope.model.selectedExpression = inputExpression;
                    }
                });

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.editOutputExpression = function (column) {

                if (!column) {
                    return;
                }

                $scope.model.selectedColumn = column;
                var editTemplate = 'views/popup/decision-table-edit-output-expression.html';

                $scope.currentDecisionTable.outputExpressions.forEach(function (outputExpression) {
                    if (outputExpression.id === column.name) {
                        $scope.model.selectedExpression = outputExpression;
                    }
                });

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.addInputExpression = function (inputExpression, insertPos) {

                // add column def
                var newInputExpression;
                if (!inputExpression) {
                    newInputExpression = {
                        id: _generateColumnId(),
                        label: 'new input',
                        expressionType: 'input'
                    };
                } else {
                    newInputExpression = {
                        id: _generateColumnId(),
                        label: inputExpression.label,
                        variableId: inputExpression.variableId,
                        type: inputExpression.type,
                        variableType: inputExpression.variableType,
                        newVariable: inputExpression.newVariable
                    };
                }

                console.log('before');
                console.log($rootScope.currentDecisionTable);

                $scope.currentDecisionTable.inputExpressions.push(newInputExpression);

                // update column definitions off the source model
                $scope.getColumnDefinitions($scope.currentDecisionTable);

                // update hot
                console.log('inserting at pos: ' + $scope.currentDecisionTable.inputExpressions.length);
                hot.alter('insert_col', $scope.currentDecisionTable.inputExpressions.length - 1);


                console.log('after');
                console.log($rootScope.currentDecisionTable);

                if (hot) {
                    hot.render();
                }
            };

            $scope.enableRemoveInputExpression = function () {
                return $scope.currentDecisionTable && $scope.currentDecisionTable.inputExpressions && $scope.currentDecisionTable.inputExpressions.length > 1;
            };

            $scope.removeInputExpression = function (column, event) {

                if (!column) {
                    return;
                }

                // remove props from data
                $rootScope.currentDecisionTableRules.forEach(function (rowObject) {
                    if (rowObject.hasOwnProperty(column.name)) {
                        delete rowObject[column.name];
                    }
                });
                delete $scope.model.columnVariableIdMap[column.name];

                var expressionPos = -1;
                // remove input expression from table
                for (var i = 0; i < $scope.currentDecisionTable.inputExpressions.length; i++) {
                    if ($scope.currentDecisionTable.inputExpressions[i].id === column.name) {
                        $scope.currentDecisionTable.inputExpressions.splice(i, 1);
                        expressionPos = i;
                        break;
                    }

                }

                // set updated column definitions
                $scope.getColumnDefinitions($scope.currentDecisionTable);

                // prevent edit modal opening
                if (event) {
                    event.stopPropagation();
                }

                return expressionPos;
            };

            $scope.updateInputExpression = function (oldInputExpressionColumn, newInputExpression) {
                var deletedColumnIndex = $scope.removeInputExpression(oldInputExpressionColumn);
                $scope.addInputExpression(newInputExpression, deletedColumnIndex);
            };

            $scope.addOutputExpression = function (outputExpression, insertPos, skipExecuteAlter) {

                // add column def
                var newOutputExpression;
                if (!outputExpression) {
                    newOutputExpression = {
                        id: _generateColumnId(),
                        label: 'new',
                        expressionType: 'output'
                    };
                } else {
                    newOutputExpression = {
                        id: _generateColumnId(),
                        label: outputExpression.label,
                        variableId: outputExpression.variableId,
                        type: outputExpression.type,
                        variableType: outputExpression.variableType,
                        newVariable: outputExpression.newVariable
                    };
                }

                console.log('before');
                console.log($rootScope.currentDecisionTable);

                $scope.currentDecisionTable.outputExpressions.push(newOutputExpression);

                // update column definitions off the source model
                $scope.getColumnDefinitions($scope.currentDecisionTable);

                // update hot
                console.log('execute alter');

                // hot.alter('insert_col', $scope.currentDecisionTable.inputExpressions.length + $scope.currentDecisionTable.outputExpressions.length);

                console.log('after');
                console.log($rootScope.currentDecisionTable);

                if (hot) {
                    hot.render();
                }
            };

            $scope.enableRemoveOutputExpression = function () {
                return $scope.currentDecisionTable && $scope.currentDecisionTable.outputExpressions && $scope.currentDecisionTable.outputExpressions.length > 1;
            };

            $scope.removeOutputExpression = function (column, event) {

                if (!column) {
                    return;
                }

                // remove props from data
                $rootScope.currentDecisionTableRules.forEach(function (rowObject) {
                    if (rowObject.hasOwnProperty(column.name)) {
                        delete rowObject[column.name];
                    }
                });
                delete $scope.model.columnVariableIdMap[column.name];

                var expressionPos = -1;
                for (var i = 0; i < $scope.currentDecisionTable.outputExpressions.length; i++) {
                    if ($scope.currentDecisionTable.outputExpressions[i].id === column.name) {
                        $scope.currentDecisionTable.outputExpressions.splice(i, 1);
                        expressionPos = i;
                        break;
                    }
                }
                $scope.getColumnDefinitions($scope.currentDecisionTable);

                // prevent edit modal opening
                if (event) {
                    event.stopPropagation();
                }

                return expressionPos;
            };

            // create rule row with unique id
            $scope.addRule = function () {
                console.log('before');
                console.log($rootScope.currentDecisionTableRules);
                $rootScope.currentDecisionTableRules.push({});
                console.log('after');
                console.log($rootScope.currentDecisionTableRules);
                if (hot) {
                    hot.render();
                }
            };

            $scope.enableRemoveRule = function () {
                return $scope.model.selectedRule && $rootScope.currentDecisionTableRules.length > 1;
            };

            $scope.removeRule = function () {
                if (!$scope.model.selectedRule) {
                    return;
                }

                var index = $rootScope.currentDecisionTableRules.indexOf($scope.model.selectedRule);
                if (index > -1) {
                    $rootScope.currentDecisionTableRules.splice(index, 1);
                }

                $scope.model.selectedRule = undefined;
            };

            $scope.enableMoveUpwards = function (selectedRule) {
                return selectedRule && $rootScope.currentDecisionTableRules.indexOf(selectedRule) !== 0;
            };

            $scope.moveRuleUpwards = function () {
                var index = $rootScope.currentDecisionTableRules.indexOf($scope.model.selectedRule);
                if (index > -1) {
                    var row = $rootScope.currentDecisionTableRules[index];
                    $rootScope.currentDecisionTableRules.splice(index, 1);
                    $rootScope.currentDecisionTableRules.splice(index - 1, 0, row);
                }
            };

            $scope.enableMoveDownwards = function (selectedRule) {
                return selectedRule && $rootScope.currentDecisionTableRules.indexOf(selectedRule) !== ($rootScope.currentDecisionTableRules.length - 1);
            };

            $scope.moveRuleDownwards = function () {
                var index = $rootScope.currentDecisionTableRules.indexOf($scope.model.selectedRule);
                if (index > -1) {
                    var row = $rootScope.currentDecisionTableRules[index];
                    $rootScope.currentDecisionTableRules.splice(index, 1);
                    $rootScope.currentDecisionTableRules.splice(index + 1, 0, row);
                }
            };

            // helper for looking up variable id by col id
            $scope.getVariableNameByColumnId = function (colId) {

                if (!colId) {
                    return;
                }

                if ($scope.model.columnVariableIdMap[colId]) {
                    return $scope.model.columnVariableIdMap[colId];
                } else {
                    return $translate.instant('DECISION-TABLE-EDITOR.EMPTY-MESSAGES.NO-VARIABLE-SELECTED');
                }
            };


            var _loadDecisionTableDefinition = function (modelId) {

                DecisionTableService.fetchDecisionTableDetails(modelId).then(function (decisionTable) {

                    $rootScope.currentDecisionTable = decisionTable.decisionTableDefinition;
                    $rootScope.currentDecisionTable.id = decisionTable.id;
                    $rootScope.currentDecisionTable.key = decisionTable.decisionTableDefinition.key;
                    $rootScope.currentDecisionTable.name = decisionTable.name;
                    $rootScope.currentDecisionTable.description = decisionTable.description;

                    // decision table model to used in save dialog
                    $rootScope.currentDecisionTableModel = {
                        id: decisionTable.id,
                        name: decisionTable.name,
                        key: decisionTable.decisionTableDefinition.key,
                        description: decisionTable.description
                    };

                    if (!$rootScope.currentDecisionTable.hitIndicator) {
                        $rootScope.currentDecisionTable.hitIndicator = hitPolicies[0];
                    }
                    _initializeDecisionTableGrid($rootScope.currentDecisionTable);

                    $timeout(function () {
                        // Flip switch in timeout to start watching all decision-related models
                        // after next digest cycle, to prevent first false-positive
                        $scope.status.loading = false;
                        $rootScope.decisionTableChanges = false;
                    }, 200);

                });
            };

            var _initializeDecisionTableGrid = function (decisionTable) {

                // initialize hot model
                if (decisionTable.rules && decisionTable.rules.length > 0) {
                    decisionTable.rules.forEach(function (rule) {
                        $rootScope.currentDecisionTableRules.push(Object.values(rule));
                    });
                }


                // if no input condition present; add one
                if (!decisionTable.inputExpressions || decisionTable.inputExpressions.length === 0) {
                    $scope.addInputExpression();
                }

                // if no output conclusion present; add one
                if (!decisionTable.outputExpressions || decisionTable.outputExpressions.length === 0) {
                    $scope.addOutputExpression();

                }

                // get column definitions
                $scope.getColumnDefinitions(decisionTable);

                // console.log($rootScope.currentDecisionTableRules);
                console.log(decisionTable);
                console.log($scope.currentDecisionTableRules);

                console.log($scope.model.columnDefs);

                var createColHeaders = function () {

                    var hotColumDefs = [];

                    $scope.model.columnDefs.forEach(function (columnDef) {
                        var txt;
                        txt = '<span class="column-header label ' + columnDef.expressionType + '" ng-click="testUpdate(\'TEST\');">';
                        txt += columnDef.variableId;
                        txt += '</span>';
                        txt += '<input class="form-control" placeholder="enter name" type="text" ng-model="columnDef.variableId" />';
                        txt += '<span onClick="getScope(\'DecisionTableToolbarController\')">CLICK</span>';

                        hotColumDefs.push(txt);
                    });

                    return hotColumDefs;
                };

                $scope.testUpdate = function (val) {
                    console.log("###### GOT UPDATE: " + val);
                };

                $scope.ctrl.db = $scope.currentDecisionTableRules;
                $scope.ctrl.colHeaders = createColHeaders();

                var beforCreateColCallback = function (event) {
                    console.log('CALL BACK TRIGGERED');
                    console.log(event);

                    $scope.addOutputExpression()
                };
            };

            $scope.getHotColums = function (decisionTable) {
                return [
                    {
                        data: 'id',
                        title: 'ID',
                        readOnly: true
                    },
                    {
                        data: 'price',
                        title: 'Price',
                        readOnly: false
                    }]
            };

            // create UI grid column definitions based on input / output expression
            $scope.getColumnDefinitions = function (decisionTable) {

                if (!decisionTable) {
                    return;
                }

                var expressionCounter = 0;
                var newColumnDefs = [];

                // input expression column defs
                if (decisionTable.inputExpressions && decisionTable.inputExpressions.length > 0) {

                    decisionTable.inputExpressions.forEach(function (inputExpression) {

                        newColumnDefs.push({
                            name: inputExpression.id,
                            displayName: inputExpression.label ? inputExpression.label : "",
                            field: inputExpression.id,
                            type: 'string',
                            headerCellClass: 'header-expression header-input-expression',
                            // headerCellTemplate: _getHeaderInputExpressionCellTemplate(),
                            cellClass: 'cell-expression cell-input-expression',
                            // cellTemplate: _getCellTemplate(inputExpression.type),
                            enableHiding: false,
                            enableCellEditOnFocus: false,
                            minWidth: MIN_COLUMN_WIDTH,
                            cellEditableCondition: function ($scope) {
                                // check if column has been mapped to variable
                                if ($scope.grid.appScope.model.columnVariableIdMap[inputExpression.id]) {
                                    return true;
                                } else {
                                    return false;
                                }
                            },
                            expressionType: 'input',
                            variableId: inputExpression.variableId
                        });

                        expressionCounter++;
                    });
                }

                // output expression column defs
                if (decisionTable.outputExpressions && decisionTable.outputExpressions.length > 0) {

                    decisionTable.outputExpressions.forEach(function (outputExpression) {

                        newColumnDefs.push({
                            name: outputExpression.id,
                            displayName: outputExpression.label ? outputExpression.label : "",
                            field: outputExpression.id,
                            type: 'string',
                            // headerCellTemplate: _getHeaderOutputExpressionCellTemplate(),
                            headerCellClass: 'header-expression header-output-expression',
                            cellClass: 'cell-expression cell-output-expression',
                            // cellTemplate: _getCellTemplate(outputExpression.type),
                            enableHiding: false,
                            enableCellEditOnFocus: false,
                            minWidth: MIN_COLUMN_WIDTH,
                            cellEditableCondition: function ($scope) {
                                // check if column has been mapped to variable
                                if ($scope.grid.appScope.model.columnVariableIdMap[outputExpression.id]) {
                                    return true;
                                } else {
                                    return false;
                                }
                            },
                            expressionType: 'output',
                            variableId: outputExpression.variableId
                        });

                        expressionCounter++;
                    });
                }

                // merge models
                $scope.model.columnDefs.length = 0;
                Array.prototype.push.apply($scope.model.columnDefs, newColumnDefs);
            };


            // fetch table from service and populate model
            _loadDecisionTableDefinition($routeParams.modelId);

            $scope.changeDetector = function () {
                $rootScope.decisionTableChanges = true;
            };
            $rootScope.$watch('currentDecisionTable', $scope.changeDetector, true);
            $rootScope.$watch('currentDecisionTableRules', $scope.changeDetector, true);

            var _generateColumnId = function () {
                columnIdCounter++;
                return "" + columnIdCounter;
            };
        }]);

angular.module('flowableModeler')
    .controller('DecisionTableInputConditionEditorCtlr', ['$rootScope', '$scope', function ($rootScope, $scope) {
        var previousVariableId = $scope.model.selectedExpression.variableId;

        // condition input options
        $scope.popup = {
            selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
            selectedExpressionVariable: {id: previousVariableId}
        };

        $scope.save = function () {
            if (previousVariableId !== $scope.popup.selectedExpressionVariable.id) {

                var newInputExpression = {
                    label: $scope.popup.selectedExpressionLabel,
                    variableId: $scope.popup.selectedExpressionVariable.id,
                    newVariable: $scope.popup.selectedExpressionNewVariable
                };

                $scope.updateInputExpression($scope.model.selectedColumn, newInputExpression);

                if ($scope.popup.selectedExpressionNewVariable) {
                    saveNewDefinedVariable();
                }

            } else {
                $scope.model.selectedColumn.displayName = $scope.popup.selectedExpressionLabel;
            }

            $scope.close();
        };

        function saveNewDefinedVariable() {
            var newVariable = {
                processVariableName: $scope.popup.selectedExpressionVariable.id,
                processVariableType: $scope.model.selectedExpression.type
            };

            if ($scope.currentDecisionTable.executionVariables.indexOf(newVariable)) {
                $scope.currentDecisionTable.executionVariables.push(newVariable);
            }
        }

        $scope.setExpressionVariableType = function (variableType) {
            $scope.popup.selectedExpressionVariable = null;
            $scope.popup.selectedExpressionVariableType = variableType;
        };

        $scope.setNewVariable = function (value) {
            $scope.popup.selectedExpressionNewVariable = value;
            if (value) {
                $scope.setExpressionVariableType('variable');
            }
        };

        $scope.close = function () {
            $scope.$hide();
        };

        // Cancel button handler
        $scope.cancel = function () {
            $scope.close();
        };
    }]);

angular.module('flowableModeler')
    .controller('DecisionTableConclusionEditorCtrl', ['$rootScope', '$scope', '$q', '$translate', function ($rootScope, $scope, $q, $translate) {

        // condition input options
        $scope.popup = {
            selectedExpressionVariableType: '',
            selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
            selectedExpressionNewVariableType: $scope.availableVariableTypes[0]
        };

        $scope.popup.selectedExpressionNewVariableId = $scope.model.selectedExpression.variableId;
        $scope.popup.selectedExpressionNewVariableType = $scope.model.selectedExpression.type;

        // make copy of variable id and type to see if full update is needed
        var variableIdCopy = angular.copy($scope.model.selectedExpression.variableId);
        var newVariableIdCopy = angular.copy($scope.model.selectedExpressionNewVariableId);
        var newVariableTypeCopy = angular.copy($scope.model.selectedExpressionNewVariableType);

        // Cancel button handler
        $scope.cancel = function () {
            $scope.close();
        };

        // Saving the edited input
        $scope.save = function () {
            $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionNewVariableId;
            $scope.model.selectedExpression.type = $scope.popup.selectedExpressionNewVariableType;

            $scope.model.selectedExpression.newVariable = $scope.popup.selectedExpressionNewVariable;
            $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;

            if (variableIdCopy !== $scope.model.selectedExpression.variableId || newVariableIdCopy !== $scope.model.selectedExpressionNewVariableId || newVariableTypeCopy !== $scope.model.selectedExpressionNewVariableType) {

                // remove current column
                var deletedColumnIndex = $scope.removeOutputExpression($scope.model.selectedColumn);

                // add new conclusion
                $scope.addOutputExpression(
                    {
                        label: $scope.model.selectedExpression.label,
                        variableId: $scope.model.selectedExpression.variableId,
                        type: $scope.model.selectedExpression.type,
                        variableType: $scope.model.selectedExpression.variableType,
                        newVariable: $scope.model.selectedExpression.newVariable
                    },
                    deletedColumnIndex
                );
            } else {
                $scope.model.selectedColumn.displayName = $scope.model.selectedExpression.label;
            }

            $scope.close();
        };

        $scope.close = function () {
            $scope.$hide();
        };

    }]);
