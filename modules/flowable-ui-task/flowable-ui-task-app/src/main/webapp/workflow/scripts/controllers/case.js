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

      $scope.selectedCaseInstance = { id: $routeParams.caseId };

      $scope.deploymentKey = $routeParams.deploymentKey;

      $scope.$on('caseinstance-deleted', function (event, data) {
        $scope.openCases();
      });

      $scope.openCases = function(task) {
        var path='';
        if($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
            path = "/apps/" + $rootScope.activeAppDefinition.id;
        }
        $location.path(path + "/cases");
      };
}]);

angular.module('flowableApp')
    .controller('CaseDetailController', ['$rootScope', '$scope', '$translate', '$http', '$timeout','$location', '$route', '$modal', '$routeParams', '$popover', 'appResourceRoot', 'TaskService', 'CommentService', 'RelatedContentService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $route, $modal, $routeParams, $popover, appResourceRoot, TaskService, CommentService, RelatedContentService) {

    $rootScope.root.showStartForm = false;

    $scope.model = {
        // Indirect binding between selected task in parent scope to have control over display
        // before actual selected task is switched
        caseInstance: $scope.selectedCaseInstance
    };

    $scope.$watch('selectedCaseInstance', function(newValue) {
        if (newValue && newValue.id) {
            $scope.model.caseUpdating = true;
            $scope.model.caseInstance = newValue;

            $scope.getCaseInstance(newValue.id);
        }
    });

    $scope.getCaseInstance = function(caseInstanceId) {
        $http({method: 'GET', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId}).
            success(function(response, status, headers, config) {
                $scope.model.caseInstance = response;
                $scope.loadCaseTasks();
            }).
            error(function(response, status, headers, config) {
                console.log('Something went wrong: ' + response);
            });
    };

    $rootScope.loadCaseTasks = function() {

        // Runtime tasks
        TaskService.getCaseInstanceTasks($scope.model.caseInstance.id, false).then(function(response) {
            $scope.model.caseTasks = response.data;
        });

        TaskService.getCaseInstanceTasks($scope.model.caseInstance.id, true).then(function(response) {
            if(response.data && response.data.length > 0) {
                $scope.model.completedCaseTasks = response.data;
            } else {
                $scope.model.completedCaseTasks = [];
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

    $scope.cancelCase = function(final) {
        if ($scope.model.caseInstance) {
            var modalInstance = _internalCreateModal({
                template: appResourceRoot + 'views/modal/case-cancel.html',
                scope: $scope,
                show: true
            }, $modal, $scope);

            if(final) {
                modalInstance.$scope.finalDelete = true;
            }
        }
    };

    $scope.deleteCase = function() {
        $scope.cancelCase(true);
    };

    $scope.$on('caseinstance-deleted', function (event, data) {
        $route.reload();
    });

    $scope.openTask = function(task) {
        $rootScope.root.selectedTaskId = task.id;
        var path='';
        if ($rootScope.activeAppDefinition && !FLOWABLE.CONFIG.integrationProfile) {
            path = "/apps/" + $rootScope.activeAppDefinition.id;
        }
        $location.path(path + "/tasks");
    };

    $scope.openStartForm = function() {
        $rootScope.root.showStartForm = true;
        $rootScope.root.selectedCaseId = $scope.model.caseInstance.id;
    };

    $scope.popupShown = function() {

    };

    $scope.closeDiagramPopup = function() {
        jQuery('.qtip').qtip('destroy', true);
    };
}]);

angular.module('flowableApp')
.controller('CancelCaseCtrl', ['$scope', '$http', '$route', 'CaseService', function ($scope, $http, $route, CaseService) {

        $scope.popup = {loading: false};

        $scope.ok = function() {
            $scope.popup.loading = true;

            CaseService.deleteCase($scope.model.caseInstance.id).
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
