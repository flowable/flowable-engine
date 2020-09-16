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

angular.module('flowableModeler').controller('FlowableDecisionServiceReferenceCtrl',
    [ '$scope', '$modal', '$http', function($scope, $modal, $http) {

     // Config for the modal window
     var opts = {
         template:  'editor-app/configuration/properties/decisionservice-reference-popup.html?version=' + Date.now(),
         scope: $scope
     };

     // Open the dialog
     _internalCreateModal(opts, $modal, $scope);
}]);

angular.module('flowableModeler').controller('FlowableDecisionServiceReferencePopupCtrl', ['$rootScope', '$scope', '$http', '$location', 'editorManager',
    function($rootScope, $scope, $http, $location, editorManager) {

        $scope.state = {
            'loadingDecisionServices': true,
            'decisionServiceError': false
        };

        $scope.popup = {
            'state': 'decisionServiceReference'
        };

        $scope.foldersBreadCrumbs = [];

        // Make click outside dialog also call close.
        $scope.$parent.$on('modal.hide.before', function() {
            $scope.close();
            $scope.$parent.$apply();
        });

        // Close button handler
        $scope.close = function() {
            $scope.property.newVariablesMapping = undefined;
            $scope.property.mode = 'read';
            $scope.$hide();
        };

        // Selecting/deselecting a decision service
        $scope.selectDecisionService = function(decisionService, $event) {
            $event.stopPropagation();
            if ($scope.selectedDecisionService && $scope.selectedDecisionService.id && decisionService.id == $scope.selectedDecisionService.id) {
                // un-select the current selection
                $scope.selectedDecisionService = null;
            } else {
                $scope.selectedDecisionService = decisionService;
            }
        };

        $scope.isSelected = function () {
            if ($scope.selectedDecisionService && $scope.selectedDecisionService.id) {
                return true;
            }
            return false;
        };

        // Saving the selected value
        $scope.save = function() {
            if ($scope.selectedDecisionService) {
                $scope.property.value = {
                    'id': $scope.selectedDecisionService.id,
                    'name': $scope.selectedDecisionService.name,
                    'key': $scope.selectedDecisionService.key
                };

            } else {
                $scope.property.value = null;
            }
            $scope.updatePropertyInModel($scope.property);
            $scope.close();
        };

        // Open the selected value
        $scope.open = function() {
            if ($scope.selectedDecisionService) {
                $scope.property.value = {
                    'id': $scope.selectedDecisionService.id,
                    'name': $scope.selectedDecisionService.name,
                    'key': $scope.selectedDecisionService.key
                };
                $scope.updatePropertyInModel($scope.property);

                var modelMetaData = editorManager.getBaseModelData();
                var json = editorManager.getModel();
                json = JSON.stringify(json);

                var params = {
                    modeltype: modelMetaData.model.modelType,
                    json_xml: json,
                    name: modelMetaData.name,
                    key: modelMetaData.key,
                    description: modelMetaData.description,
                    newversion: false,
                    lastUpdated: modelMetaData.lastUpdated
                };

                // Update
                $http({
                    method: 'POST',
                    data: params,
                    ignoreErrors: true,
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                    },
                    transformRequest: function (obj) {
	                    var str = [];
	                    for (var p in obj) {
	                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
	                    }
	                    return str.join("&");
	                },
                    url: FLOWABLE.URL.putModel(modelMetaData.modelId)
                })

                .success(function(data, status, headers, config) {
                        editorManager.handleEvents({
                            type: ORYX.CONFIG.EVENT_SAVED
                        });

						$rootScope.addHistoryItem($scope.selectedShape.resourceId);
						$location.path('decision-service-editor/' + $scope.selectedDecisionService.id);
                    })
                    .error(function(data, status, headers, config) {

                    });

                $scope.close();
            }
        };

        $scope.newDecisionService = function() {
            $scope.property.value.variablesmapping = [];

            $scope.popup.state = 'newDecisionService';

            var modelMetaData = editorManager.getBaseModelData();

            $scope.model = {
                loading: false,
                decisionService: {
                    name: '',
                    key: '',
                    description: '',
                    modelType: 6
                },
                defaultStencilSet: undefined,
                decisionTableStencilSets: []
            };
        };

        $scope.createDecisionService = function() {

            if (!$scope.model.decisionService.name || $scope.model.decisionService.name.length == 0 ||
            	!$scope.model.decisionService.key || $scope.model.decisionService.key.length == 0) {

                return;
            }

            var stencilSetId = $scope.model.decisionService.stencilSet;
            $scope.model.loading = true;

            $http({
                method: 'POST',
                url: FLOWABLE.APP_URL.getModelsUrl(),
                data: $scope.model.decisionService
            }).
            success(function(data, status, headers, config) {

                var newDecisionTableId = data.id;
                $scope.property.value = {
                    'id': newDecisionTableId,
                    'name': data.name,
                    'key': data.key
                };
                $scope.updatePropertyInModel($scope.property);

                var modelMetaData = editorManager.getBaseModelData();
                var json = editorManager.getModel();
                json = JSON.stringify(json);

                var params = {
                    modeltype: modelMetaData.model.modelType,
                    json_xml: json,
                    name: modelMetaData.name,
                    key: modelMetaData.key,
                    description: modelMetaData.description,
                    newversion: false,
                    lastUpdated: modelMetaData.lastUpdated,
                    stencilSet: stencilSetId
                };

                // Update
                $http({
                    method: 'POST',
                    data: params,
                    ignoreErrors: true,
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                    },
                    transformRequest: function (obj) {
	                    var str = [];
	                    for (var p in obj) {
	                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
	                    }
	                    return str.join("&");
	                },
                    url: FLOWABLE.URL.putModel(modelMetaData.modelId)
                })

                .success(function(data, status, headers, config) {
                        editorManager.handleEvents({
                            type: ORYX.CONFIG.EVENT_SAVED
                        });

                        $scope.model.loading = false;
                        $scope.$hide();

                        $rootScope.addHistoryItem($scope.selectedShape.resourceId);
                        $location.path('decision-service-editor/' + newDecisionTableId);
                    })
                    .error(function(data, status, headers, config) {
                        $scope.model.loading = false;
                        $scope.$hide();
                    });

            }).
            error(function(data, status, headers, config) {
                $scope.model.loading = false;
                $scope.model.errorMessage = data.message;
            });
        };

        $scope.cancel = function() {
            $scope.close();
        };

        $scope.resetCurrent = function () {
            for (var i = 0, found = false; i < $scope.decisionServices.length && found === false; i++) {
                var decision = $scope.decisionServices[i];
                if (decision.id === $scope.property.value.id) {
                    $scope.selectedDecisionService = decision;
                    found = true;
                }
            }
        };

        $scope.loadDecisionServices = function() {
            var modelMetaData = editorManager.getBaseModelData();
            $http.get(FLOWABLE.APP_URL.getDecisionServiceModelsUrl())
                .success(
                    function(response) {
                            $scope.state.loadingDecisionServices = false;
                            $scope.state.decisionServiceError = false;
                            $scope.decisionServices = response.data;
                            $scope.resetCurrent();
                })
                .error(
                    function(data, status, headers, config) {
                    $scope.state.loadingDecisionServices = false;
                    $scope.state.decisionServiceError = true;
                });
        };

        if ($scope.property && $scope.property.value && $scope.property.value.id) {
            $scope.selectedDecisionService = $scope.property.value;
            $scope.storedId = $scope.property.value.id;
        }

        $scope.loadDecisionServices();
    }
]);
