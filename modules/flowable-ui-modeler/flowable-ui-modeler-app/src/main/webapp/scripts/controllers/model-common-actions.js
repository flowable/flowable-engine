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

angular.module('flowableModeler')
.controller('EditModelPopupCtrl', ['$rootScope', '$scope', '$http', '$translate', '$location',
    function ($rootScope, $scope, $http, $translate, $location) {

        var model;
        var popupType;
        if ($scope.model.process) {
            model = $scope.model.process;
            popupType = 'PROCESS';
        } else if ($scope.model.caseModel) {
            model = $scope.model.caseModel;
            popupType = 'CASE';
        } else if ($scope.model.form) {
            model = $scope.model.form;
            popupType = 'FORM';
        } else if ($scope.model.decisionTable) {
            model = $scope.model.decisionTable;
            popupType = 'DECISION-TABLE';
        } else {
            model = $scope.model.app;
            popupType = 'APP';
        }

    	$scope.popup = {
    		loading: false,
    		popupType: popupType,
        	modelName: model.name,
        	modelKey: model.key,
        	modelDescription: model.description,
    		id: model.id
    	};

    	$scope.ok = function () {

    		if (!$scope.popup.modelName || $scope.popup.modelName.length == 0 ||
    			!$scope.popup.modelKey || $scope.popup.modelKey.length == 0) {
    			
    			return;
    		}

        	$scope.model.name = $scope.popup.modelName;
        	$scope.model.key = $scope.popup.modelKey;
        	$scope.model.description = $scope.popup.modelDescription;

    		$scope.popup.loading = true;
    		var updateData = {
    			name: $scope.model.name, 
    			key: $scope.model.key, description: 
    			$scope.model.description
    		};

    		$http({method: 'PUT', url: FLOWABLE.APP_URL.getModelUrl($scope.popup.id), data: updateData}).
    			success(function(data, status, headers, config) {
    				if ($scope.model.process) {
    					$scope.model.process = data;
    				} else if ($scope.model.caseModel) {
                        $scope.model.caseModel = data;
    				} else if ($scope.model.form) {
    					$scope.model.form = data;
    				} else if ($scope.model.decisionTable) {
    					$scope.model.decisionTable = data;
    				} else {
    					$scope.model.app = data;
    				}

    				$scope.addAlertPromise($translate('PROCESS.ALERT.EDIT-CONFIRM'), 'info');
    				$scope.$hide();
    				$scope.popup.loading = false;

    				if (popupType === 'FORM') {
                        $location.path("/forms/" +  $scope.popup.id);
                    } else if (popupType === 'APP') {
                        $location.path("/apps/" +  $scope.popup.id);
                    } else if (popupType === 'DECISION-TABLE') {
                        $location.path("/decision-tables/" +  $scope.popup.id);
                    } else if (popupType === 'CASE') {
                        $location.path("/casemodels/" +  $scope.popup.id);
                    } else {
                        $location.path("/processes/" +  $scope.popup.id);
                    }

    			}).
    			error(function(data, status, headers, config) {
    				$scope.popup.loading = false;
    				$scope.popup.errorMessage = data.message;
    			});
    	};

    	$scope.cancel = function () {
    		if (!$scope.popup.loading) {
    			$scope.$hide();
    		}
    	};
}]);

