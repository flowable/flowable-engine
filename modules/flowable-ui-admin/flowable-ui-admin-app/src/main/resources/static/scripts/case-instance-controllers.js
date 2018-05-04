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

flowableAdminApp.controller('CaseInstanceController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'cmmn-engine', sub: 'instances'};

        $scope.tabData = {
            tabs: [
                {id: 'tasks', name: 'CASE-INSTANCE.TITLE.TASKS'},
                {id: 'variables', name: 'CASE-INSTANCE.TITLE.VARIABLES'},
                {id: 'jobs', name: 'CASE-INSTANCE.TITLE.JOBS'}
            ]
        };

        $scope.tabData.activeTab = $scope.tabData.tabs[0].id;

        $scope.returnToList = function () {
            $location.path("/case-instances");
        };

        $scope.openTask = function (task) {
            if (task && task.getProperty('id')) {
                $location.path("/cmmn-task/" + task.getProperty('id'));
            }
        };

        $scope.openJob = function (job) {
            if (job && job.getProperty('id')) {
                $location.path("/job/" + job.getProperty('id'));
            }
        };

        $scope.openCaseInstance = function (instance) {
            if (instance) {
                var id;
                if (instance.getProperty !== undefined) {
                    id = instance.getProperty('id');
                } else {
                    id = instance;
                }
                $location.path("/case-instance/" + id);
            }
        };

        $scope.showAllTasks = function () {
            // Populate the task-filter with parentId
            $rootScope.filters.forced.cmmnTaskFilter = {
                caseInstanceId: $scope.caseInstance.id
            };
            $location.path("/cmmn-tasks");
        };

        $scope.openCaseDefinition = function (caseDefinitionId) {
            if (caseDefinitionId) {
                $location.path("/case-definition/" + caseDefinitionId);
            }
        };

        $scope.openDecisionTable = function (decisionTable) {
            if (decisionTable && decisionTable.getProperty('id')) {
                $location.path("/decision-table-execution/" + decisionTable.getProperty('id'));
            }
        };

        $scope.openFormInstance = function (submittedForm) {
            if (submittedForm && submittedForm.getProperty('id')) {
                $location.path("/form-instance/" + submittedForm.getProperty('id'));
            }
        };

        $scope.loadCaseDefinition = function () {
            // Load definition
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-definitions/' + $scope.caseInstance.caseDefinitionId
            }).success(function (data, status, headers, config) {
                $scope.definition = data;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadCaseInstance = function () {
            $scope.caseInstance = undefined;
            // Load process
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-instances/' + $routeParams.caseInstanceId
            }).success(function (data, status, headers, config) {
                $scope.caseInstance = data;

                if (data) {
                    $scope.caseCompleted = data.endTime != undefined;
                }

                // Start loading children
                $scope.loadCaseDefinition();
                $scope.loadTasks();
                $scope.loadVariables();
                $scope.loadJobs();
                $scope.loadDecisionTables()

                $scope.tabData.tabs.push({id: 'decisionTables', name: 'CASE-INSTANCE.TITLE.DECISION-TABLES'});
                $scope.tabData.tabs.push({id: 'forms', name: 'CASE-INSTANCE.TITLE.FORM-INSTANCES'});
                $scope.loadFormInstances();
                
            }).error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
        };

        var dateTemplate = '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>';

        // Config for subtasks grid
        $q.all([$translate('TASKS.HEADER.ID'),
            $translate('TASKS.HEADER.NAME'),
            $translate('TASKS.HEADER.ASSIGNEE'),
            $translate('TASKS.HEADER.OWNER'),
            $translate('TASKS.HEADER.CREATE-TIME'),
            $translate('TASKS.HEADER.END-TIME')])
            .then(function (headers) {
                $scope.gridTasks = {
                    data: 'tasks.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openTask,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], width: 50},
                        {field: 'name', displayName: headers[1]},
                        {field: 'assignee', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'owner', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'startTime', displayName: headers[4], cellTemplate: dateTemplate},
                        {field: 'endTime', displayName: headers[5], cellTemplate: dateTemplate}
                    ]
                };
            });

        $q.all([$translate('VARIABLES.HEADER.NAME'),
            $translate('VARIABLES.HEADER.TYPE'),
            $translate('VARIABLES.HEADER.VALUE')])
            .then(function (headers) {
                var variableValueTemplate = '<div><div class="ngCellText">{{row.getProperty("variable.valueUrl") && "(Binary)" || row.getProperty(col.field)}}</div></div>';
                var variableTypeTemplate = '<div><div class="ngCellText">{{row.getProperty(col.field) && row.getProperty(col.field) || "null"}}</div></div>';

                $scope.selectedVariables = [];

                // Config for variable grid
                $scope.gridVariables = {
                    data: 'variables.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    selectedItems: $scope.selectedVariables,
                    columnDefs: [
                        {field: 'variable.name', displayName: headers[0]},
                        {field: 'variable.type', displayName: headers[1], cellTemplate: variableTypeTemplate},
                        {field: 'variable.value', displayName: headers[2], cellTemplate: variableValueTemplate}
                    ]
                };
            });
        
        $q.all([$translate('JOBS.HEADER.ID'),
            $translate('JOBS.HEADER.DUE-DATE'),
            $translate('JOBS.HEADER.RETRIES'),
            $translate('JOBS.HEADER.EXCEPTION')])
            .then(function (headers) {
                var dateTemplate = '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>';

                // Config for variable grid
                $scope.gridJobs = {
                    data: 'jobs.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openJob,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], width: 50},
                        {field: 'dueDate', displayName: headers[1], cellTemplate: dateTemplate},
                        {field: 'retries', displayName: headers[2]},
                        {field: 'exceptionMessage', displayName: headers[3]}
                    ]
                };
            });
            
        $q.all([$translate('DECISION-TABLE-EXECUTION.HEADER.ID'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.DECISION-KEY'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.DECISION-DEFINITION-ID'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.END-TIME'),
            $translate('DECISION-TABLE-EXECUTION.HEADER.FAILED')])
            .then(function (headers) {

                $scope.gridDecisionTables = {
                    data: 'decisionTables.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDecisionTable,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'decisionKey', displayName: headers[1]},
                        {field: 'decisionDefinitionId', displayName: headers[2]},
                        {
                            field: 'endTime',
                            displayName: headers[3],
                            cellTemplate: gridConstants.dateTemplate
                        },
                        {field: 'decisionExecutionFailed', displayName: headers[4]}
                    ]
                };
            });

        $q.all([$translate('FORM-INSTANCE.HEADER.ID'),
            $translate('FORM-INSTANCE.HEADER.TASK-ID'),
            $translate('FORM-INSTANCE.HEADER.CASE-ID'),
            $translate('FORM-INSTANCE.HEADER.SUBMITTED'),
            $translate('FORM-INSTANCE.HEADER.SUBMITTED-BY')])
            .then(function (headers) {

                $scope.gridFormInstances = {
                    data: 'formInstances.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openFormInstance,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'taskId', displayName: headers[1]},
                        {field: 'scopeId', displayName: headers[2]},
                        {field: 'submittedDate', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'submittedBy', displayName: headers[4], cellTemplate: gridConstants.userObjectTemplate}
                    ]
                };
            });

        $scope.showAllJobs = function () {
            // Populate the job-filter with case id
            $rootScope.filters.forced.jobFilter = {
                caseInstanceId: $scope.caseInstance.id
            };
            $location.path("/jobs");
        };

        $scope.loadTasks = function () {
            $scope.tasks = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-instances/' + $scope.caseInstance.id + '/tasks'
            }).success(function (data, status, headers, config) {
                $scope.tasks = data;
                $scope.tabData.tabs[0].info = data.total;
            });
        };

        $scope.loadVariables = function () {
            $scope.variables = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-instances/' + $scope.caseInstance.id + '/variables'
            }).success(function (data, status, headers, config) {
                $scope.variables = data;
                $scope.tabData.tabs[1].info = data.total;
            });
        };

        $scope.loadJobs = function () {
            $scope.jobs = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-instances/' + $scope.caseInstance.id + '/jobs'
            }).success(function (data, status, headers, config) {
                $scope.jobs = data;
                $scope.tabData.tabs[3].info = data.total;
            });
        };

        $scope.loadCaseDefinition = function () {
            // Load definition
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-definitions/' + $scope.caseInstance.caseDefinitionId
            }).success(function (data, status, headers, config) {
                $scope.definition = data;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadDecisionTables = function () {
            // Load decision tables
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-instances/' + $scope.caseInstance.id + '/decision-executions'
            }).success(function (data, status, headers, config) {
                $scope.decisionTables = data;
                $scope.tabData.tabs[3].info = data.total;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadFormInstances = function () {
            // Load form instances
            $http({
                method: 'GET',
                url: '/app/rest/admin/case-form-instances/' + $scope.caseInstance.id
            }).success(function (response, status, headers, config) {
                $scope.formInstances = response;
                $scope.tabData.tabs[4].info = response.total;
            }).error(function (data, status, headers, config) {
            });
        };


        $scope.executeWhenReady(function () {
            $scope.loadCaseInstance();
        });


        // Dialogs
        $scope.deleteCaseInstance = function (action) {
            if (!action) {
                action = "delete";
            }
            var modalInstance = $modal.open({
                templateUrl: 'views/case-instance-delete-popup.html',
                controller: 'DeleteCaseModalInstanceCtrl',
                resolve: {
                    process: function () {
                        return $scope.caseInstance;
                    },
                    action: function () {
                        return action;
                    }
                }
            });

            modalInstance.result.then(function (deleteCaseInstance) {
                if (deleteProcessInstance) {
                    if (action == 'delete') {
                        $scope.addAlert($translate.instant('ALERT.CASE-INSTANCE.DELETED', $scope.caseInstance), 'info');
                        $scope.returnToList();
                    } else {
                        $scope.addAlert($translate.instant('ALERT.CASE-INSTANCE.TERMINATED', $scope.caseInstance), 'info');
                        $scope.loadCaseInstance();
                    }
                }
            });
        };

        $scope.updateSelectedVariable = function () {
            if ($scope.selectedVariables && $scope.selectedVariables.length > 0) {
                var selectedVariable = $scope.selectedVariables[0];
                var modalInstance = $modal.open({
                    templateUrl: 'views/update-variable-popup.html',
                    controller: 'UpdateCaseInstanceVariableCtrl',
                    resolve: {
                        variable: function () {
                            return selectedVariable.variable;
                        },
                        caseInstanceId: function () {
                            return $scope.caseInstance.id;
                        }
                    }
                });

                modalInstance.result.then(function (updated) {
                    if (updated == true) {
                        $scope.selectedVariables.splice(0, $scope.selectedVariables.length);
                        $scope.loadVariables();
                    }
                });
            }
        };

        $scope.deleteVariable = function () {
            if ($scope.selectedVariables && $scope.selectedVariables.length > 0) {
                var selectedVariable = $scope.selectedVariables[0];
                var modalInstance = $modal.open({
                    templateUrl: 'views/variable-delete-popup.html',
                    controller: 'DeleteCaseInstanceVariableCtrl',
                    resolve: {
                        variable: function () {
                            return selectedVariable.variable;
                        },
                        caseInstanceId: function () {
                            return $scope.caseInstance.id;
                        }
                    }
                });

                modalInstance.result.then(function (updated) {
                    if (updated == true) {
                        $scope.selectedVariables.splice(0, $scope.selectedVariables.length);
                        $scope.loadVariables();
                    }
                });
            }
        };

        $scope.addVariable = function () {
            var modalInstance = $modal.open({
                templateUrl: 'views/variable-add-popup.html',
                controller: 'AddCaseInstanceVariableCtrl',
                resolve: {
                    caseInstanceId: function () {
                        return $scope.caseInstance.id;
                    }
                }
            });

            modalInstance.result.then(function (updated) {
                if (updated == true) {
                    $scope.selectedVariables.splice(0, $scope.selectedVariables.length);
                    $scope.loadVariables();
                }
            });
        };

        $scope.terminateCaseInstance = function () {
            $scope.deleteCaseInstance("terminate");
        };
    }]);

