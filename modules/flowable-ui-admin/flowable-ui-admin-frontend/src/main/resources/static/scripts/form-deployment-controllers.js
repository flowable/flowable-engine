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

flowableAdminApp.controller('FormDeploymentController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'form-engine', sub: 'deployments'};
        
		$scope.returnToList = function() {
			$location.path("/form-deployments");
		};
		
		$scope.openDefinition = function(definition) {
			if (definition && definition.getProperty('id')) {
				$location.path("/form-definition/" + definition.getProperty('id'));
			}
		};
		
		$scope.showAllDefinitions = function() {
		    // Populate the form-filter with parentId
		    $rootScope.filters.forced.formDefinitionFilter = {
		            deploymentId: $scope.formDeployment.id
		    };
		    $location.path("/form-definitions");
		};
		
		$q.all([$translate('FORM-DEFINITIONS.HEADER.ID'),
            $translate('FORM-DEFINITIONS.HEADER.NAME'),
            $translate('FORM-DEFINITIONS.HEADER.VERSION'),
            $translate('FORM-DEFINITIONS.HEADER.KEY')])
            .then(function(headers) { 
        
                $scope.gridFormDefinitions = {
                    data: 'formDefinitions.data',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDefinition,
                    columnDefs: [
                          { field: 'id', displayName: headers[0]},
                          { field: 'name', displayName: headers[1]},
                          { field: 'version', displayName: headers[2]},
                          { field: 'key', displayName: headers[3]}
                    ]
                };
        });
		
		$scope.deleteDeployment = function() {
		    var modalInstance = $modal.open({
                templateUrl: 'views/confirm-popup.html',
                controller: 'ConfirmPopupCtrl',
                resolve: {
                    model: function () {
                      return {
                          confirm: $translate.instant('FORM-DEPLOYMENTS.ACTION.DELETE'),
                          title: $translate.instant('FORM-DEPLOYMENTS.ACTION.DELETE'),
                          message: $translate.instant('FORM-DEPLOYMENTS.POPUP.DELETE.CONFIRM-MESSAGE', $scope.formDeployment)
                          };
                    }
                }
                
            });
            
            modalInstance.result.then(function (result) {
                if (result === true) {
                    $http({method: 'DELETE', url: '/app/rest/admin/form-deployments/' + $routeParams.formDeploymentId}).
                    success(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.FORM-DEPLOYMENT.DELETED-DEPLOYMENT', $scope.formDeployment), 'info');
                        $scope.returnToList();
                    }).
                    error(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.FORM-DEPLOYMENT.DELETE-ERROR', data), 'error');
                    });
                }
            });
            
        };
        
		$scope.executeWhenReady(function() {
		    // Load deployment
		    $http({method: 'GET', url: '/app/rest/admin/form-deployments/' + $routeParams.formDeploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.formDeployment = data;
  	    	    }).
  	    	    error(function(data, status, headers, config) {
                    if (data && data.message) {
                        // Extract error-message
                        $rootScope.addAlert(data.message, 'error');
                    } else {
                        // Use default error-message
                        $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                    }
  	    	    });
  		
		    // Load process definitions
		    $http({method: 'GET', url: '/app/rest/admin/form-definitions?deploymentId=' + $routeParams.formDeploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.formDefinitions = data;
  	    	    });
		    
  	     });
}]);