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
    .controller('CaseController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$modal', '$routeParams', 'AppDefinitionService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, $routeParams, AppDefinitionService) {

            // Ensure correct main page is set
            $rootScope.setMainPageById('cases');

            $scope.selectedCaseInstance = {id: $routeParams.caseId};

            $scope.appDefinitionKey = $routeParams.appDefinitionKey;

            $scope.$on('caseinstance-deleted', function (event, data) {
                $scope.openCases();
            });

            $scope.openCases = function (task) {
                var path = '';
                if ($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
                    path = "/apps/" + $rootScope.activeAppDefinition.id;
                }
                $location.path(path + "/cases");
            };
        }]);

angular.module('flowableApp')
    .controller('CaseDetailController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$route', '$modal', '$routeParams', '$popover', 'appResourceRoot', 'TaskService', 'CaseService', 'CommentService', 'RelatedContentService', 'MilestoneService', 'StageService', 'UserEventListenerService', 'PlanItemInstanceService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $route, $modal, $routeParams, $popover, appResourceRoot, TaskService, CaseService, CommentService, RelatedContentService, MilestoneService, StageService, UserEventListenerService, PlanItemInstanceService) {

            $rootScope.root.showStartForm = false;

            $scope.model = {
                // Indirect binding between selected task in parent scope to have control over display
                // before actual selected task is switched
                caseInstance: $scope.selectedCaseInstance
            };


            $scope.model.contentSummary = {
                loading: false
            };

            $scope.$watch('selectedCaseInstance', function (newValue) {
                if (newValue && newValue.id) {
                    $scope.model.caseUpdating = true;
                    $scope.model.caseInstance = newValue;

                    $scope.getCaseInstance(newValue.id);
                }
            });

            $scope.getCaseInstance = function (caseInstanceId) {
                $http({method: 'GET', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId}).success(function (response, status, headers, config) {
                    $scope.model.caseInstance = response;
                    $scope.loadCaseTasks();
                    $scope.loadRelatedContent();
                    $scope.loadCaseInstanceStages();
                    $scope.loadUserEventListeners();
                    $scope.loadCaseInstanceMilestones();
                    $scope.loadEnabledPlanItemInstances();
                }).error(function (response, status, headers, config) {
                    console.log('Something went wrong: ' + response);
                });
            };

            $scope.loadCaseTasks = function () {

                // Runtime tasks
                TaskService.getCaseInstanceTasks($scope.model.caseInstance.id, false).then(function (response) {
                    $scope.model.caseTasks = response.data;
                });

                TaskService.getCaseInstanceTasks($scope.model.caseInstance.id, true).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.completedCaseTasks = response.data;
                    } else {
                        $scope.model.completedCaseTasks = [];
                    }

                    // Calculate duration
                    for (var i = 0; i < response.data.length; i++) {
                        var task = response.data[i];
                        if (task.duration) {
                            task.duration = moment.duration(task.duration).humanize();
                        }
                    }
                });
            };

            $scope.loadCaseInstanceStages = function () {

                $scope.model.caseHasStages = false;

                StageService.getCaseInstanceActiveStages($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseActiveStages = response.data;
                        $scope.model.caseHasStages = true;
                    } else {
                        $scope.model.caseActiveStages = [];
                    }
                });

                StageService.getCaseInstanceEndedStages($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseEndedStages = response.data;
                        $scope.model.caseHasStages = true;
                    } else {
                        $scope.model.caseEndedStages = [];
                    }
                });
            };

            $scope.loadCaseInstanceMilestones = function () {

                $scope.model.caseHasMilestones = false;

                MilestoneService.getCaseInstanceAvailableMilestones($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseAvailableMilestones = response.data;
                        $scope.model.caseHasMilestones = true;
                    } else {
                        $scope.model.caseAvailableMilestones = [];
                    }
                });

                MilestoneService.getCaseInstanceEndedMilestones($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseEndedMilestones = response.data;
                        $scope.model.caseHasMilestones = true;
                    } else {
                        $scope.model.caseEndedMilestones = [];
                    }
                });
            };

            $scope.loadUserEventListeners = function () {

                $scope.model.hasUserEventListeners = false;

                UserEventListenerService.getCaseInstanceAvailableUserEventListeners($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseAvailableUserEventListeners = response.data;
                        $scope.model.hasUserEventListeners = true;
                    } else {
                        $scope.model.caseAvailableUserEventListeners = [];
                    }
                });

                UserEventListenerService.getCaseInstanceCompletedUserEventListeners($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseCompletedUserEventListeners = response.data;
                        $scope.model.hasUserEventListeners = true;
                    } else {
                        $scope.model.caseCompletedUserEventListeners = [];
                    }
                });
            };

            $scope.loadEnabledPlanItemInstances = function() {

                $scope.model.hasEnabledPlanItemInstance = false;

                PlanItemInstanceService.getCaseInstanceEnabledPlanItemInstances($scope.model.caseInstance.id).then(function (response) {
                    if (response.data && response.data.length > 0) {
                        $scope.model.caseEnabledPlanItemInstances = response.data;
                        $scope.model.hasEnabledPlanItemInstance = true;
                    } else {
                        $scope.model.caseEnabledPlanItemInstances = [];
                    }
                });

            };

            $scope.toggleCreateContent = function () {
                $scope.model.contentSummary.addContent = !$scope.model.contentSummary.addContent;
            };

            $scope.onContentUploaded = function (content) {
                if ($scope.model.content && $scope.model.content.data) {
                    $scope.model.content.data.push(content);
                    RelatedContentService.addUrlToContent(content);
                    $scope.model.selectedContent = content;
                }
                $rootScope.addAlertPromise($translate('TASK.ALERT.RELATED-CONTENT-ADDED', content), 'info');
                $scope.toggleCreateContent();
            };

            $scope.onContentDeleted = function (content) {
                if ($scope.model.content && $scope.model.content.data) {
                    $scope.model.content.data.forEach(function (value, i, arr) {
                        if (content === value) {
                            arr.splice(i, 1);
                        }
                    })
                }
            };

            $scope.selectContent = function (content) {
                if ($scope.model.selectedContent == content) {
                    $scope.model.selectedContent = undefined;
                } else {
                    $scope.model.selectedContent = content;
                }
            };

            $scope.loadRelatedContent = function () {
                $scope.model.content = undefined;
                CaseService.getRelatedContent($scope.model.caseInstance.id).then(function (data) {
                    $scope.model.content = data;
                });
            };

            $scope.$watch("model.content", function (newValue) {
                if (newValue && newValue.data && newValue.data.length > 0) {
                    var needsRefresh = false;
                    for (var i = 0; i < newValue.data.length; i++) {
                        var entry = newValue.data[i];
                        if (!entry.contentAvailable) {
                            needsRefresh = true;
                            break;
                        }
                    }
                }
            }, true);

            $scope.cancelCase = function (final) {
                if ($scope.model.caseInstance) {
                    var modalInstance = _internalCreateModal({
                        template: appResourceRoot + 'views/modal/case-cancel.html',
                        scope: $scope,
                        show: true
                    }, $modal, $scope);

                    if (final) {
                        modalInstance.$scope.finalDelete = true;
                    }
                }
            };

            $scope.deleteCase = function () {
                $scope.cancelCase(true);
            };

            $scope.generateTest = function() {
                alert('Case test model successfuly created');
            }


            $scope.$on('caseinstance-deleted', function (event, data) {
                $route.reload();
            });

            $scope.$on('user-event-listener-triggered', function (event, data) {
                $route.reload();
            });

            $scope.openTask = function (task) {
                $rootScope.root.selectedTaskId = task.id;
                var path = '';
                if ($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
                    path = "/apps/" + $rootScope.activeAppDefinition.id;
                }
                $location.path(path + "/tasks");
            };

            $scope.triggerUserEventListener = function (userEventListener) {
                UserEventListenerService.triggerCaseInstanceUserEventListener($scope.model.caseInstance.id, userEventListener.id);
            };

            $scope.startPlanItemInstance = function (planItemInstance) {
                PlanItemInstanceService.startPlanItemInstance(planItemInstance.caseInstanceId, planItemInstance.id);
                $route.reload();
            };

            $scope.openStartForm = function () {
                $rootScope.root.showStartForm = true;
                $rootScope.root.selectedCaseId = $scope.model.caseInstance.id;
            };

            $scope.popupShown = function () {

            };

            $scope.closeDiagramPopup = function () {
                jQuery('.qtip').qtip('destroy', true);
            };
            
            $scope.showDiagram = function() {
                var modalInstance = _internalCreateModal({
                    template: appResourceRoot + 'views/modal/case-instance-graphical.html',
                    scope: $scope,
                    show: true
                }, $modal, $scope);
            };
    }]);

