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

flowableAdminApp.controller('CmmnDeploymentController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'cmmn-engine', sub: 'deployments'};
        
		$scope.returnToList = function() {
			$location.path("/cmmn-deployments");
		};
		
		$scope.openDefinition = function(definition) {
			if (definition && definition.getProperty('id')) {
				$location.path("/case-definition/" + definition.getProperty('id'));
			}
		};
		
		$scope.showAllDefinitions = function() {
		    // Populate the cmmn-filter with parentId
		    $rootScope.filters.forced.caseDefinitionFilter = {
	            deploymentId: $scope.cmmnDeployment.id
		    };
		    $location.path("/case-definitions");
		};
		
		$q.all([$translate('CASE-DEFINITIONS.HEADER.ID'),
            $translate('CASE-DEFINITIONS.HEADER.NAME'),
            $translate('CASE-DEFINITIONS.HEADER.VERSION'),
            $translate('CASE-DEFINITIONS.HEADER.KEY')])
            .then(function(headers) { 
        
                $scope.gridCaseDefinitions = {
                    data: 'caseDefinitions.data',
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
                          confirm: $translate.instant('CMMN-DEPLOYMENTS.ACTION.DELETE'),
                          title: $translate.instant('CMMN-DEPLOYMENTS.ACTION.DELETE'),
                          message: $translate.instant('CMMN-DEPLOYMENTS.POPUP.DELETE.CONFIRM-MESSAGE', $scope.cmmnDeployment)
                          };
                    }
                }
                
            });
            
            modalInstance.result.then(function (result) {
                if (result === true) {
                    $http({method: 'DELETE', url: '/app/rest/admin/cmmn-deployments/' + $routeParams.deploymentId}).
                    success(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.CMMN-DEPLOYMENT.DELETED-DEPLOYMENT', $scope.cmmnDeployment), 'info');
                        $scope.returnToList();
                    }).
                    error(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.CMMN-DEPLOYMENT.DELETE-ERROR', data), 'error');
                    });
                }
            });
            
        };
        
		$scope.executeWhenReady(function() {
		    // Load deployment
		    $http({method: 'GET', url: '/app/rest/admin/cmmn-deployments/' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.cmmnDeployment = data;
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
		    $http({method: 'GET', url: '/app/rest/admin/case-definitions?deploymentId=' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.caseDefinitions = data;
  	    	    });
		    
  	     });
}]);