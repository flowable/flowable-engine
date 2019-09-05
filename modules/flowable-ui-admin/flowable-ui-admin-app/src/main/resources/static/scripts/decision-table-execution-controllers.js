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

flowableAdminApp.controller('DecisionTableExecutionController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'dmn-engine', sub: 'executions'};

        $scope.returnToList = function () {
            $location.path("/decision-table-executions");
        };

        $scope.openDecisionTable = function (definition) {
            if (definition && definition.getProperty('id')) {
                $location.path("/decision-table/" + definition.getProperty('id'));
            }
        };

        $q.all([$translate('DECISION-TABLE-EXECUTION.HEADER.ID'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.NAME'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.VERSION'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.KEY')])
            .then(function (headers) {

                $scope.gridDecisionTables = {
                    data: 'decisionTables.data',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDecisionTable,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'name', displayName: headers[1]},
                        {field: 'version', displayName: headers[2]},
                        {field: 'key', displayName: headers[3]}
                    ]
                };
            });

        var formatInputVariablesInRows = function (inputVariables, inputVariableTypes) {
            if (inputVariables === null || inputVariableTypes === null) {
                return;
            }

            var result = [];
            var columnCounter = 1;
            var noColumns = 2;
            var tmpRow = [];

            for (var key in inputVariables) {
                if (inputVariables.hasOwnProperty(key)) {

                    tmpRow.push({
                        key: key,
                        value: inputVariables[key],
                        type: inputVariableTypes[key]
                    });

                    if (columnCounter === noColumns) {
                        result.push(tmpRow);
                    }

                    columnCounter++;
                    if (columnCounter > noColumns) {
                        tmpRow = [];
                        columnCounter = 0;
                    }
                }
            }
            return result;
        };

        $scope.executeWhenReady(function () {
            // Load historic execution
            $http({
                method: 'GET',
                url: '/app/rest/admin/decision-tables/history/' + $routeParams.executionId
            }).success(function (data, status, headers, config) {
                $scope.execution = data;
            }).error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });

            // Load historic execution audit data
            $http({
                method: 'GET',
                url: '/app/rest/admin/decision-tables/history/' + $routeParams.executionId + '/auditdata'
            }).success(function (data, status, headers, config) {
                $scope.auditData = data;
                $scope.formattedInputVariables = formatInputVariablesInRows(
                    data.inputVariables,
                    data.inputVariableTypes);
            });
        });
    }]);