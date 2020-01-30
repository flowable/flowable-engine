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
'use strict';

flowableAdminApp.controller('DecisionTableController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {
        $rootScope.navigation = {main: 'dmn-engine', sub: 'decision-tables'};
        
        $scope.returnToList = function () {
            $location.path("/decision-tables");
        };

        $scope.showAllDecisionTables = function () {
            // Populate the process-filter with parentId
            $rootScope.filters.forced.processDefinitionFilter = {
                deploymentId: $scope.deployment.id
            };
            $location.path("/process-definitions");
        };

        $scope.showAllExecutions = function () {
            $location.path("/decision-table-executions");
        };

        $scope.showDecisionTable = function () {
            $modal.open({
                templateUrl: 'views/decision-table-popup.html',
                windowClass: 'modal modal-full-width',
                controller: 'ShowDecisionTablePopupCtrl',
                resolve: {
                    decisionTable: function () {
                        return $scope.decisionTable;
                    }
                }
            });
        };

        $scope.showDecisionExecution = function (decisionExecution) {
            if (decisionExecution && decisionExecution.getProperty('id')) {
                $location.path("/decision-table-execution/"+decisionExecution.getProperty('id'));
            }
        };

        $q.all([$translate('DECISION-TABLE-EXECUTION.HEADER.ID'),
                $translate('DECISION-TABLE-EXECUTION.HEADER.PROCESS-INSTANCE-ID'),
                $translate('DECISION-TABLE-EXECUTION.HEADER.START-TIME'),
                $translate('DECISION-TABLE-EXECUTION.HEADER.END-TIME'),
                $translate('DECISION-TABLE-EXECUTION.HEADER.FAILED')])
            .then(function (headers) {

                $scope.gridDecisionExecutions = {
                    data: 'decisionExecutions.data',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: true,
                    rowHeight: 36,
                    afterSelectionChange: $scope.showDecisionExecution,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'instanceId', displayName: headers[1]},
                        {field: 'startTime', displayName: headers[2], cellTemplate: gridConstants.dateTemplate},
                        {field: 'endTime', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'failed', displayName: headers[4]}
                    ]
                };
            });

        $scope.executeWhenReady(function () {
            // Load deployment
            $http({method: 'GET', url: '/app/rest/admin/decision-tables/' + $routeParams.decisionTableId}).
            success(function (data, status, headers, config) {
                $scope.decisionTable = data;

                // Load decision executions
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/decision-tables/history?decisionKey=' + data.key + '&deploymentId=' + data.deploymentId
                }).
                success(function (executionsData, status, headers, config) {
                    $scope.decisionExecutions = executionsData;
                });
            }).
            error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
        });

    }]);

