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

var extScope;

angular.module('flowableModeler')
    .controller('DecisionTableDetailsCtrl', ['$rootScope', '$scope', '$translate', '$http', '$location', '$routeParams','$modal', '$timeout', '$popover', 'DecisionTableService', 'hotRegisterer',
        function ($rootScope, $scope, $translate, $http, $location, $routeParams, $modal, $timeout, $popover, DecisionTableService, hotRegisterer) {

            extScope = $scope;

            $scope.decisionTableMode = 'read';

            // Initialize model
            $scope.model = {
                // Store the main model id, this points to the current version of a model,
                // even when we're showing history
                latestModelId: $routeParams.modelId,
                columnDefs: [],
                columnVariableIdMap: {},
                readOnly: true,
                availableVariableTypes: ['string', 'number', 'boolean', 'date', 'collection']
            };

            // Hot Model init
            $scope.model.hotSettings = {
                stretchH: 'all',
                outsideClickDeselects: false,
                manualColumnResize: false,
                readOnly: true,
                disableVisualSelection: true
            };

            var hotReadOnlyDecisionTableEditorInstance;
            var hitPolicies = ['FIRST', 'ANY', 'UNIQUE', 'PRIORITY', 'RULE ORDER', 'OUTPUT ORDER', 'COLLECT'];
            var operators = ['==', '!=', '<', '>', '>=', '<=', 'ANY OF', 'NONE OF', 'ALL OF', 'NOT ALL OF', 'IS IN', 'IS NOT IN'];
            var columnIdCounter = 0;
            var dateFormat = 'YYYY-MM-DD';

            var variableUndefined = $translate.instant('DECISION-TABLE-EDITOR.EMPTY-MESSAGES.NO-VARIABLE-SELECTED');
            // helper for looking up variable id by col id
            $scope.getVariableNameByColumnId = function (colId) {

                if (!colId) {
                    return;
                }

                if ($scope.model.columnVariableIdMap[colId]) {
                    return $scope.model.columnVariableIdMap[colId];
                } else {
                    return variableUndefined;
                }
            };

            $scope.loadDecisionTable = function() {
                var url, decisionTableUrl;
                if ($routeParams.modelHistoryId) {
                    url = FLOWABLE.APP_URL.getModelHistoryUrl($routeParams.modelId, $routeParams.modelHistoryId);
                    decisionTableUrl = FLOWABLE.APP_URL.getDecisionTableModelsHistoryUrl($routeParams.modelHistoryId);
                } else {
                    url = FLOWABLE.APP_URL.getModelUrl($routeParams.modelId);
                    decisionTableUrl = FLOWABLE.APP_URL.getDecisionTableModelUrl($routeParams.modelId);
                }

                $http({method: 'GET', url: url}).
                    success(function(data, status, headers, config) {
                        $scope.model.decisionTable = data;
                        $scope.model.decisionTableDownloadUrl = decisionTableUrl + '/export?version=' + Date.now();
                        $scope.loadVersions();

                    }).error(function(data, status, headers, config) {
                        $scope.returnToList();
                    });
            };

            $scope.useAsNewVersion = function() {
                _internalCreateModal({
                    template: 'views/popup/model-use-as-new-version.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.toggleFavorite = function() {
                $scope.model.favoritePending = true;

                var data = {
                    favorite: !$scope.model.decisionTable.favorite
                };

                $http({method: 'PUT', url: FLOWABLE.APP_URL.getModelUrl($scope.model.latestModelId), data: data}).
                    success(function(data, status, headers, config) {
                        $scope.model.favoritePending = false;
                        if ($scope.model.decisionTable.favorite) {
                            $scope.addAlertPromise($translate('DECISION-TABLE.ALERT.UN-FAVORITE-CONFIRM'), 'info');
                        } else {
                            $scope.addAlertPromise($translate('DECISION-TABLE.ALERT.FAVORITE-CONFIRM'), 'info');
                        }
                        $scope.model.decisionTable.favorite = !$scope.model.decisionTable.favorite;
                    }).error(function(data, status, headers, config) {
                        $scope.model.favoritePending = false;
                    });
            };


            $scope.loadVersions = function() {

                var params = {
                    includeLatestVersion: !$scope.model.decisionTable.latestVersion
                };

                $http({method: 'GET', url: FLOWABLE.APP_URL.getModelHistoriesUrl($scope.model.latestModelId), params: params}).
                    success(function(data, status, headers, config) {
                        if ($scope.model.decisionTable.latestVersion) {
                            if (!data.data) {
                                data.data = [];
                            }
                            data.data.unshift($scope.model.decisionTable);
                        }

                        $scope.model.versions = data;
                    });
            };

            $scope.showVersion = function(version) {
                if (version) {
                    if (version.latestVersion) {
                        $location.path("/decision-tables/" +  $scope.model.latestModelId);
                    } else {
                        // Show latest version, no history-suffix needed in URL
                        $location.path("/decision-tables/" +  $scope.model.latestModelId + "/history/" + version.id);
                    }
                }
            };

            $scope.returnToList = function() {
                $location.path("/decision-tables/");
            };

            $scope.editDecisionTable = function() {
                _internalCreateModal({
                    template: 'views/popup/model-edit.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.duplicateDecisionTable = function() {

                var modalInstance = _internalCreateModal({
                    template: 'views/popup/decision-table-duplicate.html?version=' + Date.now()
                }, $modal, $scope);

                modalInstance.$scope.originalModel = $scope.model;

                modalInstance.$scope.duplicateDecisionTableCallback = function(result) {
                    $rootScope.editorHistory = [];
                    $location.url("/decision-table-editor/" + encodeURIComponent(result.id));
                };
            };

            $scope.deleteDecisionTable = function() {
                _internalCreateModal({
                    template: 'views/popup/model-delete.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.shareDecisionTable = function() {
                _internalCreateModal({
                    template: 'views/popup/model-share.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openEditor = function() {
                if ($scope.model.decisionTable) {
                    $location.path("/decision-table-editor/" + $scope.model.decisionTable.id);
                }
            };

            $scope.toggleHistory = function($event) {
                if (!$scope.historyState) {
                    var state = {};
                    $scope.historyState = state;

                    // Create popover
                    state.popover = $popover(angular.element($event.target), {
                        template: 'views/popover/history.html',
                        placement: 'bottom-right',
                        show: true,
                        scope: $scope,
                        container: 'body'
                    });

                    var destroy = function() {
                        state.popover.destroy();
                        delete $scope.historyState;
                    };

                    // When popup is hidden or scope is destroyed, hide popup
                    state.popover.$scope.$on('tooltip.hide', destroy);
                    $scope.$on('$destroy', destroy);
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

            $scope.doAfterRender = function () {
                var element = document.querySelector("thead > tr > th:first-of-type");
                if (element) {
                    var firstChild = element.firstChild;
                    var newElement = angular.element('<div class="hit-policy-header"><a onclick="triggerHitPolicyEditor()">' + $scope.currentDecisionTable.hitIndicator.substring(0, 1) + '</a></div>');
                    element.className = 'hit-policy-container';
                    element.replaceChild(newElement[0], firstChild);
                }

                $timeout(function () {
                    hotReadOnlyDecisionTableEditorInstance = hotRegisterer.getInstance('read-only-decision-table-editor');
                    if (hotReadOnlyDecisionTableEditorInstance) {
                        hotReadOnlyDecisionTableEditorInstance.validateCells();
                    }
                });
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

            var isCustomExpression = function (val) {
                return !!(val != null
                    && (String(val).startsWith('${') || String(val).startsWith('#{')));
            };

            var isDashValue = function (val) {
                return !!(val != null && "-" === val);
            };

            var isCorrespondingCollectionOperator = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotReadOnlyDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                var isCollectionOperator = false;
                if (isOperatorCell(operatorCellMeta)) {
                    var operatorValue = hotReadOnlyDecisionTableEditorInstance.getDataAtCell(row, operatorCol);
                    if (operatorValue === "IN" || operatorValue === "NOT IN" || operatorValue === "ANY" || operatorValue === "NOT ANY") {
                        isCollectionOperator = true;
                    }
                }
                return isCollectionOperator;
            };

            var disableCorrespondingOperatorCell = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotReadOnlyDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                if (!isOperatorCell(operatorCellMeta)) {
                    return;
                }

                if (operatorCellMeta.className != null && operatorCellMeta.className.indexOf('custom-expression-operator') !== -1) {
                    return;
                }

                var currentEditor = hotReadOnlyDecisionTableEditorInstance.getCellEditor(row, operatorCol);

                hotReadOnlyDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'className', operatorCellMeta.className + ' custom-expression-operator');
                hotReadOnlyDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'originalEditor', currentEditor);
                hotReadOnlyDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'editor', false);
                hotReadOnlyDecisionTableEditorInstance.setDataAtCell(row, operatorCol, null);
            };

            var enableCorrespondingOperatorCell = function (row, prop) {
                var operatorCol = getCorrespondingOperatorCell(row, prop);
                var operatorCellMeta = hotReadOnlyDecisionTableEditorInstance.getCellMeta(row, operatorCol);

                if (!isOperatorCell(operatorCellMeta)) {
                    return;
                }

                if (operatorCellMeta == null || operatorCellMeta.className == null || operatorCellMeta.className.indexOf('custom-expression-operator') == -1) {
                    return;
                }

                operatorCellMeta.className = operatorCellMeta.className.replace('custom-expression-operator', '');
                hotReadOnlyDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'className', operatorCellMeta.className);
                hotReadOnlyDecisionTableEditorInstance.setCellMeta(row, operatorCol, 'editor', operatorCellMeta.originalEditor);
                hotReadOnlyDecisionTableEditorInstance.setDataAtCell(row, operatorCol, '==');
            };

            var getCorrespondingOperatorCell = function (row, prop) {
                var currentCol = hotReadOnlyDecisionTableEditorInstance.propToCol(prop);
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

            $scope.openHitPolicyEditor = function () {
                var editTemplate = 'views/popup/decision-table-edit-hit-policy.html';

                $scope.model.hitPolicy = $scope.currentDecisionTable.hitIndicator;

                _internalCreateModal({
                    template: editTemplate,
                    scope: $scope
                }, $modal, $scope);
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

            var createNewOutputExpression = function (outputExpression) {
                var newOutputExpression;
                if (outputExpression) {
                    newOutputExpression = {
                        id: _generateColumnId(),
                        label: outputExpression.label,
                        variableId: outputExpression.variableId,
                        type: outputExpression.variableType,
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

                });
            };

            var evaluateDecisionTableGrid = function (decisionTable) {
                $scope.evaluateDecisionHeaders(decisionTable);
                evaluateDecisionGrid(decisionTable);
            };

            var setGridValues = function (key, type) {
                if ($scope.model.rulesData) {
                    $scope.model.rulesData.forEach(function (rowData) {
                        if (type === 'input-operator') {
                            if (!(key in rowData) || rowData[key] === '') {
                                rowData[key] = '==';
                            }
                        }
                        // else if (type === 'input-expression') {
                        //     if (!(key in rowData) || rowData[key] === '') {
                        //         rowData[key] = '-';
                        //     }
                        // }
                    });
                }
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

            var createDefaultRow = function () {
                var defaultRow = {};
                $scope.model.columnDefs.forEach(function (columnDefinition) {
                    if (columnDefinition.expressionType === 'input-operator') {
                        defaultRow[columnDefinition.data] = '==';
                    }
                    // else if (columnDefinition.expressionType === 'input-expression') {
                    //     defaultRow[columnDefinition.data] = '-';
                    // }
                    else if (columnDefinition.expressionType === 'output') {
                        defaultRow[columnDefinition.data] = '';
                    }
                });

                return defaultRow;
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
                    source: operators
                };

                return columnDefinition;
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
                    '</div>',
                    expressionType: 'input-expression',
                    expression: inputExpression,
                    className: 'htCenter',
                    width: '200'
                };

                if (inputExpression.entries && inputExpression.entries.length > 0) {
                    var entriesOptionValues = inputExpression.entries.slice(0, inputExpression.entries.length);
                    entriesOptionValues.push('-', '', ' ');

                    columnDefinition.type = 'dropdown';
                    columnDefinition.strict = true;
                    columnDefinition.source = entriesOptionValues;

                    columnDefinition.title = '<div class="input-header">' +
                        '<a onclick="triggerExpressionEditor(\'input\',' + expressionPosition + ',false)"><span class="header-label">' + (inputExpression.label ? inputExpression.label : "New Input") + '</span></a>' +
                        '<br><span class="header-variable">' + (inputExpression.variableId ? inputExpression.variableId : "none") + '</span>' +
                        '<br/><span class="header-variable-type">' + (inputExpression.type ? inputExpression.type : "") + '</span>' +
                        '<br><span class="header-entries">[' + inputExpression.entries.join() + ']</span>' +
                        '</div>';
                }

                if (type === 'date') {
                    columnDefinition.dateFormat = dateFormat;
                } else if (type === 'dropdown') {
                    columnDefinition.source = ['true', 'false', '-'];
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
                        '</div>';
                } else {
                    title += '<div class="output-header">' +
                        '<a onclick="triggerExpressionEditor(\'output\',' + expressionPosition + ',false)"><span class="header-label">' + (outputExpression.label ? outputExpression.label : "New Output") + '</span></a>' +
                        '<br><span class="header-variable">' + (outputExpression.variableId ? outputExpression.variableId : "none") + '</span>' +
                        '<br/><span class="header-variable-type">' + (outputExpression.type ? outputExpression.type : "") + '</span>' +
                        '</div>'
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
                    if (hotReadOnlyDecisionTableEditorInstance) {
                        hotReadOnlyDecisionTableEditorInstance.render();
                    }
                });
            };

            // fetch table from service and populate model
            _loadDecisionTableDefinition($routeParams.modelId);

            var _generateColumnId = function () {
                columnIdCounter++;
                return "" + columnIdCounter;
            };

            // Load model needed for favorites
            $scope.loadDecisionTable();

        }]);
