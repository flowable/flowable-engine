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
    .controller('ShowCaseDiagramCtrl', ['$scope', '$timeout', '$q', 'ResourceService', 'appResourceRoot',
        function ($scope, $timeout, $q, ResourceService, appResourceRoot) {

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
