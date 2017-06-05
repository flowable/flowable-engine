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
    .controller('DecisionTableEditorNewController2', ['$rootScope', '$scope', '$q', '$translate', '$http', '$timeout', '$location', '$modal', '$route', '$routeParams', 'DecisionTableService',
        'UtilityService', 'uiGridConstants', 'appResourceRoot', 'hotRegisterer', '$compile',
        function ($rootScope, $scope, $q, $translate, $http, $timeout, $location, $modal, $route, $routeParams, DecisionTableService,
                  UtilityService, uiGridConstants, appResourceRoot, hotRegisterer, $compile) {

            extScope = $scope;

            // Export name to grid's scope
            $scope.appResourceRoot = appResourceRoot;

            // Model init
            $scope.status = {loading: true};
            $scope.model = {
                columnDefs: [],
                columnVariableIdMap: {},
                startOutputExpression: 0
            };

            var hotDecisionTableEditorInstance;

            $scope.availableVariableTypes = ['string', 'number', 'boolean', 'date'];
            $scope.inputOperators = ['==', '!=', '<', '>'];

            $scope.doAfterGetColHeader = function (col, TH) {
                if ($scope.model.columnDefs[col] && $scope.model.columnDefs[col].expressionType === 'input-operator') {
                    TH.className += "input-operator-header";
                } else if ($scope.model.columnDefs[col] && $scope.model.columnDefs[col].expressionType === 'input-expression') {
                    TH.className += "input-expression-header";
                    if ($scope.model.startOutputExpression - 1 === col) {
                        TH.className += " last";
                    }
                } else if ($scope.model.columnDefs[col] && $scope.model.columnDefs[col].expressionType === 'output') {
                    TH.className += "output-header";
                    if ($scope.model.startOutputExpression === col) {
                        TH.className += " first";
                    }
                }
            };

            $scope.doAfterModifyColWidth = function (width, col) {
                if ($scope.model.columnDefs[col] && $scope.model.columnDefs[col].width) {
                    var settingsWidth = $scope.model.columnDefs[col].width;
                    if (settingsWidth > width) {
                        return settingsWidth;
                    }
                }
                return width;
            };

            $scope.doAfterOnCellMouseDown = function (event, coords, TD) {
                // clicked hit policy indicator
                if (coords.row === 0 && coords.col === 0) {
                    $scope.openHitPolicyEditor();
                }
            };

            $scope.doAfterRender = function () {
                hotDecisionTableEditorInstance = hotRegisterer.getInstance('decision-table-editor');
                var element = document.querySelector("thead > tr > th:first-of-type");
                if (element) {
                    var firstChild = element.firstChild;
                    var newElement = angular.element('<div class="hit-policy-header">' + $scope.currentDecisionTable.hitIndicator.substring(0, 1) + '</div>');
                    element.replaceChild(newElement[0], firstChild);
                }
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.validateCells();
                }
            };

            var columnIdCounter = 0;

            // Hot Model init
            $scope.hotModel = {
                settings: {
                    contextMenu: {
                        callback: function (key, options) {
                            if (key === 'about') {
                                setTimeout(function () {
                                    // timeout is used to make sure the menu collapsed before alert is shown
                                    alert("This is a context menu with default and custom options mixed");
                                }, 100);
                            }
                        },
                        items: {
                            "row_above": {
                                name: 'Add rule above',
                                disabled: function () {
                                    // if first row, disable this option
                                    if (hotDecisionTableEditorInstance.getSelected()) {
                                        return hotDecisionTableEditorInstance.getSelected()[0] === 0;
                                    } else {
                                        return false;
                                    }
                                }
                            },
                            "row_below": {},
                            "remove_row": {
                                name: 'Remove this rule, ok?',
                                disabled: function () {
                                    // if first row, disable this option
                                    if (hotDecisionTableEditorInstance.getSelected()) {
                                        return hotDecisionTableEditorInstance.getSelected()[0] === 0;
                                    } else {
                                        return false;
                                    }
                                }
                            },
                            "hsep1": "---------",
                            "add_input": {
                                name: 'Add input',
                                disabled: function () {
                                    if (hotDecisionTableEditorInstance.getSelected()) {
                                        return ((hotDecisionTableEditorInstance.getSelected()[1] / 2) >= $scope.model.startOutputExpression);
                                    } else {
                                        return false;
                                    }
                                },
                                callback: function (key, options) {
                                    $scope.openInputExpressionEditor(options.end.col, true);
                                }
                            },
                            "add_output": {
                                name: 'Add output',
                                disabled: function () {
                                    if (hotDecisionTableEditorInstance.getSelected()) {
                                        return ((hotDecisionTableEditorInstance.getSelected()[1] / 2) < $scope.model.startOutputExpression);
                                    } else {
                                        return false;
                                    }

                                },
                                callback: function (key, options) {
                                    $scope.openOutputExpressionEditor(options.end.col, true);
                                }
                            },
                            "hsep2": "---------",
                            "about": {name: 'About this menu'},

                        }
                    },
                    stretchH: 'all'
                }
            };


            $scope.dumpData = function () {
                console.log($scope.currentDecisionTable);
                console.log($scope.rulesData);
            };

            $scope.testHotCall = function () {
                console.log('number of rows 1: ' + hotRegisterer.getInstance('decision-table-editor').countRows());
                console.log('number of rows 2: ' + hotDecisionTableEditorInstance.countRows());
            };

            $rootScope.decisionTableChanges = false;

            var hitPolicies = ['FIRST', 'ANY', 'UNIQUE', 'PRIORITY', 'RULE_ORDER', 'OUTPUT_ORDER', 'COLLECT'];
            $scope.hitPolicies = [];
            hitPolicies.forEach(function (id) {
                $scope.hitPolicies.push({
                    id: id,
                    label: 'DECISION-TABLE.HIT-POLICIES.' + id
                });
            });

            $scope.rulesData;

            $scope.addNewInputExpression = function (inputExpression, insertPos) {
                if (!$scope.currentDecisionTable.inputExpressions) {
                    $scope.currentDecisionTable.inputExpressions = [];
                }

                var newInputExpression = {
                    id: _generateColumnId(),
                    label: inputExpression.label,
                    variableId: inputExpression.variableId,
                    type: inputExpression.type,
                    variableType: inputExpression.variableType,
                    newVariable: inputExpression.newVariable
                };

                // insert expression at position or just add
                if (insertPos !== undefined && insertPos !== -1) {
                    $scope.currentDecisionTable.inputExpressions.splice(insertPos, 0, newInputExpression);
                } else {
                    $scope.currentDecisionTable.inputExpressions.push(newInputExpression);
                }

                // update column definitions off the source model
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.addNewOutputExpression = function (outputExpression, insertPos) {
                if (!$scope.currentDecisionTable.outputExpressions) {
                    $scope.currentDecisionTable.outputExpressions = [];
                }

                var newOutputExpression = {
                    id: _generateColumnId(),
                    label: outputExpression.label,
                    variableId: outputExpression.variableId,
                    type: outputExpression.type,
                    variableType: outputExpression.variableType,
                    newVariable: outputExpression.newVariable
                };

                // insert expression at position or just add
                if (insertPos !== undefined && insertPos !== -1) {
                    $scope.currentDecisionTable.outputExpressions.splice(insertPos, 0, newOutputExpression);
                } else {
                    $scope.currentDecisionTable.outputExpressions.push(newOutputExpression);
                }

                // update column definitions off the source model
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.removeInputExpression = function (expressionPos) {
                $scope.currentDecisionTable.inputExpressions.splice(expressionPos, 1);
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.removeOutputExpression = function (expressionPos) {
                $scope.currentDecisionTable.outputExpressions.splice(expressionPos, 1);
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.openInputExpressionEditor = function (expressionPos, newExpression) {

                $scope.model.newExpression = !!newExpression;
                $scope.model.selectedColumn = expressionPos;

                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.currentDecisionTable.inputExpressions[expressionPos];
                }

                var editTemplate = 'views/popup/decision-table-edit-input-expression.html';

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openOutputExpressionEditor = function (expressionPos, newExpression) {

                $scope.model.newExpression = !!newExpression;
                $scope.model.selectedColumn = expressionPos;
                $scope.model.hitPolicy = $scope.currentDecisionTable.hitIndicator;

                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.currentDecisionTable.outputExpressions[expressionPos];
                }

                var editTemplate = 'views/popup/decision-table-edit-output-expression.html';

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openHitPolicyEditor = function () {

                $scope.model.hitPolicy = $scope.currentDecisionTable.hitIndicator;

                var editTemplate = 'views/popup/decision-table-edit-hit-policy.html';

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
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

                    evaluateDecisionTableGrid($rootScope.currentDecisionTable);

                    $timeout(function () {
                        // Flip switch in timeout to start watching all decision-related models
                        // after next digest cycle, to prevent first false-positive
                        $scope.status.loading = false;
                        $rootScope.decisionTableChanges = false;
                    });

                });
            };

            var composeInputOperatorColumnDefinition = function (inputExpression) {
                var expressionPosition = $scope.currentDecisionTable.inputExpressions.indexOf(inputExpression);

                var columnDefinition = {
                    data: inputExpression.id + '_operator',
                    expressionType: 'input-operator',
                    expression: inputExpression,
                    width: '70',
                    className: 'input-operator-cell',
                    type: 'dropdown',
                    source: ['==', '!=', '<', '>']
                };

                if ($scope.currentDecisionTable.inputExpressions.length !== 1) {
                    columnDefinition.title = '<div class="header-remove-expression">' +
                        '<a onclick="triggerRemoveExpression(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-minus-sign"></span></a>' +
                        '</div>';
                }

                return columnDefinition;
            };

            var composeInputExpressionColumnDefinition = function (inputExpression) {
                var expressionPosition = $scope.currentDecisionTable.inputExpressions.indexOf(inputExpression);

                var columnDefinition = {
                    data: inputExpression.id + '_expression',
                    title: '<div class="input-header">' +
                    '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',false)"><span class="header-label">' + inputExpression.label + '</span></a>' +
                    '<br> <span class="header-variable">' + inputExpression.variableId + '</span>' +
                    '</div>' +
                    '<div class="header-add-new-expression">' +
                    '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                    '</div>',
                    expressionType: 'input-expression',
                    expression: inputExpression,
                    className: 'htCenter',
                    width: '270'
                };

                if (inputExpression.entries && inputExpression.entries.length > 0) {
                    var entriesOptionValues = inputExpression.entries.slice(0, inputExpression.entries.length);
                    entriesOptionValues.push('-', '', ' ');

                    columnDefinition.type = 'dropdown';
                    columnDefinition.source = entriesOptionValues;

                    columnDefinition.title = '<div class="input-header">' +
                        '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',false)"><span class="header-label">' + inputExpression.label + '</span></a>' +
                        '<br> <span class="header-variable">' + inputExpression.variableId + '</span>' +
                        '<br> <span class="header-entries">' + inputExpression.entries.join() + '</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';
                }

                return columnDefinition;
            };

            var composeOutputColumnDefinition = function (outputExpression) {
                var expressionPosition = $scope.currentDecisionTable.outputExpressions.indexOf(outputExpression);

                var title = '';

                var columnDefinition = {
                    data: outputExpression.id,
                    expressionType: 'output',
                    expression: outputExpression,
                    className: 'htCenter',
                    width: '270'
                };

                if ($scope.currentDecisionTable.outputExpressions.length !== 1) {
                    title = '<div class="header-remove-expression">' +
                        '<a onclick="triggerRemoveExpression(\'output\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-minus-sign"></span></a>' +
                        '</div>';
                }

                if (outputExpression.entries && outputExpression.entries.length > 0) {
                    var entriesOptionValues = outputExpression.entries.slice(0, outputExpression.entries.length);
                    entriesOptionValues.push('-', '', ' ');

                    columnDefinition.type = 'dropdown';
                    columnDefinition.source = entriesOptionValues;

                    title += '<div class="output-header">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',false)"><span class="header-label">' + outputExpression.label + '</span></a>' +
                        '<br> <span class="header-variable">' + outputExpression.variableId + '</span>' +
                        '<br> <span class="header-entries">' + outputExpression.entries.join() + '</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';
                } else {
                    title += '<div class="output-header">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',false)"><span class="header-label">' + outputExpression.label + '</span></a>' +
                        '<br> <span class="header-variable">' + outputExpression.variableId + '</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';
                }

                columnDefinition.title = title;

                return columnDefinition;
            };

            $scope.evaluateDecisionHeaders = function (decisionTable) {
                var columnDefinitions = [];
                var inputExpressionCounter = 0;
                if (decisionTable.inputExpressions && decisionTable.inputExpressions.length > 0) {
                    decisionTable.inputExpressions.forEach(function (inputExpression) {
                        columnDefinitions.push(composeInputOperatorColumnDefinition(inputExpression));
                        columnDefinitions.push(composeInputExpressionColumnDefinition(inputExpression));

                        inputExpressionCounter += 2;
                    });
                }

                columnDefinitions[inputExpressionCounter - 1].className += ' last';
                $scope.model.startOutputExpression = inputExpressionCounter;

                if (decisionTable.outputExpressions && decisionTable.outputExpressions.length > 0) {
                    decisionTable.outputExpressions.forEach(function (outputExpression) {
                        columnDefinitions.push(composeOutputColumnDefinition(outputExpression));
                    });
                }

                columnDefinitions[inputExpressionCounter].className += ' first';

                // timeout needed for trigger hot update when removing column defs
                $timeout(function () {
                    $scope.model.columnDefs = columnDefinitions;
                    hotDecisionTableEditorInstance.render();
                });
            };

            var evaluateDecisionGrid = function (decisionTable) {
                var tmpRuleGrid = [];

                // rows
                if (decisionTable.rules && decisionTable.rules.length > 0) {
                    decisionTable.rules.forEach(function (rule) {

                        var tmpRowValues = {};
                        // rule data
                        for (var i = 0; i < Object.keys(rule).length; i++) {
                            var id = Object.keys(rule)[i];

                            // set counter to max value
                            var expressionId = 0;
                            try {
                                expressionId = parseInt(id);
                            } catch (e) {
                            }
                            if (expressionId > columnIdCounter) {
                                columnIdCounter = expressionId;
                            }

                            if (i < ($scope.model.startOutputExpression / 2)) {
                                var operator;
                                var expression;
                                if (rule[id]) {
                                    operator = rule[id].substring(0, 2);
                                    expression = rule[id].substring(3, rule[id].length);
                                    expression = expression.replace(/"/g, "");
                                } else {
                                    expression = '-';
                                }

                                var operatorId = id + '_operator';
                                var expressionId = id + '_expression';

                                tmpRowValues[operatorId] = operator;
                                tmpRowValues[expressionId] = expression;
                            } else {
                                var outputExpression;
                                if (rule[id]) {
                                    outputExpression = rule[id].replace(/"/g, "");
                                } else {
                                    outputExpression = rule[id];
                                }
                                tmpRowValues[id] = outputExpression;
                            }
                        }

                        tmpRuleGrid.push(tmpRowValues);
                    });
                }
                $scope.rulesData = tmpRuleGrid;
            };

            var evaluateDecisionTableGrid = function (decisionTable) {
                $scope.evaluateDecisionHeaders(decisionTable);
                evaluateDecisionGrid(decisionTable);
            };


            // fetch table from service and populate model
            _loadDecisionTableDefinition($routeParams.modelId);

            var _generateColumnId = function () {
                columnIdCounter++;
                return "" + columnIdCounter;
            };

        }]);

