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

flowableAdminApp.controller('AppDeploymentController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'app-engine', sub: 'deployments'};
        
		$scope.returnToList = function() {
			$location.path("/app-deployments");
		};
		
		$scope.openDefinition = function(definition) {
			if (definition && definition.getProperty('id')) {
				$location.path("/app-definition/" + definition.getProperty('id'));
			}
		};
		
		$q.all([$translate('APP-DEFINITIONS.HEADER.ID'),
            $translate('APP-DEFINITIONS.HEADER.NAME'),
            $translate('APP-DEFINITIONS.HEADER.VERSION'),
            $translate('APP-DEFINITIONS.HEADER.KEY')])
            .then(function(headers) { 
        
                $scope.gridAppDefinitions = {
                    data: 'appDefinitions.data',
                    enableRowReordering: false,
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
                          confirm: $translate.instant('APP-DEPLOYMENTS.ACTION.DELETE'),
                          title: $translate.instant('APP-DEPLOYMENTS.ACTION.DELETE'),
                          message: $translate.instant('APP-DEPLOYMENTS.POPUP.DELETE.CONFIRM-MESSAGE', $scope.cmmnDeployment)
                          };
                    }
                }
                
            });
            
            modalInstance.result.then(function (result) {
                if (result === true) {
                    $http({method: 'DELETE', url: '/app/rest/admin/app-deployments/' + $routeParams.deploymentId}).
                    success(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.APP-DEPLOYMENT.DELETED-DEPLOYMENT', $scope.appDeployment), 'info');
                        $scope.returnToList();
                    }).
                    error(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.APP-DEPLOYMENT.DELETE-ERROR', data), 'error');
                    });
                }
            });
            
        };
        
		$scope.executeWhenReady(function() {
		    // Load deployment
		    $http({method: 'GET', url: '/app/rest/admin/app-deployments/' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.appDeployment = data;
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
  		
		    // Load case definitions
		    $http({method: 'GET', url: '/app/rest/admin/app-definitions?deploymentId=' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.appDefinitions = data;
  	    	    });
		    
  	     });
}]);