flowableAdminApp.controller('DeleteCaseModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'caseInstance', 'action', function ($rootScope, $scope, $modalInstance, $http, caseInstance, action) {

        $scope.caseInstance = caseInstance;
        $scope.action = action;
        $scope.status = {loading: false};
        $scope.model = {};
        $scope.ok = function () {
            $scope.status.loading = true;

            var dataForPost = {action: $scope.action};
            if ($scope.action == 'terminate' && $scope.model.deleteReason) {
                dataForPost.deleteReason = $scope.model.deleteReason;
            }

            $http({
                method: 'POST', url: '/app/rest/admin/case-instances/' + $scope.caseInstance.id,
                data: dataForPost
            }).success(function (data, status, headers, config) {
                $modalInstance.close(true);
                $scope.status.loading = false;
            }).error(function (data, status, headers, config) {
                $modalInstance.close(false);
                $scope.status.loading = false;
            });
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };
    }]);

flowableAdminApp.controller('ShowCaseInstanceDiagramPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'caseInstance', '$timeout', function ($rootScope, $scope, $modalInstance, $http, caseInstance, $timeout) {

        $scope.model = {
            id: caseInstance.id,
            name: caseInstance.name
        };

        $scope.status = {loading: false};

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };

        $timeout(function () {
            $("#bpmnModel").attr("data-instance-id", caseInstance.id);
            $("#bpmnModel").attr("data-definition-id", caseInstance.caseDefinitionId);
            $("#bpmnModel").attr("data-server-id", $rootScope.activeServers['cmmn'].id);
            if (process.endTime != undefined) {
                $("#bpmnModel").attr("data-history-id", caseInstance.id);
            }
            $("#bpmnModel").load("./display/displaymodel.html?instanceId=" + caseInstance.id);
        }, 200);


    }]);