angular.module('flowableModeler')
    .controller('DecisionTableInputConditionEditorCtlr', ['$rootScope', '$scope', function ($rootScope, $scope) {
        var getEntriesValues = function (entriesArrayOfArrays) {
            var newEntriesArray = [];
            // remove last value
            entriesArrayOfArrays.pop();

            entriesArrayOfArrays.forEach(function (entriesArray) {
                newEntriesArray.push(entriesArray[0]);
            });

            return newEntriesArray;
        };

        var createEntriesValues = function (entriesArray) {
            var entriesArrayOfArrays = [];
            while (entriesArray.length) entriesArrayOfArrays.push(entriesArray.splice(0, 1));
            return entriesArrayOfArrays;
        };

        var deleteRowRenderer = function (instance, td, row) {
            td.innerHTML = '';
            td.className = 'remove_container';

            if ((row + 1) != $scope.popup.selectedExpressionInputValues.length) {
                var div = document.createElement('div');
                div.onclick = function () {
                    return instance.alter("remove_row", row);
                };
                div.className = 'btn';
                div.appendChild(document.createTextNode('x'));
                td.appendChild(div);
            }
            return td;
        };

        // condition input options
        if ($scope.model.newExpression === false) {
            $scope.popup = {
                selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
                selectedExpressionVariableId: $scope.model.selectedExpression.variableId,
                selectedExpressionInputValues: $scope.model.selectedExpression.entries && $scope.model.selectedExpression.entries.length > 0 ? createEntriesValues($scope.model.selectedExpression.entries) : [['']],
                columnDefs: [
                    {
                        width: '300'
                    },
                    {
                        width: '40',
                        readOnly: true,
                        renderer: deleteRowRenderer
                    }
                ],
                hotSettings: {
                    stretchH: 'none'
                }
            };
        } else {
            $scope.popup = {
                selectedExpressionLabel: '',
                selectedExpressionVariableId: '',
                selectedExpressionInputValues: [['']],
                columnDefs: [
                    {
                        width: '300'

                    },
                    {
                        renderer: deleteRowRenderer,
                        readOnly: true,
                        width: '40'
                    }
                ],
                hotSettings: {
                    stretchH: 'none'
                }
            };
        }

        $scope.save = function () {
            if ($scope.model.newExpression) {
                var newInputExpression = {
                    variableId: $scope.popup.selectedExpressionVariableId,
                    type: 'input',
                    label: $scope.popup.selectedExpressionLabel,
                    entries: getEntriesValues($scope.popup.selectedExpressionInputValues)
                };

                $scope.addNewInputExpression(newInputExpression, $scope.model.selectedColumn + 1);
            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionVariableId;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
                $scope.model.selectedExpression.entries = getEntriesValues($scope.popup.selectedExpressionInputValues);
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            }

            $scope.close();
        };

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
        var getEntriesValues = function (entriesArrayOfArrays) {
            var newEntriesArray = [];
            // remove last value
            entriesArrayOfArrays.pop();

            entriesArrayOfArrays.forEach(function (entriesArray) {
                newEntriesArray.push(entriesArray[0]);
            });

            return newEntriesArray;
        };

        var createEntriesValues = function (entriesArray) {
            var entriesArrayOfArrays = [];
            while (entriesArray.length) entriesArrayOfArrays.push(entriesArray.splice(0, 1));
            return entriesArrayOfArrays;
        };

        var deleteRowRenderer = function (instance, td, row) {
            td.innerHTML = '';
            td.className = 'remove_container';

            if ((row + 1) != $scope.popup.selectedExpressionOutputValues.length) {
                var div = document.createElement('div');
                div.onclick = function () {
                    return instance.alter("remove_row", row);
                };
                div.className = 'btn';
                div.appendChild(document.createTextNode('x'));
                td.appendChild(div);
            }
            return td;
        };

        if ($scope.model.newExpression === false) {
            $scope.popup = {
                selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
                selectedExpressionNewVariableId: $scope.model.selectedExpression.variableId,
                selectedExpressionNewVariableType: $scope.model.selectedExpression.variableType ? $scope.model.selectedExpression.variableType : $scope.availableVariableTypes[0],
                selectedExpressionOutputValues: $scope.model.selectedExpression.entries && $scope.model.selectedExpression.entries.length > 0 ? createEntriesValues($scope.model.selectedExpression.entries) : [['']],
                currentHitPolicy: $scope.model.hitPolicy,
                columnDefs: [
                    {
                        width: '250'
                    },
                    {
                        width: '40',
                        readOnly: true,
                        renderer: deleteRowRenderer
                    }
                ],
                hotSettings: {
                    currentColClassName: 'currentCol',
                    stretchH: 'none'
                }
            };
        } else {
            $scope.popup = {
                selectedExpressionLabel: '',
                selectedExpressionNewVariableId: '',
                selectedExpressionNewVariableType: $scope.availableVariableTypes[0],
                selectedExpressionOutputValues: [['']],
                currentHitPolicy: $scope.model.hitPolicy,
                columnDefs: [
                    {
                        width: '250'
                    },
                    {
                        width: '40',
                        readOnly: true,
                        renderer: deleteRowRenderer
                    }
                ],
                hotSettings: {
                    stretchH: 'none'
                }
            };
        }

        // Cancel button handler
        $scope.cancel = function () {
            $scope.close();
        };

        // Saving the edited input
        $scope.save = function () {
            if ($scope.model.newExpression) {
                var newOutputExpression = {
                    variableId: $scope.popup.selectedExpressionNewVariableId,
                    variableType: $scope.popup.selectedExpressionNewVariableType,
                    type: 'output',
                    label: $scope.popup.selectedExpressionLabel,
                    entries: getEntriesValues($scope.popup.selectedExpressionOutputValues)
                };

                $scope.addNewOutputExpression(newOutputExpression, $scope.model.selectedColumn + 1);
            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionNewVariableId;
                $scope.model.selectedExpression.variableType = $scope.popup.selectedExpressionNewVariableType;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
                $scope.model.selectedExpression.entries = getEntriesValues($scope.popup.selectedExpressionOutputValues);
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            }

            $scope.close();
        };

        $scope.close = function () {
            $scope.$hide();
        };

    }]);

angular.module('flowableModeler')
    .controller('DecisionTableHitPolicyEditorCtrl', ['$rootScope', '$scope', '$q', '$translate', function ($rootScope, $scope, $q, $translate) {

        $scope.popup = {
            currentHitPolicy: $scope.model.hitPolicy,
            availableHitPolicies: $scope.hitPolicies
        };

        // Cancel button handler
        $scope.cancel = function () {
            $scope.close();
        };

        // Saving the edited input
        $scope.save = function () {

            $scope.currentDecisionTable.hitIndicator = $scope.popup.currentHitPolicy;
            $scope.evaluateDecisionHeaders($scope.currentDecisionTable);

            $scope.close();
        };

        $scope.close = function () {
            $scope.$hide();
        };

    }]);