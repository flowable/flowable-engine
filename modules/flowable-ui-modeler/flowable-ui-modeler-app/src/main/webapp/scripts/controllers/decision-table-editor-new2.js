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

            var MIN_COLUMN_WIDTH = 200;

            // Export name to grid's scope
            $scope.appResourceRoot = appResourceRoot;

            // Model init
            $scope.status = {loading: true};
            $scope.model = {
                columnDefs: [],
                columnVariableIdMap: {},
                startOutputExpression: 0
            };

            $scope.availableVariableTypes = ['string', 'number', 'boolean', 'date'];
            $scope.inputOperators = ['==', '!=', '<', '>'];

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
                                        return ((hotDecisionTableEditorInstance.getSelected()[1] / 2)< $scope.model.startOutputExpression);
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

            var hotDecisionTableEditorInstance;


            $scope.dumpData = function () {
                console.log($scope.rulesData);
            };

            $scope.testHotCall = function () {
                console.log('number of rows 1: ' + hotRegisterer.getInstance('decision-table-editor').countRows());
                console.log('number of rows 2: ' + hotDecisionTableEditorInstance.countRows());
            };

            $rootScope.decisionTableChanges = false;

            var hitPolicies = ['FIRST', 'ANY'];
            $scope.hitPolicies = [];
            hitPolicies.forEach(function (id) {
                $scope.hitPolicies.push({
                    id: id,
                    label: 'DECISION-TABLE.HIT-POLICIES.' + id
                });
            });

            $scope.rulesData;
            $scope.rowHeaders = function (index) {
                if (index == 0) {
                    return 'HP';
                } else {
                    return index;
                }
            };

             var customColHeaders = function(col) {
                var currentColumn = $scope.model.columnDefs[col];
                console.log(col);
            };

            $scope.customRenderer = function (row, col, prop) {
                var cellProperties = {};
                if (row === 0) {
                    cellProperties.renderer = customHeaderRenderer;
                    cellProperties.readOnly = true;
                } else if (col < $scope.model.startOutputExpression) {
                    cellProperties.renderer = customInputEntryRenderer;
                    cellProperties.editor = false;
                }

                return cellProperties;
            };

            var customHeaderRenderer = function (instance, td, row, col, prop, value, cellProperties) {
                Handsontable.renderers.TextRenderer.apply(this, arguments);

                var style = td.style;
                style.textAlign = 'center';
                style.fontStyle = 'normal';

                td.innerHTML = '';

                if (value && value !== undefined) {
                    var containerElement1 = angular.element('<div class="header-center"></div>');

                    if (value.type && value.type === 'input') {
                        td.className = 'inputHeader';
                        var el = $compile('<a class="header-label" href="" ng-click="openInputExpressionEditor(' + col + ')">' + value.label + '</a>')($scope);
                        containerElement1.append(el[0]);

                        var addElement = $compile('<a href="" ng-click="openInputExpressionEditor(' + col + ', true)"><span class="glyphicon glyphicon-plus-sign"></span></a>')($scope);
                    }
                    if (value.type && value.type === 'output') {
                        style.background = '#eee';
                        td.className = 'outputHeader';

                        var el = $compile('<a class="header-label" href="" ng-click="openOutputExpressionEditor(' + col + ')">' + value.label + '</a>')($scope);
                        containerElement1.append(el[0]);

                        var addElement = $compile('<a href="" ng-click="openOutputExpressionEditor(' + col + ', true)"><span class="glyphicon glyphicon-plus-sign"></span></a>')($scope);
                    }

                    var breakElement = angular.element('<br>');
                    containerElement1.append(breakElement[0]);

                    var variableElement = angular.element('<span class="header-variable">' + value.variableId + '</span>');
                    containerElement1.append(variableElement[0]);

                    td.appendChild(containerElement1[0]);

                    var containerElement2 = angular.element('<div class="header-add-new-expression"></div>');
                    containerElement2.append(addElement[0]);

                    td.appendChild(containerElement2[0]);
                }

                return td;
            };
            //
            // $scope.colHeaders = function(col) {
            //     var txt;
            //
            //     switch (col) {
            //         case 0:
            //             return '<b>Bold</b> and <em>Beautiful</em>';
            //
            //         case 1:
            //             var addElement = $compile('<a href="#" ng-click="openOutputExpressionEditor(' + col + ', true)"><span class="glyphicon glyphicon-plus-sign"></span></a>')($scope);
            //             console.log(addElement);
            //             return '<b>Black</b> and <em>Beautiful</em>' + addElement[0].outerHTML;
            //
            //     }
            //
            //     return col;
            // };

            var customInputEntryRenderer = function (instance, td, row, col, prop, value, cellProperties) {
                // Handsontable.renderers.TextRenderer.apply(this, arguments);

                td.innerHTML = '';
                var operatorSelectOptions = ['==', '!=', '<', '>'];

                if (value && value !== undefined) {

                    var parentContainer = document.createElement('div');
                    parentContainer.className = 'input-container';

                    var containerElement1 = document.createElement('div');
                    containerElement1.className = 'input-operator-container';

                    var operatorSelect = document.createElement('select');
                    operatorSelect.className = 'input-operator-select';

                    operatorSelectOptions.forEach(function (val) {
                        var optionElement = document.createElement('option');
                        optionElement.value = val;
                        optionElement.text = val;
                        operatorSelect.appendChild(optionElement);
                    }, this);

                    containerElement1.appendChild(operatorSelect);

                    var containerElement2 = document.createElement('div');
                    containerElement2.className = 'input-expression-container';

                    var expressionInput = $compile('<input type="text" value="' + value.expression + '" class="input-expression-input">')($scope);
                    // var expressionInput = $compile('<input type="text" ng-model="rulesData['+row+']['+col+'].expression" class="input-expression-input">')($scope);
                    //
                    //
                    // var expressionInput = document.createElement('input');
                    // expressionInput.className = 'input-expression-input';
                    // expressionInput.setAttribute('type','text');

                    containerElement2.appendChild(expressionInput[0]);

                    parentContainer.appendChild(containerElement1);
                    parentContainer.appendChild(containerElement2);

                    td.appendChild(parentContainer);
                }
            };

            $scope.addInputExpression = function (inputExpression, insertPos) {
                if (!$scope.currentDecisionTable.inputExpressions) {
                    $scope.currentDecisionTable.inputExpressions = [];
                }

                var newInputExpression;
                if (!inputExpression) {
                    newInputExpression = {id: _generateColumnId()};
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

                // if no rules present add one
                // if ($rootScope.currentDecisionTableRules.length === 0) {
                //     $scope.addRule();
                // }

                // add props to rule data
                // $rootScope.currentDecisionTableRules.forEach(function (rowObject) {
                //     rowObject[newInputExpression.id] = "";
                // });

                // insert expression at position or just add
                if (insertPos !== undefined && insertPos !== -1) {
                    $scope.currentDecisionTable.inputExpressions.splice(insertPos, 0, newInputExpression);
                } else {
                    $scope.currentDecisionTable.inputExpressions.push(newInputExpression);
                }

                $scope.model.columnVariableIdMap[newInputExpression.id] = newInputExpression.variableId;

                // update column definitions off the source model
                evaluateDecisionHeaders($scope.currentDecisionTable);
            };

            $scope.openInputExpressionEditor = function (columnId, newExpression) {

                $scope.model.newExpression = !!newExpression;
                $scope.model.selectedColumn = columnId;

                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.currentDecisionTable.inputExpressions[0];
                }

                var editTemplate = 'views/popup/decision-table-edit-input-expression.html';

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openOutputExpressionEditor = function (columnId, newExpression) {

                $scope.model.newExpression = !!newExpression;
                $scope.model.selectedColumn = columnId;

                if (!$scope.model.newExpression) {
                    $scope.model.selectedExpression = $scope.rulesData[0][columnId];
                }

                var editTemplate = 'views/popup/decision-table-edit-output-expression.html';

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

            var evaluateDecisionHeaders = function (decisionTable) {
                var columnDefinitions = [];
                var inputExpressionCounter = 0;
                if (decisionTable.inputExpressions && decisionTable.inputExpressions.length > 0) {
                    decisionTable.inputExpressions.forEach(function (inputExpression) {
                        inputExpression.type = 'input';
                        var columnDefinitionInputOperator = {
                            data: inputExpression.id + '_operator',
                            expressionType: 'input',
                            expression: inputExpression,
                            width: '20',
                            className: 'htCenter',
                            type: 'dropdown',
                            source: ['==', '!=', '<', '>']
                        };
                        columnDefinitions.push(columnDefinitionInputOperator);


                        var columnDefinitionInputExpression = {
                            data: inputExpression.id + '_expression',
                            title: '<span onclick="triggerInputExpressionEditor('+decisionTable.inputExpressions.indexOf(inputExpression)+', false)" class="input-header">'+inputExpression.label+'</span>',
                            expressionType: 'input',
                            expression: inputExpression,
                            className: 'htCenter'
                        };
                        columnDefinitions.push(columnDefinitionInputExpression);

                        inputExpressionCounter++;
                    });
                }

                $scope.model.startOutputExpression = inputExpressionCounter;

                if (decisionTable.outputExpressions && decisionTable.outputExpressions.length > 0) {
                    decisionTable.outputExpressions.forEach(function (outputExpression) {
                        outputExpression.type = 'output';
                        var columnDefinitionOutput = {
                            data: outputExpression.id,
                            title: '<span class="output-header">'+outputExpression.label+'</span>',
                            expressionType: 'output',
                            expression: outputExpression,
                            className: 'htCenter'
                        };
                        columnDefinitions.push(columnDefinitionOutput);
                    });
                }

                console.log(columnDefinitions);
                $scope.model.columnDefs = columnDefinitions;
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
                            } catch(e){}
                            if (expressionId > columnIdCounter) {
                                columnIdCounter = expressionId;
                            }

                            if (i < $scope.model.startOutputExpression) {
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

                console.log(tmpRuleGrid);
                $scope.rulesData = tmpRuleGrid;
            };

            var evaluateDecisionTableGrid = function (decisionTable) {

                evaluateDecisionHeaders(decisionTable);
                evaluateDecisionGrid(decisionTable);

            };


            // fetch table from service and populate model
            _loadDecisionTableDefinition($routeParams.modelId);

            var _generateColumnId = function () {
                columnIdCounter++;
                return "" + columnIdCounter;
            };

            $timeout(function () {
                // Flip switch in timeout to start watching all decision-related models
                // after next digest cycle, to prevent first false-positive
                hotDecisionTableEditorInstance = hotRegisterer.getInstance('decision-table-editor');
            });

        }]);