angular.module('flowableApp')
    .controller('ShowCaseDiagramCtrl', ['$scope', '$http', '$timeout', '$q', 'ResourceService', 'appResourceRoot',
        function ($scope, $http, $timeout, $q, ResourceService, appResourceRoot) {
            $scope.model.isDebuggerEnabled = false;
            $scope.model.scriptLanguage = 'groovy';

            $scope.model.variables = [];
            $scope.model.displayVariables = true;

            $http({
                method: 'GET',
                url: '../app/rest/cmmn-debugger/',
                async: false
            }).success(function (data) {
                $scope.model.isDebuggerEnabled = data;
                $scope.loadVariables();
                $scope.getPlanItems();
                $scope.getEventLog();
            });

            $scope.model.planItems = undefined;
            $scope.model.selectedPlanItemId = undefined;

            $scope.model.errorMessage = '';

            $scope.tabData = {
                tabs: [
                    {id: 'variables', name: 'CASE.TITLE.VARIABLES'},
                    {id: 'planItems', name: 'CASE.TITLE.PLANITEMS'},
                    {id: 'log', name: 'CASE.TITLE.LOG'}
                ],
                activeTab: 'variables'
            };

            if (!$scope.model.caseInstance.ended) {
                $scope.tabData.tabs.push(
                  {id: 'expression', name: 'CASE.TITLE.EXPRESSION'}
                );
                $scope.tabData.tabs.push(
                  {id: 'script', name: 'CASE.TITLE.SCRIPT'}
                );
            }

            // config for plan items grid
            $scope.gridPlanItems = {
                data: $scope.model.planItems,
                columnDefs: [
                    {field: 'id', displayName: "Id", name: 'id', maxWidth: 15},
                    {field: 'caseInstanceId', displayName: "Case instance id", name: 'caseInstanceId', maxWidth: 15},
                    {field: 'stageInstanceId',displayName: "Stage instance id",name: 'stageInstanceId',maxWidth: 80},
                    {field: 'name',displayName: "Name",name: 'name',maxWidth: 80},
                    {field: 'state',displayName: "State",name: 'state',maxWidth: 80},
                    //{field: 'elementId',displayName: "Element key",name: 'elementId',maxWidth: 80},
                    {field: 'completeable', displayName: "Completeable", name: 'completeable', maxWidth: 30},
                    {field: 'tenantId', displayName: "Tenant id", name: 'tenantId', maxWidth: 80}
                ],
                enableRowSelection: true,
                multiSelect: false,
                noUnselect: false,
                enableRowHeaderSelection: false,
                onRegisterApi: function (gridApi) {
                    $scope.gridPlanItemsApi = gridApi;
                    $scope.gridPlanItemsApi.grid.modifyRows($scope.gridPlanItems.data);
                    if ($scope.gridPlanItems.data) {
                        $scope.selectRowForSelectedPlanItem();
                    }
                    $scope.gridPlanItemsApi.selection.on.rowSelectionChanged($scope, function (row) {
                        if(row.isSelected) {
                            var elementToUnselect = modelDiv.attr("selected-element");
                            if (elementToUnselect) {
                                var shapeToUnselect = paper.getById(elementToUnselect);
                                if (shapeToUnselect) {
                                    shapeToUnselect.attr({"stroke": "green"});
                                }
                            }
                            var elementId = row.entity.elementId;
                            modelDiv.attr("selected-element", elementId);
                            $scope.model.selectedPlanItem = row.entity.id;

                            var elementToSelect = paper.getById(elementId);
                            var selectableRow = row.entity.state === 'active' || row.entity.state === 'enabled';// || row.entity.state === 'suspended';
                            if (elementToSelect && selectableRow) {
                                elementToSelect.attr({"stroke": "red"});
                            } else {
                                row.setSelected(false);
                            }
                        } else {
                            $scope.model.selectedPlanItem = undefined;
                        }
                    });
                }
            };

            // Config for variable grid
            $scope.gridVariables = {
                data: $scope.model.variables,
                columnDefs: [
                    {field: 'scopeId', displayName: "Scope", maxWidth: 10},
                    {field: 'subScopeId', displayName: "Subscope", maxWidth: 10},
                    {field: 'type', displayName: "Type", maxWidth: 10},
                    {field: 'name', displayName: "Name", maxWidth: 10},
                    {
                        field: 'value', displayName: "Value",
                        cellTemplate: '<div><div style="text-align: left" class="ngCellText ui-grid-cell-contents">{{grid.getCellValue(row, col)}}</div></div>'
                    }
                ],
                onRegisterApi: function (gridApi) {
                    $scope.gridVariablesApi = gridApi;
                }
            };

            // Config for log grid
            $scope.gridLog = {
                columnDefs: [
                    {field: 'id', displayName: "Id", maxWidth: 10},
                    {field: 'type', displayName: "Type", maxWidth: 10},
                    {field: 'timeStamp', displayName: "Time Stamp", maxWidth: 90},
                    {field: 'executionId', displayName: "PlanItemId", maxWidth: 90},//TODO: change field name
                ],
                enableRowSelection: true,
                multiSelect: false,
                noUnselect: true,
                enableRowHeaderSelection: false,
                onRegisterApi: function (gridApi) {
                    $scope.gridLogApi = gridApi;
                }
            };

            $scope.getPlanItems = function () {
                if ($scope.model.isDebuggerEnabled) {
                    $http({
                        method: 'GET',
                        url: '../app/rest/cmmn-debugger/planItems/' + $scope.model.caseInstance.id
                    }).success(function (data) {
                        $scope.model.planItems = data;
                        $scope.gridPlanItems.data = data;
                        if ($scope.gridPlanItemsApi) {
                            $scope.gridPlanItemsApi.grid.modifyRows($scope.gridPlanItems.data);
                            $scope.selectRowForSelectedPlanItem();
                        }
                        jQuery("#cmmnModel").data($scope.model.planItems);
                    }).error(function (data, status, headers, config) {
                        $scope.model.errorMessage = data;
                    });
                }
            };

            $scope.selectRowForSelectedPlanItem = function() {
                if ($scope.model.isDebuggerEnabled && $scope.gridPlanItems.data && $scope.gridPlanItemsApi) {
                    for (var i = 0; i < $scope.gridPlanItems.data.length; i++) {
                        if ($scope.model.selectedPlanItemId === $scope.gridPlanItems.data[i].id) {
                            $scope.gridPlanItemsApi.selection.selectRow($scope.gridPlanItems.data[i]);
                            i = $scope.gridPlanItems.data.length;
                        }
                    }
                }
            };

            $scope.loadVariables = function () {
                if ($scope.model.isDebuggerEnabled) {
                    $http({
                        method: 'GET',
                        url: '../app/rest/cmmn-debugger/variables/' + $scope.model.caseInstance.id
                    }).success(function (data, status, headers, config) {
                        $scope.model.variables = data;
                        $scope.gridVariables.data = data;
                        if ($scope.gridVariablesApi) {
                            $scope.gridVariablesApi.core.refresh();
                        }
                    });
                }
            };

            $scope.getEventLog = function () {
                if ($scope.model.isDebuggerEnabled) {
                    $http({
                        method: 'GET',
                        url: '../app/rest/cmmn-debugger/eventlog/' + $scope.model.caseInstance.id
                    }).success(function (data) {
                        $scope.gridLog.data = data;
                        if ($scope.gridLogApi) {
                            $scope.gridLogApi.core.refresh();
                        }
                    });
                }
            };

            $scope.evaluateExpression = function () {
                if ($scope.model.isDebuggerEnabled) {
                    $scope.model.errorMessage = '';
                    $scope.model.result = '';

                    var planItem = $scope.model.selectedPlanItemId;
                    $http({
                        method: 'POST',
                        url: '../app/rest/cmmn-debugger/evaluate/expression/' + planItem,
                        data: $scope.model.expression
                    }).success(function (data) {
                        $scope.model.result = data;
                    }).error(function (data, status, headers, config) {
                        $rootScope.addAlert("Execution evaluation failed :" + data, 'error');
                    });
                }
            }

            $scope.evaluateScript = function () {
                if ($scope.model.isDebuggerEnabled) {
                    $scope.model.errorMessage = '';

                    var planItem = $scope.model.selectedPlanItemId;
                    $http({
                        method: 'POST',
                        url: '../app/rest/cmmn-debugger/evaluate/' + $scope.model.scriptLanguage + '/' + planItem,
                        data: $scope.model.scriptText
                    }).success(function (data) {
                        $rootScope.addAlert("script executed", 'info')
                    }).error(function (data, status, headers, config) {
                        $rootScope.addAlert(data, 'error');
                    });
                }
            };

            $timeout(function () {
                jQuery("#cmmnModel").attr('data-model-id', $scope.model.caseInstance.id);
                jQuery("#cmmnModel").attr('data-model-type', 'runtime');

                // in case we want to show a historic model, include additional attribute on the div
                if ($scope.model.caseInstance.ended) {
                    jQuery("#cmmnModel").attr('data-history-id', $scope.model.caseInstance.id);
                }

                var viewerUrl = appResourceRoot + "../display-cmmn/displaymodel.html?version=" + Date.now();

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
    .controller('CancelCaseCtrl', ['$scope', '$http', '$route', 'CaseService', function ($scope, $http, $route, CaseService) {

        $scope.popup = {loading: false};

        $scope.ok = function () {
            $scope.popup.loading = true;

            CaseService.deleteCase($scope.model.caseInstance.id).then(function (response, status, headers, config) {
                $scope.$hide();
            }).finally(function (response, status, headers, config) {
                $scope.popup.loading = false;
            })
        };

        $scope.cancel = function () {
            $scope.$hide();
        }
    }
    ]);
