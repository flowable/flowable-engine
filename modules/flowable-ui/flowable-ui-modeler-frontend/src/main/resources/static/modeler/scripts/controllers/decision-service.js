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
    .controller('DecisionServiceDetailsCtrl', ['$rootScope', '$scope', '$translate', '$http', '$location', '$routeParams', '$modal', '$timeout', '$popover', 'ResourceService',
        function ($rootScope, $scope, $translate, $http, $location, $routeParams, $modal, $timeout, $popover, ResourceService) {

            // Main page (needed for visual indicator of current page)
            $rootScope.setMainPageById('decisions');

            // Initialize model
            $scope.model = {
                // Store the main model id, this points to the current version of a model,
                // even when we're showing history
                latestModelId: $routeParams.modelId
            };

            $scope.loadDecisionService = function () {
                var url;
                if ($routeParams.modelHistoryId) {
                    url = FLOWABLE.APP_URL.getModelUrl($routeParams.modelId) + '/history/' + $routeParams.modelHistoryId;
                } else {
                    url = FLOWABLE.APP_URL.getModelUrl($routeParams.modelId);
                }

                $http({method: 'GET', url: url}).success(function (data, status, headers, config) {
                    $scope.model.decisionService = data;

                    $scope.loadVersions();

                    $scope.model.decisionDownloadUrl = FLOWABLE.APP_URL.getDmnModelDownloadUrl($routeParams.modelId, $routeParams.modelHistoryId);

                    $rootScope.$on('$routeChangeStart', function (event, next, current) {
                        jQuery('.qtip').qtip('destroy', true);
                    });

                    $timeout(function () {
                        jQuery("#dmnModel").attr('data-model-id', $routeParams.modelId);
                        jQuery("#dmnModel").attr('data-model-type', 'design');

                        // in case we want to show a historic model, include additional attribute on the div
                        if (!$scope.model.decisionService.latestVersion) {
                            jQuery("#dmnModel").attr('data-history-id', $routeParams.modelHistoryId);
                        }

                        var viewerUrl = "display-dmn/displaymodel.html?version=" + Date.now();

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
                    });

                }).error(function (data, status, headers, config) {
                    $scope.returnToList();
                });
            };

            $scope.useAsNewVersion = function () {
                _internalCreateModal({
                    template: 'views/popup/model-use-as-new-version.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.loadVersions = function () {

                var params = {
                    includeLatestVersion: !$scope.model.decisionService.latestVersion
                };

                $http({
                    method: 'GET',
                    url: FLOWABLE.APP_URL.getModelHistoriesUrl($scope.model.latestModelId),
                    params: params
                }).success(function (data, status, headers, config) {
                    if ($scope.model.decisionService.latestVersion) {
                        if (!data.data) {
                            data.data = [];
                        }
                        data.data.unshift($scope.model.decisionService);
                    }

                    $scope.model.versions = data;
                });
            };

            $scope.showVersion = function (version) {
                if (version) {
                    if (version.latestVersion) {
                        $location.path("/decision-services/" + $scope.model.latestModelId);
                    } else {
                        // Show latest version, no history-suffix needed in URL
                        $location.path("/decision-services/" + $scope.model.latestModelId + "/history/" + version.id);
                    }
                }
            };

            $scope.returnToList = function () {
                $location.path("/decision-services/");
            };

            $scope.editDecisionService = function () {
                _internalCreateModal({
                    template: 'views/popup/model-edit.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.duplicateDecisionService = function () {
                var modalInstance = _internalCreateModal({
                    template: 'views/popup/decision-service-duplicate.html?version=' + Date.now()
                }, $modal, $scope);

                modalInstance.$scope.originalModel = $scope.model;
            };

            $scope.deleteDecisionService = function () {
                _internalCreateModal({
                    template: 'views/popup/model-delete.html',
                    scope: $scope
                }, $modal, $scope);
            };

            $scope.openEditor = function () {
                if ($scope.model.decisionService) {
                    $location.path("/decision-service-editor/" + $scope.model.decisionService.id);
                }
            };

            $scope.toggleHistory = function ($event) {
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

                    var destroy = function () {
                        state.popover.destroy();
                        delete $scope.historyState;
                    }

                    // When popup is hidden or scope is destroyed, hide popup
                    state.popover.$scope.$on('tooltip.hide', destroy);
                    $scope.$on('$destroy', destroy);
                }
            };

            $scope.loadDecisionService();
        }]);