angular.module('flowableModeler')
    .controller('DecisionTableInputConditionEditorCtlr', ['$rootScope', '$scope', function ($rootScope, $scope) {
        // condition input options
        if ($scope.model.newExpression === false) {
            $scope.popup = {
                selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
                selectedExpressionVariable: {id: $scope.model.selectedExpression.variableId}
            };
        } else {
            $scope.popup = {
                selectedExpressionLabel: '',
                selectedExpressionVariable: {id: null}
            };
        }

        $scope.save = function () {
            // if (previousVariableId !== $scope.popup.selectedExpressionVariable.id) {
            //
            //     var newInputExpression = {
            //         label: $scope.popup.selectedExpressionLabel,
            //         variableId: $scope.popup.selectedExpressionVariable.id,
            //         newVariable: $scope.popup.selectedExpressionNewVariable
            //     };
            //
            //     // $scope.updateInputExpression($scope.model.selectedColumn, newInputExpression);
            //
            //     // if ($scope.popup.selectedExpressionNewVariable) {
            //     //     saveNewDefinedVariable();
            //     // }
            //
            // } else {
            //     $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
            // }

            if ($scope.model.newExpression) {
                var newInputExpression = {
                    variableId: $scope.popup.selectedExpressionVariable.id,
                    type: 'input',
                    label: $scope.popup.selectedExpressionLabel
                };

                for (var i = 0; i < $scope.rulesData.length; i++) {
                    if (i === 0) {
                        $scope.rulesData[i].splice($scope.model.selectedColumn + 1, 0, newInputExpression);
                    } else {
                        $scope.rulesData[i].splice($scope.model.selectedColumn + 1, 0, null);
                    }
                }

                $scope.model.startOutputExpression++;
            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionVariable.id;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
            }

            $scope.close();
        };

        // function saveNewDefinedVariable() {
        //     var newVariable = {
        //         processVariableName: $scope.popup.selectedExpressionVariable.id,
        //         processVariableType: $scope.model.selectedExpression.type
        //     };
        //
        //     if ($scope.currentDecisionTable.executionVariables.indexOf(newVariable)) {
        //         $scope.currentDecisionTable.executionVariables.push(newVariable);
        //     }
        // }

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

        if ($scope.model.newExpression === false) {
            $scope.popup = {
                selectedExpressionLabel: $scope.model.selectedExpression.label ? $scope.model.selectedExpression.label : '',
                selectedExpressionNewVariableId: $scope.model.selectedExpression.variableId,
                selectedExpressionNewVariableType: $scope.model.selectedExpression.variableType ? $scope.model.selectedExpression.variableType : $scope.availableVariableTypes[0]
            };
        } else {
            $scope.popup = {
                selectedExpressionLabel: '',
                selectedExpressionNewVariableId: '',
                selectedExpressionNewVariableType: $scope.availableVariableTypes[0]
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
                    label: $scope.popup.selectedExpressionLabel
                };

                for (var i = 0; i < $scope.rulesData.length; i++) {
                    if (i === 0) {
                        $scope.rulesData[i].splice($scope.model.selectedColumn + 1, 0, newOutputExpression);
                    } else {
                        $scope.rulesData[i].splice($scope.model.selectedColumn + 1, 0, null);
                    }
                }

            } else {
                $scope.model.selectedExpression.variableId = $scope.popup.selectedExpressionNewVariableId;
                $scope.model.selectedExpression.variableType = $scope.popup.selectedExpressionNewVariableType;
                $scope.model.selectedExpression.label = $scope.popup.selectedExpressionLabel;
            }

            $scope.close();
        };


        $scope.close = function () {
            $scope.$hide();
        };

    }]);
