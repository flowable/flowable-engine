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

flowableAdminApp.controller('DecisionTableDeploymentController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q) {

        $rootScope.navigation = {main: 'dmn-engine', sub: 'deployments'};
        
		$scope.returnToList = function() {
			$location.path("/decision-table-deployments");
		};
		
		$scope.openDecisionTable = function(definition) {
			if (definition && definition.getProperty('id')) {
				$location.path("/decision-table/" + definition.getProperty('id'));
			}
		};
		
		$scope.showAllDecisionTables = function() {
		    // Populate the process-filter with parentId
		    $rootScope.filters.forced.decisionTablesFilter = {
		            deploymentId: $scope.deployment.id
		    };
		    $location.path("/decision-tables");
		};
		
		$q.all([$translate('DECISION-TABLES.HEADER.ID'),
            $translate('DECISION-TABLES.HEADER.NAME'),
            $translate('DECISION-TABLES.HEADER.VERSION'),
            $translate('DECISION-TABLES.HEADER.KEY')])
            .then(function(headers) { 
        
                $scope.gridDecisionTables = {
                    data: 'decisionTables.data',
                    enableRowReordering: false,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDecisionTable,
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
                          confirm: $translate.instant('DECISION-TABLE-DEPLOYMENTS.ACTION.DELETE'),
                          title: $translate.instant('DECISION-TABLE-DEPLOYMENTS.ACTION.DELETE'),
                          message: $translate.instant('DECISION-TABLE-DEPLOYMENTS.POPUP.DELETE.CONFIRM-MESSAGE', $scope.deployment)
                          };
                    }
                }
                
            });
            
            modalInstance.result.then(function (result) {
                if (result === true) {
                    $http({method: 'DELETE', url: '/app/rest/admin/decision-table-deployments/' + $routeParams.deploymentId}).
                    success(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.DECISION-TABLE-DEPLOYMENT.DELETED-DEPLOYMENT', $scope.deployment), 'info');
                        $scope.returnToList();
                    }).
                    error(function(data, status, headers, config) {
                        $scope.addAlert($translate.instant('ALERT.DECISION-TABLE-DEPLOYMENT.DELETE-ERROR', data), 'error');
                    });
                }
            });
            
        };
        
		$scope.executeWhenReady(function() {
		    // Load deployment
		    $http({method: 'GET', url: '/app/rest/admin/decision-table-deployments/' + $routeParams.deploymentId}).
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
		    $http({method: 'GET', url: '/app/rest/admin/decision-tables?deploymentId=' + $routeParams.deploymentId}).
  	    	    success(function(data, status, headers, config) {
  	    	        $scope.decisionTables = data;
  	    	    });
		    
  	     });
}]);