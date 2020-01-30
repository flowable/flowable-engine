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
'use strict';

flowableAdminApp.controller('DeploymentController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'process-engine', sub: 'deployments'};
        
		$scope.returnToList = function() {
			$location.path("/deployments");
		};
		
		$scope.openDefinition = function(definition) {
			if (definition && definition.getProperty('id')) {
				$location.path("/process-definition/" + definition.getProperty('id'));
			}
		};
		
		$scope.showAllDefinitions = function() {
		    // Populate the process-filter with parentId
		    $rootScope.filters.forced.processDefinitionFilter = {
		            deploymentId: $scope.deployment.id
		    };
		    $location.path("/process-definitions");
		};
		
		$q.all([$translate('PROCESS-DEFINITIONS.HEADER.ID'), 
            $translate('PROCESS-DEFINITIONS.HEADER.NAME'),
            $translate('PROCESS-DEFINITIONS.HEADER.VERSION'),
            $translate('PROCESS-DEFINITIONS.HEADER.KEY')])
            .then(function(headers) { 
        
                $scope.gridDefinitions = {
                    data: 'definitions.data',
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
                          confirm: $translate.instant('DEPLOYMENTS.ACTION.DELETE'), 
                          title: $translate.instant('DEPLOYMENTS.ACTION.DELETE'), 
                          message: $translate.instant('DEPLOYMENTS.POPUP.DELETE.CONFIRM-MESSAGE', $scope.deployment)
                          };
                    }
                }
                
            });
            
            modalInstance.result.then(function (result) {
                if (result === true) {
                    $http({method: 'DELETE', url: '/app/rest/admin/deployments/' + $routeParams.deploymentId}).
                    success(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.DEPLOYMENT.DELETED-DEPLOYMENT', $scope.deployment), 'info');
                        $scope.returnToList();
                    }).
                    error(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.DEPLOYMENT.DELETE-ERROR', data), 'error');
                    });
                }
            });
            
        };
        
		$scope.executeWhenReady(function() {
		    // Load deployment
		    $http({method: 'GET', url: '/app/rest/admin/deployments/' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.deployment = data;
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
		    $http({method: 'GET', url: '/app/rest/admin/process-definitions?deploymentId=' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.definitions = data;
  	    	    });
		    
  	     });
}]);