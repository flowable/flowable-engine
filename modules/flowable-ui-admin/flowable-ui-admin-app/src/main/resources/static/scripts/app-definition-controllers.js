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

flowableAdminApp.controller('AppDefinitionController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'app-engine', sub: 'definitions'};

        $scope.tabData = {
		    tabs: [
		       {id: 'processDefinitions', name: 'APP-DEFINITION.TITLE.PROCESS-DEFINITIONS'},
		       {id: 'caseDefintions', name: 'APP-DEFINITION.TITLE.CASE-DEFINITIONS'},
		       {id: 'decisionTables', name: 'APP-DEFINITION.TITLE.DECISION-TABLES'},
		       {id: 'formDefinitions', name: 'APP-DEFINITION.TITLE.FORM-DEFINITIONS'}
		    ]
		};
		$scope.tabData.activeTab = $scope.tabData.tabs[0].id;

		$scope.returnToList = function() {
			$location.path("/app-definitions");
		};

		$scope.openDefinition = function(definitionId) {
			if (definitionId) {
				$location.path("/app-definition/" + definitionId);
			}
		};


		$scope.openDeployment = function(deploymentId) {
		    if (deploymentId) {
		        $location.path("/app-deployment/" + deploymentId);
		    }
		};

        $scope.openProcessDefinition = function(processDefinition) {
          if (processDefinition && processDefinition.getProperty('id')) {
            $location.path("/process-definition/" + processDefinition.getProperty('id'));
          }
        };
        
        $scope.openCaseDefinition = function(caseDefinition) {
          if (caseDefinition && caseDefinition.getProperty('id')) {
            $location.path("/case-definition/" + caseDefinition.getProperty('id'));
          }
        };
    
        $scope.openFormDefinition = function (formDefinition) {
            if (formDefinition && formDefinition.getProperty('id')) {
                $location.path("/form-definition/" + formDefinition.getProperty('id'));
            }
        };
        
        $scope.openDecisionTable = function (decisionTable) {
            if (decisionTable && decisionTable.getProperty('id')) {
                $location.path("/decision-table/" + decisionTable.getProperty('id'));
            }
        };
        
        $scope.loadProcessDefinitions = function() {
            $scope.processDefinitions = undefined;
            $http({method: 'GET', url: '/app/rest/admin/app-definitions/' + $scope.definition.id +'/process-definitions?deploymentId=' + $scope.definition.deploymentId}).
            success(function(data, status, headers, config) {
                $scope.processDefinitions = data;
                $scope.tabData.tabs[0].info = data.total;
            });
        };
    
        $scope.loadCaseDefinitions = function() {
            $scope.caseDefinitions = undefined;
            $http({method: 'GET', url: '/app/rest/admin/app-definitions/' + $scope.definition.id +'/case-definitions?deploymentId=' + $scope.definition.deploymentId}).
            success(function(data, status, headers, config) {
                $scope.caseDefinitions = data;
                $scope.tabData.tabs[1].info = data.total;
            });
        };
        
        $scope.loadDecisionTables = function() {
            $scope.decisionTables = undefined;
            $http({method: 'GET', url: '/app/rest/admin/app-definitions/'+ $scope.definition.id + '/decision-tables?deploymentId=' + $scope.definition.deploymentId}).
            success(function(data, status, headers, config) {
                $scope.decisionTables = data;
                $scope.tabData.tabs[2].info = data.total;
            }).
            error(function(data, status, headers, config) {
            });
        };
        
        $scope.loadFormDefinitions = function() {
            $scope.formDefinitions = undefined;
            $http({method: 'GET', url: '/app/rest/admin/app-definitions/'+ $scope.definition.id + '/form-definitions?deploymentId=' + $scope.definition.deploymentId}).
            success(function(data, status, headers, config) {
                $scope.formDefinitions = data;
                $scope.tabData.tabs[3].info = data.total;
            }).
            error(function(data, status, headers, config) {
            });
        };

		$scope.executeWhenReady(function() {
		    // Load definition
		    $http({method: 'GET', url: '/app/rest/admin/app-definitions/' + $routeParams.definitionId}).
		    success(function(data, status, headers, config) {
		        $scope.definition = data;
		        $scope.loadProcessDefinitions();
		        $scope.loadCaseDefinitions();
                $scope.loadDecisionTables();
                $scope.loadFormDefinitions();
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

            $q.all([$translate('PROCESS-DEFINITION.ID'),
                  $translate('PROCESS-DEFINITION.NAME'),
                  $translate('PROCESS-DEFINITION.KEY'),
                  $translate('PROCESS-DEFINITION.CATEGORY')])
            .then(function(headers) {
                
                $scope.gridProcessDefinitions = {
                    data: 'processDefinitions.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openProcessDefinition,
                    columnDefs: [
                        { field: 'id', displayName: headers[0], width: 75},
                        { field: 'name', displayName: headers[1]},
                        { field: 'key', displayName: headers[2]},
                        { field: 'category', displayName: headers[3]}
                    ]
                };
            });

		    $q.all([$translate('CASE-DEFINITION.ID'),
                  $translate('CASE-DEFINITION.NAME'),
                  $translate('CASE-DEFINITION.KEY'),
                  $translate('CASE-DEFINITION.CATEGORY')])
            .then(function(headers) {
                
                $scope.gridCaseDefinitions = {
                    data: 'caseDefinitions.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openCaseDefinition,
                    columnDefs: [
                        { field: 'id', displayName: headers[0], width: 75},
                        { field: 'name', displayName: headers[1]},
                        { field: 'key', displayName: headers[2]},
                        { field: 'category', displayName: headers[3]}
                    ]
                };
            });
            
            $q.all([$translate('DECISION-TABLE.ID'),
                  $translate('DECISION-TABLE.NAME'),
                  $translate('DECISION-TABLE.KEY'),
                  $translate('DECISION-TABLE.CATEGORY')])
            .then(function(headers) {
                
                $scope.gridDecisionTables = {
                    data: 'decisionTables.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDecisionTable,
                    columnDefs: [
                        { field: 'id', displayName: headers[0], width: 75},
                        { field: 'name', displayName: headers[1]},
                        { field: 'key', displayName: headers[2]},
                        { field: 'category', displayName: headers[3]}
                    ]
                };
            });
            
            $q.all([$translate('FORM-DEFINITION.ID'),
                  $translate('FORM-DEFINITION.NAME'),
                  $translate('FORM-DEFINITION.KEY'),
                  $translate('FORM-DEFINITION.CATEGORY')])
            .then(function(headers) {
                
                $scope.gridFormDefinitions = {
                    data: 'formDefinitions.data',
                    enableRowReordering: false,
                    multiSelect: false,
                    keepLastSelected : false,
                    enableSorting: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openFormDefinition,
                    columnDefs: [
                        { field: 'id', displayName: headers[0], width: 75},
                        { field: 'name', displayName: headers[1]},
                        { field: 'key', displayName: headers[2]},
                        { field: 'category', displayName: headers[3]}
                    ]
                };
            });
		      
		});

}]);
