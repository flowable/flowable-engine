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

angular.module('flowableApp')
  .controller('ProcessController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$modal', '$routeParams', 'AppDefinitionService',
    function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, $routeParams, AppDefinitionService) {

      // Ensure correct main page is set
      $rootScope.setMainPageById('processes');

      $scope.selectedProcessInstance = { id: $routeParams.processId };

        $scope.appDefinitionKey = $routeParams.appDefinitionKey;

        $scope.$on('processinstance-deleted', function (event, data) {
            $scope.openProcesses();
        });

        $scope.openProcesses = function(task) {
            var path='';
            if($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
                path = "/apps/" + $rootScope.activeAppDefinition.id;
            }
            $location.path(path + "/processes");
        };
}]);

angular.module('flowableApp')
    .controller('ProcessDetailController', ['$rootScope', '$scope', '$translate', '$http', '$timeout','$location', '$route', '$modal', '$routeParams', '$popover', 'appResourceRoot', 'TaskService', 'CommentService', 'RelatedContentService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $route, $modal, $routeParams, $popover, appResourceRoot, TaskService, CommentService, RelatedContentService) {

    $rootScope.root.showStartForm = false;

    $scope.model = {
        // Indirect binding between selected task in parent scope to have control over display
        // before actual selected task is switched
        processInstance: $scope.selectedProcessInstance
    };

    $scope.$watch('selectedProcessInstance', function(newValue) {
        if (newValue && newValue.id) {
            $scope.model.processUpdating = true;
            $scope.model.processInstance = newValue;

            $scope.getProcessInstance(newValue.id);
        }
    });

    $scope.getProcessInstance = function(processInstanceId) {
        $http({method: 'GET', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/process-instances/' + processInstanceId}).
            success(function(response, status, headers, config) {
                $scope.model.processInstance = response;
                $scope.loadProcessTasks();
                $scope.loadComments();
            }).
            error(function(response, status, headers, config) {
                console.log('Something went wrong: ' + response);
            });
    };

    $rootScope.loadProcessTasks = function() {

        // Runtime tasks
        TaskService.getProcessInstanceTasks($scope.model.processInstance.id, false).then(function(response) {
            $scope.model.processTasks = response.data;
        });

        TaskService.getProcessInstanceTasks($scope.model.processInstance.id, true).then(function(response) {
            if(response.data && response.data.length > 0) {
                $scope.model.completedProcessTasks = response.data;
            } else {
                $scope.model.completedProcessTasks = [];
            }

            // Calculate duration
            for(var i=0; i<response.data.length; i++) {
                var task = response.data[i];
                if(task.duration) {
                    task.duration = moment.duration(task.duration).humanize();
                }
            }
        });
    };

    $scope.toggleCreateComment = function() {

        $scope.model.addComment = !$scope.model.addComment;

        if($scope.model.addComment) {
            $timeout(function() {
                angular.element('.focusable').focus();
            }, 100);
        }
    };

    $scope.cancelProcess = function(final) {
        if ($scope.model.processInstance) {
            var modalInstance = _internalCreateModal({
                template: appResourceRoot + 'views/modal/process-cancel.html',
                scope: $scope,
                show: true
            }, $modal, $scope);

            if(final) {
                modalInstance.$scope.finalDelete = true;
            }
        }
    };

    $scope.deleteProcess = function() {
        $scope.cancelProcess(true);
    };

    $scope.$on('processinstance-deleted', function (event, data) {
        $route.reload();
    });

    $scope.openTask = function(task) {
        // TODO: use URL instead
        $rootScope.root.selectedTaskId = task.id;
        var path='';
        if($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
            path = "/apps/" + $rootScope.activeAppDefinition.id;
        }
        $location.path(path + "/tasks");
    };

    $scope.openStartForm = function() {
        $rootScope.root.showStartForm = true;
        $rootScope.root.selectedProcessId = $scope.model.processInstance.id;
    };

    $scope.popupShown = function() {

    };

    $scope.closeDiagramPopup = function() {
        jQuery('.qtip').qtip('destroy', true);
    };

    $scope.loadComments = function() {
        CommentService.getProcessInstanceComments($scope.model.processInstance.id).then(function (data) {
            $scope.model.comments = data;
        });
    };

    $scope.confirmNewComment = function() {
        $scope.model.commentLoading = true;

        CommentService.createProcessInstanceComment($scope.model.processInstance.id, $scope.model.newComment.trim())
            .then(function(comment) {
                $scope.model.newComment = undefined;
                $scope.model.commentLoading = false;
                $rootScope.addAlertPromise($translate('PROCESS.ALERT.COMMENT-ADDED'));

                $scope.toggleCreateComment();


                $scope.loadComments();
            });
    };

    $scope.showDiagram = function() {
        var modalInstance = _internalCreateModal({
            template: appResourceRoot + 'views/modal/process-instance-graphical.html',
            scope: $scope,
            show: true
        }, $modal, $scope);

    };
}]);

angular.module('flowableApp')
    .controller('ShowProcessDiagramCtrl', ['$scope', '$http', '$interval', '$timeout', '$translate', '$q', 'ResourceService', 'appResourceRoot',
        function ($scope, $http, $interval, $timeout, $translate, $q, ResourceService, appResourceRoot) {

            $scope.model.isDebuggerEnabled = false;

            $http({
                method: 'GET',
                url: '../app/rest/debugger/',
                async: false
            }).success(function (data) {
                $scope.model.isDebuggerEnabled = data;
            });

            $scope.model.variables = [];
            $scope.model.executions = undefined;
            $scope.model.selectedExecution = $scope.model.processInstance.id;
            $scope.model.displayVariables = true;

            $scope.model.errorMessage = '';

            // config for executions grid
            $scope.gridExecutions = {
                data: $scope.model.executions,
                columnDefs: [
                    {field: 'id', displayName: "Id", name: 'id', maxWidth: 10},
                    {field: 'parentId', displayName: "Parent id", name: 'parentId', maxWidth: 10},
                    {
                        field: 'processInstanceId',
                        displayName: "Process id",
                        name: 'processInstanceId',
                        maxWidth: 90
                    },
                    {
                        field: 'superExecutionId',
                        displayName: "Super execution id",
                        name: 'superExecutionId',
                        maxWidth: 90
                    },
                    {field: 'activityId', displayName: "Activity", name: 'activityId', maxWidth: 90},
                    {field: 'suspended', displayName: "Suspended", name: 'suspended', maxWidth: 90},
                    {field: 'tenantId', displayName: "Tenant id", name: 'tenantId', maxWidth: 90}
                ],
                enableRowSelection: true,
                multiSelect: false,
                noUnselect: true,
                enableRowHeaderSelection: false,
                onRegisterApi: function (gridApi) {
                    $scope.gridExecutionsApi = gridApi;
                    $scope.gridExecutionsApi.grid.modifyRows($scope.gridExecutions.data);
                    if ($scope.gridExecutions.data) {
                        for (var i = 0; i < $scope.gridExecutions.data.length; i++) {
                            if ($scope.model.selectedExecution == $scope.gridExecutions.data[i].id) {
                                $scope.gridExecutionsApi.selection.selectRow($scope.gridExecutions.data[i]);
                                i = $scope.gridExecutions.data.length;
                            }
                        }
                    }
                    $scope.gridExecutionsApi.selection.on.rowSelectionChanged($scope, function (row) {
                        var activityToUnselect = modelDiv.attr("selected-activity");
                        if (activityToUnselect) {
                            var rectangleToUnselect = paper.getById(activityToUnselect);
                            if (rectangleToUnselect) {
                                rectangleToUnselect.attr({"stroke": "green"});
                            }
                        }
                        modelDiv.attr("selected-execution", row.entity.id);
                        $scope.model.selectedExecution = row.entity.id;
                        modelDiv.attr("selected-activity", row.entity.activityId);
                        if (row.entity.activityId) {
                            var paperActivity = paper.getById(row.entity.activityId);
                            if (paperActivity) {
                                paperActivity.attr({"stroke": "red"});
                            }
                        }

                        $scope.loadVariables();
                    });
                }
            };

            $scope.getExecutions = function () {
                $http({
                    method: 'GET',
                    url: '../app/rest/debugger/executions/' + $scope.model.processInstance.id
                }).success(function (data) {
                    $scope.model.executions = data;
                    $scope.gridExecutions.data = data;
                    if ($scope.gridExecutionsApi) {
                        $scope.gridExecutionsApi.grid.modifyRows($scope.gridExecutions.data);
                        for (var i = 0; i < $scope.gridExecutions.data.length; i++) {
                            if ($scope.model.selectedExecution == $scope.gridExecutions.data[i].id) {
                                $scope.gridExecutionsApi.selection.selectRow($scope.gridExecutions.data[i]);
                                i = $scope.gridExecutions.data.length;
                            }
                        }
                    }
                    jQuery("#bpmnModel").data($scope.model.executions);
                }).error(function (data, status, headers, config) {
                    $scope.model.errorMessage = data;
                });
            }

            $scope.getExecutions();

            $http({
                method: 'GET',
                url: '../app/rest/debugger/variables/' + $scope.model.processInstance.id 
            }).success(function (data) {
                $scope.gridVariables.data = data;
                if ($scope.gridVariablesApi) {
                    $scope.gridVariablesApi.core.refresh();
                }
            });

            $scope.getEventLog = function () {
                $http({
                    method: 'GET',
                    url: '../app/rest/debugger/eventlog/' + $scope.model.processInstance.id
                }).success(function (data) {
                    $scope.gridLog.data = data;
                    if ($scope.gridLogApi) {
                        $scope.gridLogApi.core.refresh();
                    }
                });
            }
            $scope.getEventLog();

            $scope.tabData = {
                tabs: [
                    {id: 'variables', name: 'PROCESS.TITLE.VARIABLES'},
                    {id: 'executions', name: 'PROCESS.TITLE.EXECUTIONS'},
                    {id: 'log', name: 'PROCESS.TITLE.LOG'}
                ],
                activeTab: 'variables'
            };

            $scope.loadVariables = function () {
                $http({
                    method: 'GET',
                    url: '../app/rest/debugger/variables/' + jQuery("#bpmnModel").attr("selected-execution")
                }).success(function (data, status, headers, config) {
                    $scope.model.variables = data;
                    $scope.gridVariables.data = data;
                    if ($scope.gridVariablesApi) {
                        $scope.gridVariablesApi.core.refresh();
                    }
                });
            };

            $scope.executionSelected = function () {
                jQuery("#bpmnModel").attr("selectedElement", $scope.model.selectedExecution.activityId);
                $scope.loadVariables();
            }

            // Config for variable grid
            $scope.gridVariables = {
                data: $scope.model.variables,
                columnDefs: [
                    {field: 'processId', displayName: "Process", maxWidth: 10},
                    {field: 'executionId', displayName: "Execution", maxWidth: 10},
                    {field: 'taskId', displayName: "Task", maxWidth: 10},
                    {field: 'type', displayName: "Type", maxWidth: 10},
                    {field: 'name', displayName: "Name", maxWidth: 10},
                    {
                        field: 'value', displayName: "Value",
                        cellTemplate: '<div><div style="text-align: left" class="ngCellText">{{grid.getCellValue(row, col)}}</div></div>'
                    }
                ],
                onRegisterApi: function (gridApi) {
                    $scope.gridVariablesApi = gridApi;
                }
            };

            // Config for variable grid
            $scope.gridLog = {
                columnDefs: [
                    {field: 'id', displayName: "Id", maxWidth: 10},
                    {field: 'type', displayName: "Type", maxWidth: 10},
                    {field: 'timeStamp', displayName: "Time Stamp", maxWidth: 90},
                    {field: 'executionId', displayName: "Execution", maxWidth: 90},
                    {field: 'taskId', displayName: "Task id", maxWidth: 90}
                ],
                enableRowSelection: true,
                multiSelect: false,
                noUnselect: true,
                enableRowHeaderSelection: false,
                onRegisterApi: function (gridApi) {
                    $scope.gridLogApi = gridApi;
                }
            };

            $timeout(function () {
                jQuery("#bpmnModel").attr('data-model-id', $scope.model.processInstance.id);
                jQuery("#bpmnModel").attr('data-model-type', 'runtime');

                // in case we want to show a historic model, include additional attribute on the div
                if ($scope.model.processInstance.ended) {
                    jQuery("#bpmnModel").attr('data-history-id', $scope.model.processInstance.id);
                }

                var viewerUrl = appResourceRoot + "../display/displaymodel.html?version=" + Date.now();

                // If Flowable has been deployed inside an AMD environment Raphael will fail to register
                // itself globally until displaymodel.js (which depends ona global Raphale variable) is running,
                // therefore remove AMD's define method until we have loaded in Raphael and displaymodel.js
                // and assume/hope its not used during.
                var amdDefine = window.define;
                window.define = undefined;
                ResourceService.loadFromHtml(viewerUrl, function () {
                    // Restore AMD's define method again
                    window.define = amdDefine;
                });
            }, 100);
        }
        ]
    );

angular.module('flowableApp')
.controller('CancelProcessCtrl', ['$scope', '$http', '$route', 'ProcessService', function ($scope, $http, $route, ProcessService) {

        $scope.popup = {loading: false};

        $scope.ok = function() {
            $scope.popup.loading = true;

            ProcessService.deleteProcess($scope.model.processInstance.id).
                then(function(response, status, headers, config) {
                    $scope.$hide();
                }).
                finally(function(response, status, headers, config) {
                    $scope.popup.loading = false;
                })
        };

        $scope.cancel = function() {
            $scope.$hide();
        }
    }
]);