flowableAdminApp.controller('UpdateCaseInstanceVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'variable', 'caseInstanceId', function ($rootScope, $scope, $modalInstance, $http, variable, caseInstanceId) {

        $scope.status = {loading: false};
        $scope.originalVariable = variable;

        $scope.updateVariable = {
            name: variable.name,
            value: variable.value,
            type: variable.type
        };

        $scope.executeUpdateVariable = function () {

            $scope.status.loading = true;

            var dataForPut = {
                name: $scope.updateVariable.name,
                type: $scope.updateVariable.type
            };

            if ($scope.updateVariable.value !== null || $scope.updateVariable.value !== undefined || $scope.updateVariable.value !== '') {

                if ($scope.updateVariable.type === 'string') {

                    dataForPut.value = $scope.updateVariable.value;

                } else if ($scope.updateVariable.type === 'boolean') {

                    if ($scope.updateVariable.value) {
                        dataForPut.value = true;
                    } else {
                        dataForPut.value = false;
                    }

                } else if ($scope.updateVariable.type === 'date') {

                    dataForPut.value = $scope.updateVariable.value;

                } else if ($scope.updateVariable.type === 'double'
                    || $scope.updateVariable.type === 'long'
                    || $scope.updateVariable.type === 'integer'
                    || $scope.updateVariable.type === 'short') {

                    dataForPut.value = Number($scope.updateVariable.value);

                }

            } else {

                dataForPut.value = null;

            }

            $http({
                method: 'PUT',
                url: '/app/rest/admin/case-instances/' + caseInstanceId + '/variables/' + $scope.updateVariable.name,
                data: dataForPut
            }).success(function (data, status, headers, config) {
                $modalInstance.close(true);
                $scope.status.loading = false;
            }).error(function (data, status, headers, config) {
                $modalInstance.close(false);
                $scope.status.loading = false;
            });

        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

    }]);