flowableAdminApp.controller('ShowDecisionTablePopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'decisionTable', '$timeout', '$translate', 'uiGridConstants',
        function ($rootScope, $scope, $modalInstance, $http, decisionTable, $timeout, $translate, uiGridConstants) {

            var MIN_COLUMN_WIDTH = 200;

            $scope.status = {loading: false};

            $scope.cancel = function () {
                if (!$scope.status.loading) {
                    $modalInstance.dismiss('cancel');
                }
            };

            $scope.popup = {
                currentDecisionTableRules: [],
                columnDefs: [],
                columnVariableIdMap: {}
            };

            $scope.executeWhenReady(function () {
                // Load deployment
                $http({
                    method: 'GET',
                    url: '/app/rest/admin/decision-tables/' + decisionTable.id + '/editorJson'
                }).
                success(function (data, status, headers, config) {
                    $scope.popup.currentDecisionTable = data;
                }).
                error(function (data, status, headers, config) {
                    if (data && data.message) {
                        // Extract error-message
                        $rootScope.addAlert(data.message, 'error');
                    } else {
                        // Use default error-message
                        $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                    }
                });
            });

            var variableUndefined = $translate.instant('DECISION-TABLE-EDITOR.EMPTY-MESSAGES.NO-VARIABLE-SELECTED');
            // helper for looking up variable id by col id
            $scope.getVariableNameByColumnId = function (colId) {

                if (!colId) {
                    return;
                }

                if ($scope.popup.columnVariableIdMap[colId]) {
                    return $scope.popup.columnVariableIdMap[colId];
                } else {
                    return variableUndefined;
                }
            };

            var _loadDecisionTableDefinition = function () {

                if ($scope.popup.currentDecisionTable.inputExpressions) {
                    $scope.popup.currentDecisionTable.inputExpressions.forEach(function (inputExpression) {
                        $scope.popup.columnVariableIdMap[inputExpression.id] = inputExpression.variableId;
                    });
                }

                if ($scope.popup.currentDecisionTable.outputExpressions) {
                    $scope.popup.currentDecisionTable.outputExpressions.forEach(function (outputExpression) {
                        $scope.popup.columnVariableIdMap[outputExpression.id] = outputExpression.variableId;
                    });
                }

                // initialize ui grid model
                if ($scope.popup.currentDecisionTable.rules && $scope.popup.currentDecisionTable.rules.length > 0) {
                    Array.prototype.push.apply($scope.popup.currentDecisionTableRules, $scope.popup.currentDecisionTable.rules);
                }

                // get column definitions
                $scope.getColumnDefinitions($scope.popup.currentDecisionTable);

            };

            // Custom UI grid template
            var _getHeaderInputExpressionCellTemplate = function () {
                return "" +
                    "<div role=\"columnheader\" ui-grid-one-bind-aria-labelledby-grid=\"col.uid + '-header-text ' + col.uid + '-sortdir-text'\">" +
                    "   <div class=\"ui-grid-cell-contents ui-grid-header-cell-primary-focus\" col-index=\"renderIndex\" title=\"TOOLTIP\">" +
                    "       <div class=\"text-center\"><span>{{ col.displayName CUSTOM_FILTERS }}</span></div>" +
                    "       <div class=\"text-center\"><span ui-grid-one-bind-id-grid=\"col.uid + '-header-text'\" style=\"text-decoration: underline;\">[ {{ grid.appScope.getVariableNameByColumnId(col.name) }} ]</span></div>" +
                    "   </div>" +
                    "</div>";
            };

            var _getHeaderOutputExpressionCellTemplate = function () {
                return "" +
                    "<div role=\"columnheader\" ui-grid-one-bind-aria-labelledby-grid=\"col.uid + '-header-text ' + col.uid + '-sortdir-text'\">" +
                    "   <div class=\"ui-grid-cell-contents ui-grid-header-cell-primary-focus\" col-index=\"renderIndex\" title=\"TOOLTIP\">" +
                    "       <div class=\"text-center\"><span>{{ col.displayName CUSTOM_FILTERS }}</span></div>" +
                    "       <div class=\"text-center\"><span ui-grid-one-bind-id-grid=\"col.uid + '-header-text'\" style=\"text-decoration: underline;\">[ {{ grid.appScope.getVariableNameByColumnId(col.name) }} ]</span></div>" +
                    "   </div>" +
                    "</div>";
            };

            var _rowHeaderTemplate = function () {
                return "<div class=\"ui-grid-disable-selection\"><div class=\"ui-grid-cell-contents text-center customRowHeader\">{{rowRenderIndex + 1}}</div></div>"
            };

            var _getCellTemplate = function (columnType) {
                var cellTemplate = "" +
                    "<div class=\"ui-grid-cell-contents\" ng-class=\"{ 'ui-grid-cell-contents-empty': !COL_FIELD }\" title=\"TOOLTIP\">" +
                    "   <span class=\"contents-value\">{{COL_FIELD}}</span>" +
                    "</div>";
                return cellTemplate;
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
                            headerCellTemplate: _getHeaderInputExpressionCellTemplate(),
                            cellClass: 'cell-expression cell-input-expression',
                            cellTemplate: _getCellTemplate(),
                            enableHiding: false,
                            enableCellEditOnFocus: false,
                            minWidth: MIN_COLUMN_WIDTH
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
                            headerCellTemplate: _getHeaderOutputExpressionCellTemplate(),
                            headerCellClass: 'header-expression header-output-expression',
                            cellClass: 'cell-expression cell-output-expression',
                            cellTemplate: _getCellTemplate(),
                            enableHiding: false,
                            enableCellEditOnFocus: false,
                            minWidth: MIN_COLUMN_WIDTH
                        });

                        expressionCounter++;
                    });
                }

                // merge models
                if ($scope.popup.columnDefs) {
                    $scope.popup.columnDefs.length = 0;
                }

                else {
                    $scope.popup.columnDefs = [];
                }
                Array.prototype.push.apply($scope.popup.columnDefs, newColumnDefs);

                $scope.popup.gridApi.core.notifyDataChange(uiGridConstants.dataChange.ALL);
            };

            // config for grid
            $scope.popup.gridOptions = {
                data: $scope.popup.currentDecisionTableRules,
                enableRowHeaderSelection: false,
                multiSelect: false,
                modifierKeysToMultiSelect: false,
                enableHorizontalScrollbar: 2,
                enableColumnMenus: false,
                enableSorting: false,
                enableCellEditOnFocus: false,
                columnDefs: $scope.popup.columnDefs
                //headerTemplate: 'views/templates/decision-table-header-template.html'
            };

            $scope.popup.gridOptions.onRegisterApi = function (gridApi) {
                //set gridApi on scope
                $scope.popup.gridApi = gridApi;

                var cellTemplate = _rowHeaderTemplate();   // you could use your own template here
                $scope.popup.gridApi.core.addRowHeaderColumn({name: 'rowHeaderCol', displayName: '', width: 35, cellTemplate: cellTemplate});

                // Load definition that will be rendered
                _loadDecisionTableDefinition();
            };
            

        }]);

