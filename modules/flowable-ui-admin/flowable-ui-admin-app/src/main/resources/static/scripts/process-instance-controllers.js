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

flowableAdminApp.controller('ProcessInstanceController', ['$scope', '$rootScope', '$http', '$timeout', '$location', '$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'process-engine', sub: 'instances'};

        $scope.tabData = {
            tabs: [
                {id: 'tasks', name: 'PROCESS-INSTANCE.TITLE.TASKS'},
                {id: 'variables', name: 'PROCESS-INSTANCE.TITLE.VARIABLES'},
                {id: 'subProcesses', name: 'PROCESS-INSTANCE.TITLE.SUBPROCESSES'},
                {id: 'jobs', name: 'PROCESS-INSTANCE.TITLE.JOBS'}
            ]
        };

        $scope.tabData.activeTab = $scope.tabData.tabs[0].id;

        $scope.returnToList = function () {
            $location.path("/process-instances");
        };

        $scope.openTask = function (task) {
            if (task && task.getProperty('id')) {
                $location.path("/task/" + task.getProperty('id'));
            }
        };

        $scope.openJob = function (job) {
            if (job && job.getProperty('id')) {
                $location.path("/job/" + job.getProperty('id'));
            }
        };

        $scope.openProcessInstance = function (instance) {
            if (instance) {
                var id;
                if (instance.getProperty !== undefined) {
                    id = instance.getProperty('id');
                } else {
                    id = instance;
                }
                $location.path("/process-instance/" + id);
            }
        };

        $scope.showAllTasks = function () {
            // Populate the task-filter with parentId
            $rootScope.filters.forced.taskFilter = {
                processInstanceId: $scope.process.id
            };
            $location.path("/tasks");
        };

        $scope.showAllSubprocesses = function () {
            // Populate the process-filter with parentId
            $rootScope.filters.forced.instanceFilter = {
                superProcessInstanceId: $scope.process.id
            };
            $scope.returnToList();
        };

        $scope.openProcessDefinition = function (processDefinitionId) {
            if (processDefinitionId) {
                $location.path("/process-definition/" + processDefinitionId);
            }
        };

        $scope.showProcessDiagram = function () {
            $modal.open({
                templateUrl: 'views/process-instance-diagram-popup.html',
                windowClass: 'modal modal-full-width',
                controller: 'ShowProcessInstanceDiagramPopupCtrl',
                resolve: {
                    process: function () {
                        return $scope.process;
                    }
                }
            });
        };
        
        $scope.showMigrateProcessDialog = function () {
            $scope.migrationScope = {
                targetProcessDefinition: undefined
            };
            var modalInstance = $modal.open({
                templateUrl: 'views/process-instance-migration-popup.html', 
                controller: 'ShowProcessInstanceMigrationPopupCtrl',
                resolve: {
                    process: function () {
                        return $scope.process;
                    },
                    processDefinition: function() {
                        return $scope.definition;
                    },
                    migrationScope: function() {
                        return $scope.migrationScope;
                    }
                }
            });
            
            modalInstance.result.then(function (migrateProcessInstance) {
                if (migrateProcessInstance) {
                    var migrationModalInstance = $modal.open({
                        templateUrl: 'views/process-instance-migration-diagram-popup.html',
                        windowClass: 'modal modal-full-width',
                        controller: 'ShowProcessInstanceMigrationDiagramPopupCtrl',
                        resolve: {
                            process: function () {
                                return $scope.process;
                            },
                            migrationScope: function () {
                                return $scope.migrationScope;
                            }
                        }
                    });
                    
                    migrationModalInstance.result.then(function (result) {
                        $scope.loadProcessInstance();
                    });
                }
            });
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

        $scope.loadProcessDefinition = function () {
            // Load definition
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-definitions/' + $scope.process.processDefinitionId
            }).success(function (data, status, headers, config) {
                $scope.definition = data;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadProcessInstance = function () {
            $scope.process = undefined;
            // Load process
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $routeParams.processInstanceId
            }).success(function (data, status, headers, config) {
                $scope.process = data;

                if (data) {
                    $scope.processCompleted = data.endTime != undefined;
                }

                // Start loading children
                $scope.loadProcessDefinition();
                $scope.loadTasks();
                $scope.loadVariables();
                $scope.loadSubProcesses();
                $scope.loadJobs();
                $scope.loadDecisionTables()

                $scope.tabData.tabs.push({id: 'decisionTables', name: 'PROCESS-INSTANCE.TITLE.DECISION-TABLES'});
                $scope.tabData.tabs.push({id: 'forms', name: 'PROCESS-INSTANCE.TITLE.FORM-INSTANCES'});
                //TODO: implement when decision task runtime data is stored
                // $scope.loadDecisionTables();
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
        $q.all([$translate('PROCESS-INSTANCES.HEADER.ID'),
            $translate('PROCESS-INSTANCES.HEADER.NAME'),
            $translate('PROCESS-INSTANCES.HEADER.PROCESS-DEFINITION'),
            $translate('PROCESS-INSTANCES.HEADER.STATUS')])
            .then(function (headers) {
                var subprocessStateTemplate = '<div><div class="ngCellText">{{row.getProperty("endTime") && "Completed" || "Active"}}</div></div>';
                // Config for variable grid
                $scope.gridSubprocesses = {
                    data: 'subprocesses.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected: false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openProcessInstance,
                    columnDefs: [
                        {field: 'id', displayName: headers[0]},
                        {field: 'name', displayName: headers[1]},
                        {field: 'processDefinitionId', displayName: headers[2]},
                        {field: 'endTime', displayName: headers[3], cellTemplate: subprocessStateTemplate}
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
            $translate('FORM-INSTANCE.HEADER.PROCESS-ID'),
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
                        {field: 'processInstanceId', displayName: headers[2]},
                        {field: 'submittedDate', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                        {field: 'submittedBy', displayName: headers[4], cellTemplate: gridConstants.userObjectTemplate}
                    ]
                };
            });

        $scope.showAllJobs = function () {
            // Populate the job-filter with process id
            $rootScope.filters.forced.jobFilter = {
                processInstanceId: $scope.process.id
            };
            $location.path("/jobs");
        };

        $scope.loadTasks = function () {
            $scope.tasks = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $scope.process.id + '/tasks'
            }).success(function (data, status, headers, config) {
                $scope.tasks = data;
                $scope.tabData.tabs[0].info = data.total;
            });
        };

        $scope.loadVariables = function () {
            $scope.variables = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $scope.process.id + '/variables'
            }).success(function (data, status, headers, config) {
                $scope.variables = data;
                $scope.tabData.tabs[1].info = data.total;
            });
        };

        $scope.loadSubProcesses = function () {
            $scope.subprocesses = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $scope.process.id + '/subprocesses'
            }).success(function (data, status, headers, config) {
                $scope.subprocesses = data;
                $scope.tabData.tabs[2].info = data.total;
            });
        };

        $scope.loadJobs = function () {
            $scope.jobs = undefined;
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $scope.process.id + '/jobs'
            }).success(function (data, status, headers, config) {
                $scope.jobs = data;
                $scope.tabData.tabs[3].info = data.total;
            });
        };

        $scope.loadProcessDefinition = function () {
            // Load definition
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-definitions/' + $scope.process.processDefinitionId
            }).success(function (data, status, headers, config) {
                $scope.definition = data;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadDecisionTables = function () {
            // Load decision tables
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-instances/' + $scope.process.id + '/decision-executions'
            }).success(function (data, status, headers, config) {
                $scope.decisionTables = data;
                $scope.tabData.tabs[4].info = data.total;
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.loadFormInstances = function () {
            // Load form instances
            $http({
                method: 'GET',
                url: '/app/rest/admin/process-form-instances/' + $scope.process.id
            }).success(function (data, status, headers, config) {
                $scope.formInstances = data;
                $scope.tabData.tabs[5].info = data.total;
            }).error(function (data, status, headers, config) {
            });
        };


        $scope.executeWhenReady(function () {
            $scope.loadProcessInstance();
        });


        // Dialogs
        $scope.deleteProcessInstance = function (action) {
            if (!action) {
                action = "delete";
            }
            var modalInstance = $modal.open({
                templateUrl: 'views/process-instance-delete-popup.html',
                controller: 'DeleteProcessModalInstanceCtrl',
                resolve: {
                    process: function () {
                        return $scope.process;
                    },
                    action: function () {
                        return action;
                    }
                }
            });

            modalInstance.result.then(function (deleteProcessInstance) {
                if (deleteProcessInstance) {
                    if (action == 'delete') {
                        $scope.addAlert($translate.instant('ALERT.PROCESS-INSTANCE.DELETED', $scope.process), 'info');
                        $scope.returnToList();
                    } else {
                        $scope.addAlert($translate.instant('ALERT.PROCESS-INSTANCE.TERMINATED', $scope.process), 'info');
                        $scope.loadProcessInstance();
                    }
                }
            });
        };

        $scope.updateSelectedVariable = function () {
            if ($scope.selectedVariables && $scope.selectedVariables.length > 0) {
                var selectedVariable = $scope.selectedVariables[0];
                var modalInstance = $modal.open({
                    templateUrl: 'views/update-variable-popup.html',
                    controller: 'UpdateVariableCtrl',
                    resolve: {
                        variable: function () {
                            return selectedVariable.variable;
                        },
                        processInstanceId: function () {
                            return $scope.process.id;
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
                    controller: 'DeleteVariableCtrl',
                    resolve: {
                        variable: function () {
                            return selectedVariable.variable;
                        },
                        processInstanceId: function () {
                            return $scope.process.id;
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
                controller: 'AddVariableCtrl',
                resolve: {
                    processInstanceId: function () {
                        return $scope.process.id;
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

        $scope.terminateProcessInstance = function () {
            $scope.deleteProcessInstance("terminate");
        };
    }]);

flowableAdminApp.controller('DeleteProcessModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'process', function ($rootScope, $scope, $modalInstance, $http, process, action) {

        $scope.process = process;
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
                method: 'POST', url: '/app/rest/admin/process-instances/' + $scope.process.id,
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

flowableAdminApp.controller('ShowProcessInstanceDiagramPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'process', '$timeout', function ($rootScope, $scope, $modalInstance, $http, process, $timeout) {

        $scope.model = {
            id: process.id,
            name: process.name
        };

        $scope.status = {loading: false};

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };

        $timeout(function () {
            $("#bpmnModel").attr("data-instance-id", process.id);
            $("#bpmnModel").attr("data-definition-id", process.processDefinitionId);
            $("#bpmnModel").attr("data-server-id", $rootScope.activeServers['process'].id);
            if (process.endTime != undefined) {
                $("#bpmnModel").attr("data-history-id", process.id);
            }
            $("#bpmnModel").load("./display/displaymodel.html?instanceId=" + process.id);
        }, 200);


    }]);
    
flowableAdminApp.controller('ShowProcessInstanceMigrationPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'process', 'processDefinition', 'migrationScope', 
    function ($rootScope, $scope, $modalInstance, $http, process, processDefinition, migrationScope) {

        $scope.process = process;
        $scope.processDefinition = processDefinition;
        $scope.status = {loading: false};
        $scope.model = {
            targetNotSelected: true,
            currentDefinitionId: processDefinition.id
        };
        
        $http({
            method: 'GET',
            url: '/app/rest/admin/process-definitions?key=' + $scope.processDefinition.key
        }).success(function (response, status, headers, config) {
            var definitionList = response.data;
            var finalResult = [];
            for (var i = 0; i < definitionList.length; i++) {
                if (definitionList[i].id !== processDefinition.id) {
                    finalResult.push(definitionList[i]);
                }
            }
            $scope.filteredProcessDefinitions = finalResult;
            
        }).error(function (data, status, headers, config) {
        });
        
        $scope.targetProcessDefinitionChanged = function () {
            if ($scope.model.newProcessDefinition && $scope.model.newProcessDefinition.length > 0) {
                migrationScope.targetProcessDefinition = $scope.model.newProcessDefinition;
                $scope.model.targetNotSelected = false;
            }
        };
        
        $scope.ok = function () {
            $modalInstance.close(true);
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };
    }]);
    
flowableAdminApp.controller('ShowProcessInstanceMigrationDiagramPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'process', 'migrationScope', '$timeout',
    function ($rootScope, $scope, $modalInstance, $http, process, migrationScope, $timeout) {

        $scope.model = {
            id: process.id,
            name: process.name
        };

        $scope.status = {loading: false};

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.close(true);
            }
        };

        $timeout(function () {
            $("#bpmnModel").attr("data-instance-id", process.id);
            $("#bpmnModel").attr("data-definition-id", process.processDefinitionId);
            $("#bpmnModel").attr("data-server-id", $rootScope.activeServers['process'].id);
            $("#targetModel").attr("data-migration-definition-id", migrationScope.targetProcessDefinition);
            
            $("#bpmnModel").load("./display/displaymodel.html?instanceId=" + process.id);
        }, 200);


    }]);

flowableAdminApp.controller('UpdateVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'variable', 'processInstanceId', function ($rootScope, $scope, $modalInstance, $http, variable, processInstanceId) {

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
                url: '/app/rest/admin/process-instances/' + processInstanceId + '/variables/' + $scope.updateVariable.name,
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

flowableAdminApp.controller('DeleteVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'variable', 'processInstanceId',
        function ($rootScope, $scope, $modalInstance, $http, variable, processInstanceId) {

            $scope.status = {loading: false};
            $scope.variable = variable;

            $scope.deleteVariable = function () {
                $http({
                    method: 'DELETE',
                    url: '/app/rest/admin/process-instances/' + processInstanceId + '/variables/' + $scope.variable.name
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

flowableAdminApp.controller('AddVariableCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'processInstanceId',
        function ($rootScope, $scope, $modalInstance, $http, processInstanceId) {

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
                    url: '/app/rest/admin/process-instances/' + processInstanceId + '/variables',
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