flowableAdminApp.controller('DeleteCaseInstanceVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'variable', 'caseInstanceId',
        function ($rootScope, $scope, $modalInstance, $http, variable, caseInstanceId) {

            $scope.status = {loading: false};
            $scope.variable = variable;

            $scope.deleteVariable = function () {
                $http({
                    method: 'DELETE',
                    url: '/app/rest/admin/case-instances/' + caseInstanceId + '/variables/' + $scope.variable.name
                }).success(function (data, status, headers, config) {
                    $modalInstance.close(true);
                    $scope.status.loading = false;
                }).error(function (data, status, headers, config) {
                    $modalInstance.close(false);
                    $scope.status.loading = false;
                });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

        }]);

flowableAdminApp.controller('AddCaseInstanceVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'caseInstanceId',
        function ($rootScope, $scope, $modalInstance, $http, caseInstanceId) {

            $scope.status = {loading: false};

            $scope.types = [
                "string",
                "boolean",
                "date",
                "double",
                "integer",
                "long",
                "short"
            ];

            $scope.newVariable = {};

            $scope.createVariable = function () {

                var data = {
                    name: $scope.newVariable.name,
                    type: $scope.newVariable.type,
                };

                if ($scope.newVariable.type === 'string') {

                    data.value = $scope.newVariable.value;

                } else if ($scope.newVariable.type === 'boolean') {

                    if ($scope.newVariable.value) {
                        data.value = true;
                    } else {
                        data.value = false;
                    }

                } else if ($scope.newVariable.type === 'date') {

                    data.value = $scope.newVariable.value;

                } else if ($scope.newVariable.type === 'double'
                    || $scope.newVariable.type === 'long'
                    || $scope.newVariable.type === 'integer'
                    || $scope.newVariable.type === 'short') {

                    data.value = Number($scope.newVariable.value);

                }

                $http({
                    method: 'POST',
                    url: '/app/rest/admin/case-instances/' + caseInstanceId + '/variables',
                    data: data
                }).success(function (data, status, headers, config) {
                    $modalInstance.close(true);
                    $scope.status.loading = false;
                }).error(function (data, status, headers, config) {
                    $modalInstance.close(false);
                    $scope.status.loading = false;
                });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

        }]);