angular.module('flowableModeler')
    .controller('DeleteModelPopupCtrl', ['$rootScope', '$scope', '$http', '$translate', function ($rootScope, $scope, $http, $translate) {

        var model;
        var popupType;
        if ($scope.model.process) {
            model = $scope.model.process;
            popupType = 'PROCESS';
        } else if ($scope.model.caseModel) {
            model = $scope.model.caseModel;
            popupType = 'CASE';
        } else if ($scope.model.form) {
            model = $scope.model.form;
            popupType = 'FORM';
        } else if ($scope.model.decisionTable) {
            model = $scope.model.decisionTable;
            popupType = 'DECISION-TABLE';
        } else {
            model = $scope.model.app;
            popupType = 'APP';
        }

        $scope.popup = {
            loading: true,
            loadingRelations: true,
            cascade: 'false',
            popupType: popupType,
            model: model
        };

        // Loading relations when opening
        $http({method: 'GET', url: FLOWABLE.APP_URL.getModelParentRelationsUrl($scope.popup.model.id)}).
            success(function (data, status, headers, config) {
                $scope.popup.loading = false;
                $scope.popup.loadingRelations = false;
                $scope.popup.relations = data;
            }).
            error(function (data, status, headers, config) {
                $scope.$hide();
                $scope.popup.loading = false;
            });

        $scope.ok = function () {
            $scope.popup.loading = true;
            var params = {
                // Explicit string-check because radio-values cannot be js-booleans
                cascade: $scope.popup.cascade === 'true'
            };

            $http({method: 'DELETE', url: FLOWABLE.APP_URL.getModelUrl($scope.popup.model.id), params: params}).
                success(function (data, status, headers, config) {
                    $scope.$hide();
                    $scope.popup.loading = false;
                    $scope.addAlertPromise($translate(popupType + '.ALERT.DELETE-CONFIRM'), 'info');
                    $scope.returnToList();
                }).
                error(function (data, status, headers, config) {
                    $scope.$hide();
                    $scope.popup.loading = false;
                });
        };

        $scope.cancel = function () {
            if (!$scope.popup.loading) {
                $scope.$hide();
            }
        };
    }]);

angular.module('flowableModeler')
.controller('UseAsNewVersionPopupCtrl', ['$rootScope', '$scope', '$http', '$translate', '$location', function ($rootScope, $scope, $http, $translate, $location) {

	var model;
	var popupType;
	if ($scope.model.process) {
		model = $scope.model.process;
		popupType = 'PROCESS';
	} else if ($scope.model.caseModel) {
        model = $scope.model.caseModel;
        popupType = 'CASE';
	} else if ($scope.model.form) {
        model = $scope.model.form;
        popupType = 'FORM';
    } else if ($scope.model.decisionTable) {
        model = $scope.model.decisionTable;
        popupType = 'DECISION-TABLE';
    } else {
        model = $scope.model.app;
        popupType = 'APP';
    }

	$scope.popup = {
		loading: false,
		model: model,
		popupType: popupType,
		latestModelId: $scope.model.latestModelId,
		comment: ''
	};

	$scope.ok = function () {
		$scope.popup.loading = true;

		var actionData = {
			action: 'useAsNewVersion',
			comment: $scope.popup.comment
		};

		$http({method: 'POST', url: FLOWABLE.APP_URL.getModelHistoryUrl($scope.popup.latestModelId, $scope.popup.model.id), data: actionData}).
			success(function(data, status, headers, config) {

                var backToOverview = function() {
                    if (popupType === 'FORM') {
                        $location.path("/forms/" +  $scope.popup.latestModelId);
                    } else if (popupType === 'APP') {
                        $location.path("/apps/" +  $scope.popup.latestModelId);
                    } else if (popupType === 'DECISION-TABLE') {
                        $location.path("/decision-tables/" +  $scope.popup.latestModelId);
                    } else if (popupType === 'CASE') {
                        $location.path("/casemodels/" +  $scope.popup.latestModelId);
                    } else {
                        $location.path("/processes/" +  $scope.popup.latestModelId);
                    }
                };


                if (data && data.unresolvedModels && data.unresolvedModels.length > 0) {

                    // There were unresolved models

                    $scope.popup.loading = false;
                    $scope.popup.foundUnresolvedModels = true;
                    $scope.popup.unresolvedModels = data.unresolvedModels;

                    $scope.close = function() {
                        $scope.$hide();
                        backToOverview();
                    };


                } else {

                    // All models working resolved perfectly

                    $scope.popup.loading = false;
                    $scope.$hide();

                    $scope.addAlertPromise($translate(popupType + '.ALERT.NEW-VERSION-CONFIRM'), 'info');
                    backToOverview();

                }

			}).
			error(function(data, status, headers, config) {
				$scope.$hide();
				$scope.popup.loading = false;
			});
	};

	$scope.cancel = function () {
		if (!$scope.popup.loading) {
			$scope.$hide();
		}
	};
}]);
