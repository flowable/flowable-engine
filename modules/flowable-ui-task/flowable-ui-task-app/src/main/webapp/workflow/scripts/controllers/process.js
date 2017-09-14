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

        $scope.deploymentKey = $routeParams.deploymentKey;

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

    $scope.showCreateProcessTestDialog = function() {
        $scope.model.skeleton = $scope.model.processInstance.id;
        var modalInstance = _internalCreateModal({
            template: appResourceRoot + 'views/modal/process-test-create.html',
            scope: $scope,
            show: true
        }, $modal, $scope);

    };
}]);

angular.module('flowableApp')
    .controller('ShowProcessDiagramCtrl', ['$scope', '$http', '$timeout', 'ResourceService', 'appResourceRoot',
        function ($scope, $http, $timeout, ResourceService, appResourceRoot) {

            $scope.model.expression = '';

            $timeout(function() {
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
                ResourceService.loadFromHtml(viewerUrl, function(){
                    // Restore AMD's define method again
                    window.define = amdDefine;
                });
            }, 100);

            $scope.evaluateExpression = function() {
                $scope.model.errorMessage='';

                $http({
                    method: 'POST',
                    url: '../app/rest/debugger/evaluate/expression/'+ $scope.model.processInstance.id,
                    data: $scope.model.expression
                }).success(function (data) {
                    $scope.model.result = data;
                }).error(function (data, status, headers, config) {
                    $scope.model.errorMessage = data;
                });
            }

            $scope.evaluateScript = function () {
                $scope.model.errorMessage = '';
                $scope.model.result='';

                $http({
                    method: 'POST',
                    url: '../app/rest/debugger/evaluate/script/' + $scope.model.processInstance.id,
                    data: $scope.model.script
                }).success(function (data) {
                    $scope.model.result = "OK";
                }).error(function (data, status, headers, config) {
                    $scope.model.errorMessage = data;
                });
            }

            $scope.logExpression = function() {
                var eventLogRequest = {
                    type : 'expressionDebuggerLog',
                    processDefinitionId : $scope.model.processInstance.processDefinitionId,
                    processInstanceId: $scope.model.processInstance.id,
                    executionId : $scope.model.processInstance.executionId,
                    data : {
                        expression : $scope.model.expression
                    }
                };
                $scope.model.errorMessage = '';
                $scope.model.result = '';

                $http({
                    method: 'PUT',
                    url: '../app/rest/debugger/event-log',
                    data: eventLogRequest
                }).success(function (data) {
                    $scope.model.result = data;
                }).error(function (data, status, headers, config) {
                    $scope.model.errorMessage = data;
                });
            }

            $scope.logScript = function() {
                var eventLogRequest = {
                    type : 'DEBUG_LOG_SCRIPT',
                    processDefinitionId : $scope.model.processInstance.processDefinitionId,
                    processInstanceId: $scope.model.processInstance.id,
                    executionId : $scope.model.processInstance.executionId,
                    data : {
                        script : $scope.model.script
                    }
                };
                $scope.model.errorMessage = '';
                $scope.model.result = '';

                $http({
                    method: 'PUT',
                    url: '../app/rest/debugger/event-log',
                    data: eventLogRequest
                }).success(function (data) {
                    $scope.model.result = data;
                }).error(function (data, status, headers, config) {
                    $scope.model.errorMessage = data;
                });
            }
        }
    ]
);

angular.module('flowableApp')
.controller('CreateNewProcessTestModelCtrl', ['$rootScope', '$scope', '$modal', '$http', '$location',
    function ($rootScope, $scope, $modal, $http, $location) {

        $scope.model = {
            loading: false,
            skeleton: $scope.model.processInstance.id,
            process: {
                name: 'processTest',
                key: 'processTest',
                description: '',
                modelType: 0,
            }
        };

        if ($scope.initialModelType !== undefined) {
            $scope.model.process.modelType = $scope.initialModelType;
        }

        $scope.ok = function () {

            if (!$scope.model.process.name || $scope.model.process.name.length == 0 ||
                !$scope.model.process.key || $scope.model.process.key.length == 0) {

                return;
            }

            $scope.model.loading = true;

            $http({
                method: 'POST',
                url: '../app/rest/models?skeleton=' + $scope.model.skeleton,
                data: $scope.model.process
            }).success(function (data) {
                $scope.$hide();

                $scope.model.loading = false;
                alert('Model successfuly created');
            }).error(function (data, status, headers, config) {
                $scope.model.loading = false;
                $scope.model.errorMessage = 'Test create has failed';
            });
        };

        $scope.cancel = function () {
            if (!$scope.model.loading) {
                $scope.$hide();
            }
        };
    }]);

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
