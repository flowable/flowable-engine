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
    .controller('DecisionTableEditorController', ['$rootScope', '$scope', '$q', '$translate', '$http', '$timeout', '$location', '$modal', '$route', '$routeParams', 'DecisionTableService',
        'UtilityService', 'uiGridConstants', 'appResourceRoot', 'hotRegisterer',
        function ($rootScope, $scope, $q, $translate, $http, $timeout, $location, $modal, $route, $routeParams, DecisionTableService,
                  UtilityService, uiGridConstants, appResourceRoot, hotRegisterer) {

            extScope = $scope;

            $rootScope.decisionTableChanges = false;

            var hotDecisionTableEditorInstance;
            var hitPolicies = ['FIRST', 'ANY', 'UNIQUE', 'PRIORITY', 'RULE ORDER', 'OUTPUT ORDER', 'COLLECT'];
            var stringOperators = ['==', '!=', 'IS IN', 'IS NOT IN'];
            var numberOperators = ['==', '!=', '<', '>', '>=', '<=', 'IS IN', 'IS NOT IN'];
            var booleanOperators = ['==', '!='];
            var dateOperators = ['==', '!=', '<', '>', '>=', '<=', 'IS IN', 'IS NOT IN'];
            var collectionOperators = ['ANY OF', 'NONE OF', 'ALL OF', 'NOT ALL OF', '==', '!='];
            var allOperators = ['==', '!=', '<', '>', '>=', '<=', 'ANY OF', 'NONE OF', 'ALL OF', 'NOT ALL OF', 'IS IN', 'IS NOT IN'];
            var collectOperators = {
                'SUM': '+',
                'MIN': '<',
                'MAX': '>',
                'COUNT': '#'
            };

            var columnIdCounter = 0;
            var hitPolicyHeaderElement;
            var dateFormat = 'YYYY-MM-DD';

            // Export name to grid's scope
            $scope.appResourceRoot = appResourceRoot;

            // Model init
            $scope.status = {loading: true};
            $scope.model = {
                rulesData: [],
                columnDefs: [],
                columnVariableIdMap: {},
                startOutputExpression: 0,
                selectedRow: undefined,
                availableInputVariableTypes: ['string', 'number', 'boolean', 'date', 'collection'],
                availableOutputVariableTypes: ['string', 'number', 'boolean', 'date']
            };

            // Hot Model init
            $scope.model.hotSettings = {
                contextMenu: {
                    items: {
                        "insert_row_above": {
                            name: 'Insert rule above',
                            callback: function (key, options) {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    $scope.addRule(hotDecisionTableEditorInstance.getSelected()[0]);
                                }
                            }
                        },
                        "insert_row_below": {
                            name: 'Add rule below',
                            callback: function (key, options) {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    $scope.addRule(hotDecisionTableEditorInstance.getSelected()[0] + 1);
                                }
                            }
                        },
                        "clear_row": {
                            name: 'Clear rule',
                            callback: function (key, options) {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    $scope.clearRule(hotDecisionTableEditorInstance.getSelected()[0]);
                                }
                            }
                        },
                        "remove_row": {
                            name: 'Remove this rule',
                            disabled: function () {
                                // if only 1 rule disable
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    return $scope.model.rulesData.length < 2;
                                } else {
                                    return false;
                                }
                            }
                        },
                        "hsep1": "---------",
                        "move_rule_up": {
                            name: 'Move rule up',
                            disabled: function () {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    return hotDecisionTableEditorInstance.getSelected()[0] === 0;
                                }
                            },
                            callback: function (key, options) {
                                $scope.moveRuleUpwards();
                            }
                        },
                        "move_rule_down": {
                            name: 'Move rule down',
                            disabled: function () {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    return hotDecisionTableEditorInstance.getSelected()[0] + 1 === $scope.model.rulesData.length;
                                }
                            },
                            callback: function (key, options) {
                                $scope.moveRuleDownwards();
                            }
                        },
                        "hsep2": "---------",
                        "add_input": {
                            name: 'Add input',
                            callback: function (key, options) {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    $scope.openInputExpressionEditor(hotDecisionTableEditorInstance.getSelected()[1], true);
                                }
                            }
                        },
                        "add_output": {
                            name: 'Add output',
                            callback: function (key, options) {
                                if (hotDecisionTableEditorInstance.getSelected()) {
                                    if (hotDecisionTableEditorInstance.getSelected()[1] < $scope.model.startOutputExpression) {
                                        $scope.openOutputExpressionEditor($scope.currentDecisionTable.outputExpressions.length, true);
                                    } else {
                                        $scope.openOutputExpressionEditor((hotDecisionTableEditorInstance.getSelected()[1] - $scope.model.startOutputExpression), true);
                                    }
                                }
                            }
                        },
                        "hsep3": "---------",
                        "undo": {
                            name: 'Undo'
                        },
                        "redo": {
                            name: 'Redo'
                        }
                    }
                },
                manualColumnResize: false,
                stretchH: 'all',
                outsideClickDeselects: false,
                viewportColumnRenderingOffset: $scope.model.columnDefs.length + 10
            };


            $scope.hitPolicies = [];
            hitPolicies.forEach(function (id) {
                $scope.hitPolicies.push({
                    id: id,
                    label: 'DECISION-TABLE.HIT-POLICIES.' + id
                });
            });

            $scope.collectOperators = [];
            Object.keys(collectOperators).forEach(function (key) {
                $scope.collectOperators.push({
                    id: key,
                    label: 'DECISION-TABLE.COLLECT-OPERATORS.' + key
                });
            });

            $scope.addRule = function (rowNumber) {
                if (rowNumber !== undefined) {
                    $scope.model.rulesData.splice(rowNumber, 0, createDefaultRow());
                } else {
                    $scope.model.rulesData.push(createDefaultRow());
                }
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.render();
                }
            };

            $scope.removeRule = function () {
                $scope.model.rulesData.splice($scope.model.selectedRow, 1);
            };

            $scope.clearRule = function (rowNumber) {
                if (rowNumber === undefined) {
                    return;
                }
                $scope.model.rulesData[rowNumber] = createDefaultRow();
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.render();
                }
            };

            $scope.moveRuleUpwards = function () {
                $scope.model.rulesData.splice($scope.model.selectedRow - 1, 0, $scope.model.rulesData.splice($scope.model.selectedRow, 1)[0]);
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.render();
                }
            };

            $scope.moveRuleDownwards = function () {
                $scope.model.rulesData.splice($scope.model.selectedRow + 1, 0, $scope.model.rulesData.splice($scope.model.selectedRow, 1)[0]);
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.render();
                }
            };

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
                if (coords.row === 0 && coords.col === 0 && TD.className === '') {
                    $scope.openHitPolicyEditor();
                } else {
                    if (coords && coords.row !== undefined) {
                        $timeout(function () {
                            $scope.model.selectedRow = coords.row;
                        });
                    }
                }
            };

            $scope.doAfterScroll = function () {
                if (hotDecisionTableEditorInstance) {
                    hotDecisionTableEditorInstance.render();
                }
            };

            $scope.doAfterRender = function (isForced) {

                if (hitPolicyHeaderElement) {
                    var element = document.querySelector("thead > tr > th:first-of-type");
                    if (element) {
                        var firstChild = element.firstChild;
                        element.className = 'hit-policy-container';
                        element.replaceChild(hitPolicyHeaderElement[0], firstChild);
                    }
                }

                if (hotDecisionTableEditorInstance === undefined) {
                    $timeout(function () {
                        hotDecisionTableEditorInstance = hotRegisterer.getInstance('decision-table-editor');
                        hotDecisionTableEditorInstance.validateCells();
                    });
                } else {
                    hotDecisionTableEditorInstance.validateCells();
                }
            };

            $scope.dumpData = function () {
                console.log($scope.currentDecisionTable);
                console.log($scope.model.rulesData);
                console.log($scope.model.columnDefs);
            };

            $scope.doAfterValidate = function (isValid, value, row, prop, source) {
                if (isCorrespondingCollectionOperator(row, prop)) {
                    return true;
                } else if (isCustomExpression(value) || isDashValue(value)) {
                    disableCorrespondingOperatorCell(row, prop);
                    return true;
                } else {
                    enableCorrespondingOperatorCell(row, prop);
                }
            };

            // dummy validator for text fields in order to trigger the post validation hook
            var textValidator = function(value, callback) {
                callback(true);
            };

            var isCustomExpression = function (val) {
                return !!(val != null
                    && (String(val).startsWith('${') || String(val).startsWith('#{')));
            };

            var isDashValue = function (val) {
                return !!(val != null && "-" === val);
            };

            var isCorrespondingCollectionOperator = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                var isCollectionOperator = false;
                if (isOperatorCell(operatorCellMeta)) {
                    var operatorValue = hotDecisionTableEditorInstance.getDataAtCell(row, operatorCol);
                    if (operatorValue === "IN" || operatorValue === "NOT IN" || operatorValue === "ANY" || operatorValue === "NOT ANY") {
                        isCollectionOperator = true;
                    }
                }
                return isCollectionOperator;
            };

            var disableCorrespondingOperatorCell = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                if (!isOperatorCell(operatorCellMeta)) {
                    return;
                }

                if (operatorCellMeta.className != null && operatorCellMeta.className.indexOf('custom-expression-operator') !== -1) {
                    return;
                }

                var currentEditor = hotDecisionTableEditorInstance.getCellEditor(row, operatorCol);

                hotDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'className', operatorCellMeta.className + ' custom-expression-operator');
                hotDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'originalEditor', currentEditor);
                hotDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'editor', false);
                hotDecisionTableEditorInstance.setDataAtCell(row, operatorCol, null);
            };

            var enableCorrespondingOperatorCell = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                if (!isOperatorCell(operatorCellMeta)) {
                    return;
                }

                if (operatorCellMeta == null || operatorCellMeta.className == null || operatorCellMeta.className.indexOf('custom-expression-operator') == -1) {
                    return;
                }

                operatorCellMeta.className = operatorCellMeta.className.replace('custom-expression-operator', '');
                hotDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'className', operatorCellMeta.className);
                hotDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'editor', operatorCellMeta.originalEditor);
                hotDecisionTableEditorInstance.setDataAtCell(row, operatorCol, '==');
            };

            var getCorrespondingOperatorCell = function (row, prop) {
                var currentCol = hotDecisionTableEditorInstance.propToCol(prop);
                if (currentCol < 1) {
                    return;
                }
                var operatorCol = currentCol - 1;
                return operatorCol;
            };

            var isOperatorCell = function (cellMeta) {
                return !(cellMeta == null || cellMeta.prop == null || typeof cellMeta.prop !== 'string'|| cellMeta.prop.endsWith("_operator") === false);
            };

            var createNewInputExpression = function (inputExpression) {
                var newInputExpression;
                if (inputExpression) {
                    newInputExpression = {
                        id: _generateColumnId(),
                        label: inputExpression.label,
                        variableId: inputExpression.variableId,
                        type: inputExpression.type,
                        newVariable: inputExpression.newVariable,
                        entries: inputExpression.entries
                    };

                } else {
                    newInputExpression = {
                        id: _generateColumnId(),
                        label: null,
                        variableId: null,
                        type: null,
                        newVariable: null,
                        entries: null
                    };
                }
                return newInputExpression;
            };

            var createNewOutputExpression = function (outputExpression) {
                var newOutputExpression;
                if (outputExpression) {
                    newOutputExpression = {
                        id: _generateColumnId(),
                        label: outputExpression.label,
                        variableId: outputExpression.variableId,
                        type: outputExpression.type,
                        newVariable: outputExpression.newVariable,
                        entries: outputExpression.entries
                    };

                } else {
                    newOutputExpression = {
                        id: _generateColumnId(),
                        label: null,
                        variableId: null,
                        type: null,
                        newVariable: null,
                        entries: null
                    };
                }
                return newOutputExpression;
            };

            $scope.addNewInputExpression = function (inputExpression, insertPos) {
                var newInputExpression = createNewInputExpression(inputExpression);

                // insert expression at position or just add
                if (insertPos !== undefined && insertPos !== -1) {
                    $scope.currentDecisionTable.inputExpressions.splice(insertPos, 0, newInputExpression);
                } else {
                    $scope.currentDecisionTable.inputExpressions.push(newInputExpression);
                }

                // update data model with initial values
                $scope.model.rulesData.forEach(function (rowData) {
                    rowData[newInputExpression.id + '_expression'] = '-';
                });

                // update column definitions off the source model
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.addNewOutputExpression = function (outputExpression, insertPos) {
                var newOutputExpression = createNewOutputExpression(outputExpression);

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
                var removedElements = $scope.currentDecisionTable.inputExpressions.splice(expressionPos, 1);
                removePropertyFromGrid(removedElements[0].id, 'input');
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            var removePropertyFromGrid = function (key, type) {
                if ($scope.model.rulesData) {
                    $scope.model.rulesData.forEach(function (rowData) {
                        if (type === 'input') {
                            delete rowData[key + '_operator'];
                            delete rowData[key + '_expression'];
                        } else if (type === 'output') {
                            delete rowData[key];
                        }
                    });
                }
            };

            $scope.removeOutputExpression = function (expressionPos) {
                var removedElements = $scope.currentDecisionTable.outputExpressions.splice(expressionPos, 1);
                removePropertyFromGrid(removedElements[0].id, 'output');
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.openInputExpressionEditor = function (expressionPos, newExpression) {
                var editTemplate = 'views/popup/decision-table-edit-input-expression.html';

                $scope.model.newExpression = !!newExpression;

                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.currentDecisionTable.inputExpressions[expressionPos];
                } else {
                    if (expressionPos >= $scope.model.startOutputExpression) {
                        $scope.model.selectedColumn = $scope.model.startOutputExpression - 1;
                    } else {
                        $scope.model.selectedColumn = Math.floor(expressionPos / 2);
                    }
                }

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openOutputExpressionEditor = function (expressionPos, newExpression) {
                var editTemplate = 'views/popup/decision-table-edit-output-expression.html';

                $scope.model.newExpression = !!newExpression;
                $scope.model.hitPolicy = $scope.currentDecisionTable.hitIndicator;
                $scope.model.selectedColumn = expressionPos;


                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.currentDecisionTable.outputExpressions[expressionPos];
                }

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openHitPolicyEditor = function () {
                var editTemplate = 'views/popup/decision-table-edit-hit-policy.html';

                $scope.model.hitPolicy = $scope.currentDecisionTable.hitIndicator;
                $scope.model.collectOperator = $scope.currentDecisionTable.collectOperator;

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

                    $scope.model.lastUpdatedBy = decisionTable.lastUpdatedBy;
                    $scope.model.createdBy = decisionTable.createdBy;

                    // decision table model to used in save dialog
                    $rootScope.currentDecisionTableModel = {
                        id: decisionTable.id,
                        name: decisionTable.name,
                        key: decisionTable.key,
                        description: decisionTable.description
                    };

                    if (!$rootScope.currentDecisionTable.hitIndicator) {
                        $rootScope.currentDecisionTable.hitIndicator = hitPolicies[0];
                    }

                    var hitPolicyHeaderString = '<div class="hit-policy-header"><a onclick="triggerHitPolicyEditor()">' + $rootScope.currentDecisionTable.hitIndicator.substring(0, 1);
                    if ($rootScope.currentDecisionTable.collectOperator) {
                        hitPolicyHeaderString += collectOperators[$rootScope.currentDecisionTable.collectOperator];
                    }
                    hitPolicyHeaderString += '</a></div>';
                    hitPolicyHeaderElement = angular.element(hitPolicyHeaderString);

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
                    source: getOperatorsForColumnType(inputExpression.type)
                };

                if ($scope.currentDecisionTable.inputExpressions.length !== 1) {
                    columnDefinition.title = '<div class="header-remove-expression">' +
                        '<a onclick="triggerRemoveExpression(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-minus-sign"></span></a>' +
                        '</div>';
                }

                return columnDefinition;
            };

            var getOperatorsForColumnType = function (type) {
                switch (type) {
                    case 'number':
                        return numberOperators;
                    case 'date':
                        return dateOperators;
                    case 'boolean':
                        return booleanOperators;
                    case 'string':
                        return stringOperators;
                    case 'collection':
                        return collectionOperators;
                    default:
                        return allOperators;
                }
            };

            var composeInputExpressionColumnDefinition = function (inputExpression) {
                var expressionPosition = $scope.currentDecisionTable.inputExpressions.indexOf(inputExpression);

                var type;
                switch (inputExpression.type) {
                    case 'date':
                        type = 'date';
                        break;
                    case 'number':
                        type = 'numeric';
                        break;
                    case 'boolean':
                        type = 'dropdown';
                        break;
                    default:
                        type = 'text';
                }

                var columnDefinition = {
                    data: inputExpression.id + '_expression',
                    type: type,
                    title: '<div class="input-header">' +
                    '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',false)"><span class="header-label">' + (inputExpression.label ? inputExpression.label : "New Input") + '</span></a>' +
                    '<br><span class="header-variable">' + (inputExpression.variableId ? inputExpression.variableId : "none") + '</span>' +
                    '<br/><span class="header-variable-type">' + (inputExpression.type ? inputExpression.type : "") + '</brspan>' +
                    '</div>' +
                    '<div class="header-add-new-expression">' +
                    '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                    '</div>',
                    expressionType: 'input-expression',
                    expression: inputExpression,
                    className: 'htCenter',
                    width: '200'
                };

                if (inputExpression.entries && inputExpression.entries.length > 0) {
                    var entriesOptionValues = inputExpression.entries.slice(0, inputExpression.entries.length);
                    entriesOptionValues.push('-');

                    columnDefinition.type = 'dropdown';
                    columnDefinition.strict = true;
                    columnDefinition.source = entriesOptionValues;

                    columnDefinition.title = '<div class="input-header">' +
                        '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',false)"><span class="header-label">' + (inputExpression.label ? inputExpression.label : "New Input") + '</span></a>' +
                        '<br><span class="header-variable">' + (inputExpression.variableId ? inputExpression.variableId : "none") + '</span>' +
                        '<br/><span class="header-variable-type">' + (inputExpression.type ? inputExpression.type : "") + '</span>' +
                        '<br><span class="header-entries">[' + inputExpression.entries.join() + ']</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';
                } else if (type === 'text') {
                    columnDefinition.validator = textValidator;
                }

                if (type === 'date') {
                    columnDefinition.dateFormat = dateFormat;
                    columnDefinition.validator = function (value, callback) {
                        if (value === '-') {
                            callback(true);
                        } else {
                            Handsontable.DateValidator.call(this, value, callback);
                        }
                    }

                } else if (type === 'dropdown') {
                    columnDefinition.source = ['true', 'false', '-'];
                }

                if (type !== 'text') {
                    columnDefinition.allowEmpty = false;
                }

                return columnDefinition;
            };

            var composeOutputColumnDefinition = function (outputExpression) {
                var expressionPosition = $scope.currentDecisionTable.outputExpressions.indexOf(outputExpression);

                var type;
                switch (outputExpression.type) {
                    case 'date':
                        type = 'date';
                        break;
                    case 'number':
                        type = 'numeric';
                        break;
                    case 'boolean':
                        type = 'dropdown';
                        break;
                    default:
                        type = 'text';
                }

                var title = '';
                var columnDefinition = {
                    data: outputExpression.id,
                    type: type,
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
                    columnDefinition.type = 'dropdown';
                    columnDefinition.source = entriesOptionValues;
                    columnDefinition.strict = true;

                    title += '<div class="output-header">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',false)"><span class="header-label">' + (outputExpression.label ? outputExpression.label : "New Output") + '</span></a>' +
                        '<br><span class="header-variable">' + (outputExpression.variableId ? outputExpression.variableId : "none") + '</span>' +
                        '<br/><span class="header-variable-type">' + (outputExpression.type ? outputExpression.type : "") + '</span>' +
                        '<br><span class="header-entries">[' + outputExpression.entries.join() + ']</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';

                } else {
                    title += '<div class="output-header">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',false)"><span class="header-label">' + (outputExpression.label ? outputExpression.label : "New Output") + '</span></a>' +
                        '<br><span class="header-variable">' + (outputExpression.variableId ? outputExpression.variableId : "none") + '</span>' +
                        '<br/><span class="header-variable-type">' + (outputExpression.type ? outputExpression.type : "") + '</span>' +
                        '</div>' +
                        '<div class="header-add-new-expression">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',true)"><span class="glyphicon glyphicon-plus-sign"></span></a>' +
                        '</div>';
                }

                if (type === 'date') {
                    columnDefinition.dateFormat = dateFormat;
                } else if (type === 'dropdown') {
                    columnDefinition.source = ['true', 'false', '-'];
                }

                columnDefinition.title = title;

                return columnDefinition;
            };

            $scope.evaluateDecisionHeaders = function (decisionTable) {

                var element = document.querySelector("thead > tr > th:first-of-type > div > a");
                if (element) {
                    if (decisionTable.collectOperator) {
                        element.innerHTML = decisionTable.hitIndicator.substring(0, 1) + collectOperators[decisionTable.collectOperator];
                    } else {
                        element.innerHTML = decisionTable.hitIndicator.substring(0, 1);
                    }
                }

                var columnDefinitions = [];
                var inputExpressionCounter = 0;
                if (decisionTable.inputExpressions && decisionTable.inputExpressions.length > 0) {
                    decisionTable.inputExpressions.forEach(function (inputExpression) {
                        var inputOperatorColumnDefinition = composeInputOperatorColumnDefinition(inputExpression);
                        columnDefinitions.push(inputOperatorColumnDefinition);
                        setGridValues(inputOperatorColumnDefinition.data, inputOperatorColumnDefinition.expressionType);

                        var inputExpressionColumnDefinition = composeInputExpressionColumnDefinition(inputExpression);
                        columnDefinitions.push(inputExpressionColumnDefinition);
                        setGridValues(inputExpressionColumnDefinition.data, inputExpressionColumnDefinition.expressionType);

                        inputExpressionCounter += 2;
                    });

                } else { // create default input expression
                    decisionTable.inputExpressions = [];
                    var inputExpression = createNewInputExpression();
                    decisionTable.inputExpressions.push(inputExpression);
                    columnDefinitions.push(composeInputOperatorColumnDefinition(inputExpression));
                    columnDefinitions.push(composeInputExpressionColumnDefinition(inputExpression));
                    inputExpressionCounter += 2;
                }

                columnDefinitions[inputExpressionCounter - 1].className += ' last';
                $scope.model.startOutputExpression = inputExpressionCounter;

                if (decisionTable.outputExpressions && decisionTable.outputExpressions.length > 0) {
                    decisionTable.outputExpressions.forEach(function (outputExpression) {
                        columnDefinitions.push(composeOutputColumnDefinition(outputExpression));
                    });

                } else { // create default output expression
                    decisionTable.outputExpressions = [];
                    var outputExpression = createNewOutputExpression();
                    decisionTable.outputExpressions.push(outputExpression);
                    columnDefinitions.push(composeOutputColumnDefinition(outputExpression));
                }

                columnDefinitions[inputExpressionCounter].className += ' first';

                // timeout needed for trigger hot update when removing column defs
                $scope.model.columnDefs = columnDefinitions;
                $timeout(function () {
                    if (hotDecisionTableEditorInstance) {
                        hotDecisionTableEditorInstance.render();
                    }
                });
            };

            var setGridValues = function (key, type) {
                if ($scope.model.rulesData) {
                    $scope.model.rulesData.forEach(function (rowData) {
                        if (type === 'input-operator') {
                            if (!(key in rowData) || rowData[key] === '') {
                                rowData[key] = '==';
                            }
                        }
                    });
                }
            };

            var createDefaultRow = function () {
                var defaultRow = {};
                $scope.model.columnDefs.forEach(function (columnDefinition) {
                    if (columnDefinition.expressionType === 'input-operator') {
                        defaultRow[columnDefinition.data] = '==';
                    }
                    else if (columnDefinition.expressionType === 'input-expression') {
                        defaultRow[columnDefinition.data] = '-';
                    }
                    else if (columnDefinition.expressionType === 'output') {
                        defaultRow[columnDefinition.data] = '';
                    }
                });

                return defaultRow;
            };

            var evaluateDecisionGrid = function (decisionTable) {
                var tmpRuleGrid = [];

                // rows
                if (decisionTable.rules && decisionTable.rules.length > 0) {
                    decisionTable.rules.forEach(function (rule) {

                        // rule data
                        var tmpRowValues = {};
                        for (var i = 0; i < Object.keys(rule).length; i++) {
                            var id = Object.keys(rule)[i];

                            $scope.model.columnDefs.forEach(function (columnDef) {
                                // set counter to max value
                                var expressionId = 0;
                                try {
                                    expressionId = parseInt(columnDef.expression.id);
                                } catch (e) {
                                }
                                if (expressionId > columnIdCounter) {
                                    columnIdCounter = expressionId;
                                }
                            });

                            // collection operators backwards compatibility
                            // var cellValue = regressionTransformation(id, rule[id]);

                            tmpRowValues[id] = rule[id];
                        }

                        tmpRuleGrid.push(tmpRowValues);
                    });
                } else {
                    // initialize default values
                    tmpRuleGrid.push(createDefaultRow());
                }
                // $rootScope.currentDecisionTableRules = tmpRuleGrid;
                $scope.model.rulesData = tmpRuleGrid;
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
    .controller('DecisionTableInputConditionEditorCtlr', ['$rootScope', '$scope', 'hotRegisterer', '$timeout', function ($rootScope, $scope, hotRegisterer, $timeout) {

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
            var localCopy = entriesArray.slice(0);
            var entriesArrayOfArrays = [];
            while (localCopy.length) {
                entriesArrayOfArrays.push(localCopy.splice(0, 1));
            }

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
                selectedExpressionVariableType: $scope.model.selectedExpression.type ? $scope.model.selectedExpression.type : $scope.model.availableInputVariableTypes[0],
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
                selectedExpressionVariableType: $scope.model.availableInputVariableTypes[0],
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
                    type: $scope.popup.selectedExpressionVariableType,
                    label: $scope.popup.selectedExpressionLabel,
                    entries: $scope.popup.selectedExpressionVariableType !== 'collection' ? getEntriesValues($scope.popup.selectedExpressionInputValues) : []
                };

                $scope.addNewInputExpression(newInputExpression, $scope.model.selectedColumn + 1);

            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionVariableId;
                $scope.model.selectedExpression.type = $scope.popup.selectedExpressionVariableType;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
                $scope.model.selectedExpression.entries = $scope.popup.selectedExpressionVariableType !== 'collection' ? getEntriesValues($scope.popup.selectedExpressionInputValues) : [];
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
    .controller('DecisionTableConclusionEditorCtrl', ['$rootScope', '$scope', '$q', '$translate', 'hotRegisterer', function ($rootScope, $scope, $q, $translate, hotRegisterer) {

        var hotInstance;
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
            var localCopy = entriesArray.slice(0);
            var entriesArrayOfArrays = [];
            while (localCopy.length) {
                entriesArrayOfArrays.push(localCopy.splice(0, 1));
            }

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

        $scope.doAfterRender = function () {
            hotInstance = hotRegisterer.getInstance('decision-table-allowed-values');
        };

        if ($scope.model.newExpression === false) {
            $scope.popup = {
                selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
                selectedExpressionNewVariableId: $scope.model.selectedExpression.variableId,
                selectedExpressionNewVariableType: $scope.model.selectedExpression.type ? $scope.model.selectedExpression.type : $scope.model.availableOutputVariableTypes[0],
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
                selectedExpressionNewVariableType: $scope.model.availableOutputVariableTypes[0],
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
                    type: $scope.popup.selectedExpressionNewVariableType,
                    label: $scope.popup.selectedExpressionLabel,
                    entries: getEntriesValues(hotInstance.getData())
                };
                $scope.addNewOutputExpression(newOutputExpression, $scope.model.selectedColumn + 1);

            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionNewVariableId;
                $scope.model.selectedExpression.type = $scope.popup.selectedExpressionNewVariableType;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
                $scope.model.selectedExpression.entries = getEntriesValues(hotInstance.getData());
                $scope.evaluateDecisionHeaders($scope.currentDecisionTable);
            }

            $scope.close();
        };

        $scope.close = function () {

            $scope.$hide();
        };

        $scope.dumpData = function () {
            console.log(hotInstance.getData());
        }

    }]);

angular.module('flowableModeler')
    .controller('DecisionTableHitPolicyEditorCtrl', ['$rootScope', '$scope', '$q', '$translate', function ($rootScope, $scope, $q, $translate) {

        $scope.popup = {
            currentHitPolicy: $scope.model.hitPolicy,
            currentCollectOperator: $scope.model.collectOperator,
            availableHitPolicies: $scope.hitPolicies,
            availableCollectOperators: $scope.collectOperators
        };

        // Cancel button handler
        $scope.cancel = function () {
            $scope.close();
        };

        // Saving the edited input
        $scope.save = function () {
            $scope.currentDecisionTable.hitIndicator = $scope.popup.currentHitPolicy;
            if ($scope.popup.currentHitPolicy === 'COLLECT') {
                $scope.currentDecisionTable.collectOperator = $scope.popup.currentCollectOperator;
            } else {
                $scope.currentDecisionTable.collectOperator = undefined;
            }

            $scope.evaluateDecisionHeaders($scope.currentDecisionTable);

            $scope.close();
        };

        $scope.close = function () {
            $scope.$hide();
        };

    }]);
