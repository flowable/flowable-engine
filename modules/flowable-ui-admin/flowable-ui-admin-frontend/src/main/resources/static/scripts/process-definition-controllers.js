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

flowableAdminApp.controller('ProcessDefinitionController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'process-engine', sub: 'definitions'};

        $scope.tabData = {
		    tabs: [
		       {id: 'processInstances', name: 'PROCESS-DEFINITION.TITLE.PROCESS-INSTANCES'},
		       {id: 'jobs', name: 'PROCESS-DEFINITION.TITLE.JOBS'}
		    ]
		};
		$scope.tabData.activeTab = $scope.tabData.tabs[0].id;

		$scope.returnToList = function() {
			$location.path("/process-definitions");
		};

		$scope.openDefinition = function(definitionId) {
			if (definitionId) {
				$location.path("/process-definition/" + definitionId);
			}
		};


		$scope.openDeployment = function(deploymentId) {
		    if (deploymentId) {
		        $location.path("/deployment/" + deploymentId);
		    }
		};

    $scope.editCategory = function() {
      var modalInstance = $modal.open({
        templateUrl: 'views/process-definition-edit-category-popup.html',
        controller: 'EditProcessDefinitionCategoryModalCtrl',
        resolve: {
          definition: function() {
            return $scope.definition;
          }
        }
      });

      modalInstance.result.then(function (data) {
        if(data) {
          $scope.addAlert($translate.instant('ALERT.PROCESS-DEFINITION.CATEGORY-UPDATED', $scope.definition), 'info');
          $scope.definition = data;
        }
      });
    };

    $scope.showProcessDiagram = function() {
      $modal.open({
        templateUrl: 'views/process-definition-diagram-popup.html',
        windowClass: 'modal modal-full-width',
        controller: 'ShowProcessDefinitionDiagramPopupCtrl',
        resolve: {
          definition: function() {
            return $scope.definition;
          }
        }
      });
    };
    
    $scope.showMigrateProcessDialog = function () {
        $scope.migrationScope = {
            targetProcessDefinition: undefined
        };
        
        var modalInstance = $modal.open({
            templateUrl: 'views/process-definition-migration-popup.html', 
            controller: 'ShowProcessDefinitionMigrationPopupCtrl',
            resolve: {
                processDefinition: function() {
                    return $scope.definition;
                },
                migrationScope: function() {
                    return $scope.migrationScope;
                }
            }
        });
        
        modalInstance.result.then(function (migrateProcessInstance) {
            if (migrateProcessInstance) {
                var migrationModalInstance = $modal.open({
                    templateUrl: 'views/process-definition-migration-diagram-popup.html',
                    windowClass: 'modal modal-full-width',
                    controller: 'ShowProcessDefinitionMigrationDiagramPopupCtrl',
                    resolve: {
                        processDefinition: function () {
                            return $scope.definition;
                        },
                        migrationScope: function () {
                            return $scope.migrationScope;
                        }
                    }
                });
                
                migrationModalInstance.result.then(function (result) {
                    $scope.loadProcessInstances();
                });
            }
        });
    };

    $scope.openJob = function(job) {
      if (job && job.getProperty('id')) {
        $location.path("/job/" + job.getProperty('id'));
      }
    };

    $scope.openProcessInstance = function(instance) {
      if (instance && instance.getProperty('id')) {
        $location.path("/process-instance/" + instance.getProperty('id'));
      }
    };
    
    $scope.openStartForm = function () {
        if ($scope.startForm) {
            $location.path("/form/" + $scope.startForm.id);
        }
    };
    
    $scope.openFormDefinition = function (form) {
        if (form && form.getProperty('id')) {
            $location.path("/form-definition/" + form.getProperty('id'));
          }
    };
    
    $scope.openDecisionTable = function (decisionTable) {
        if (decisionTable && decisionTable.getProperty('id')) {
            $location.path("/decision-table/" + decisionTable.getProperty('id'));
          }
    };

    $scope.showAllJobs = function() {
      // Populate the job-filter with process definition id
      $rootScope.filters.forced.jobFilter = {
        processDefinitionId: $scope.definition.id
      };
      $location.path("/jobs");
    };

    $scope.showAllProcesses = function() {
      // Populate the process-filter with parentId
      $rootScope.filters.forced.instanceFilter = {
          processDefinitionId: $scope.definition.id
      };
      $location.path("/process-instances");
    };

    $scope.loadProcessInstances = function() {
      $scope.processInstances = undefined;
      $http({method: 'GET', url: '/app/rest/admin/process-definitions/' + $scope.definition.id +'/process-instances'}).
      success(function(data, status, headers, config) {
        $scope.processInstances = data;
        $scope.tabData.tabs[0].info = data.total;
      });
    };

    $scope.loadJobs = function() {
        $scope.jobs = undefined;
        $http({method: 'GET', url: '/app/rest/admin/process-definitions/' + $scope.definition.id +'/jobs'}).
        success(function(data, status, headers, config) {
            $scope.jobs = data;
            $scope.tabData.tabs[1].info = data.total;
        });
    };
    
    $scope.loadDecisionTables = function() {
        // Load decision tables
        $http({method: 'GET', url: '/app/rest/admin/process-definition-decision-tables/' + $scope.definition.id}).
        success(function(data, status, headers, config) {
            $scope.decisionTables = data;
            $scope.tabData.tabs[2].info = data.length;
        }).
        error(function(data, status, headers, config) {
        });
    };
    
    $scope.loadFormDefinitions = function() {
        // Load forms
        $http({method: 'GET', url: '/app/rest/admin/process-definition-form-definitions/' + $scope.definition.id}).
        success(function(data, status, headers, config) {
            $scope.formDefinitions = data;
            $scope.tabData.tabs[3].info = data.length;
        }).
        error(function(data, status, headers, config) {
        });
    };

	$scope.executeWhenReady(function() {
	    // Load definition
	    $http({method: 'GET', url: '/app/rest/admin/process-definitions/' + $routeParams.definitionId}).
	    success(function(data, status, headers, config) {
	        $scope.definition = data;
	        $scope.loadProcessInstances();
	        $scope.loadJobs();
            $scope.tabData.tabs.push({id: 'decisionTables', name: 'PROCESS-DEFINITION.TITLE.DECISION-TABLES'});
            $scope.tabData.tabs.push({id: 'forms', name: 'PROCESS-DEFINITION.TITLE.FORMS'});
            
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


	    $q.all([$translate('PROCESS-INSTANCES.HEADER.ID'),
              $translate('PROCESS-INSTANCES.HEADER.NAME'),
              $translate('PROCESS-INSTANCES.HEADER.STATUS'),
              $translate('PROCESS-INSTANCES.HEADER.CREATE-TIME')])
        .then(function(headers) {
            var stateTemplate = '<div><div class="ngCellText">{{row.getProperty("endTime") && "Completed" || "Active"}}</div></div>';
            var dateTemplate = '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>';

            // Config for variable grid
            $scope.gridProcessInstances = {
                data: 'processInstances.data',
                enableRowReordering: false,
                enableColumnResize: true,
                multiSelect: false,
                keepLastSelected : false,
                enableSorting: false,
                rowHeight: 36,
                afterSelectionChange: $scope.openProcessInstance,
                columnDefs: [
                    { field: 'id', displayName: headers[0], width: 75},
                    { field: 'name', displayName: headers[1]},
                    { field: 'endTime', displayName: headers[2], cellTemplate: stateTemplate},
                    { field: 'startTime', displayName: headers[3], cellTemplate: dateTemplate}
                ]
            };
        });

	    $q.all([$translate('JOBS.HEADER.ID'),
              $translate('JOBS.HEADER.DUE-DATE'),
              $translate('JOBS.HEADER.RETRIES'),
              $translate('JOBS.HEADER.EXCEPTION')])
        .then(function(headers) {
            var dateTemplate = '<div><div class="ngCellText" title="{{row.getProperty(col.field) | dateformat:\'full\'}}">{{row.getProperty(col.field) | dateformat}}</div></div>';

            // Config for variable grid
            $scope.gridJobs = {
                data: 'jobs.data',
                enableRowReordering: false,
                enableColumnResize: true,
                multiSelect: false,
                keepLastSelected : false,
                enableSorting: false,
                rowHeight: 36,
                afterSelectionChange: $scope.openJob,
                columnDefs: [
                    { field: 'id', displayName: headers[0], width: 50},
                    { field: 'dueDate', displayName: headers[1], cellTemplate: dateTemplate},
                    { field: 'retries', displayName: headers[2]},
                    { field: 'exceptionMessage', displayName: headers[3]}
                ]
            };
        });
	    
	    $q.all([$translate('DECISION-TABLES.HEADER.ID'),
                $translate('DECISION-TABLES.HEADER.NAME'),
                $translate('DECISION-TABLES.HEADER.KEY'),
                $translate('DECISION-TABLES.HEADER.VERSION'),
                $translate('DECISION-TABLES.HEADER.TENANT-ID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridDecisionTables = {
                    data: 'decisionTables',
                    enableRowReordering: true,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openDecisionTable,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'key', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'version', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate}]
                };
            });
	    
	    $q.all([$translate('FORM-DEFINITIONS.HEADER.ID'),
                $translate('FORM-DEFINITIONS.HEADER.NAME'),
                $translate('FORM-DEFINITIONS.HEADER.DEPLOYMENTID'),
                $translate('FORM-DEFINITIONS.HEADER.TENANTID')])
            .then(function (headers) {
                // Config for grid
                $scope.gridFormDefinitions = {
                    data: 'formDefinitions',
                    enableRowReordering: true,
                    enableColumnResize: true,
                    multiSelect: false,
                    keepLastSelected: false,
                    rowHeight: 36,
                    afterSelectionChange: $scope.openFormDefinition,
                    columnDefs: [
                        {field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'deploymentId', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                        {field: 'tenantId', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate}]
                };
            });
	      
	});

}]);

flowableAdminApp.controller('EditProcessDefinitionCategoryModalCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'definition', function ($rootScope, $scope, $modalInstance, $http, definition) {

  $scope.model = {
      id: definition.id,
      category: definition.category,
      name: definition.name,
      key: definition.key,
  };

  $scope.status = {loading: false};


  $scope.ok = function () {
    $scope.status.loading = true;

    var data = {
        category: $scope.model.category
    };

    $http({method: 'PUT', url: '/app/rest/admin/process-definitions/' + $scope.model.id, data: data}).
      success(function(data, status, headers, config) {
        $modalInstance.close(data);
        $scope.status.loading = false;
      }).
      error(function(data, status, headers, config) {
        $modalInstance.close(false);
        $scope.status.loading = false;
      });
  };

  $scope.cancel = function () {
  if(!$scope.status.loading) {
    $modalInstance.dismiss('cancel');
  }
  };
}]);

flowableAdminApp.controller('ShowProcessDefinitionDiagramPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'definition', '$timeout', function ($rootScope, $scope, $modalInstance, $http, definition, $timeout) {

  $scope.model = {
      id: definition.id,
      name: definition.name
  };

  $scope.status = {loading: false};

  $scope.cancel = function () {
    if(!$scope.status.loading) {
      $modalInstance.dismiss('cancel');
    }
  };

  $timeout(function() {
    $("#bpmnModel").attr("data-definition-id", definition.id);
    $("#bpmnModel").attr("data-server-id", $rootScope.activeServers['process']);
    $("#bpmnModel").load("./display/displaymodel.html?definitionId=" + definition.id);
  }, 200);


}]);

flowableAdminApp.controller('ShowProcessDefinitionMigrationPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'processDefinition', 'migrationScope', 
    function ($rootScope, $scope, $modalInstance, $http, processDefinition, migrationScope) {

        $scope.processDefinition = processDefinition;
        $scope.status = {loading: false};
        $scope.model = {
            targetNotSelected: true,
            currentDefinitionId: processDefinition.id
        };
        
        $http({
            method: 'GET',
            url: '/app/rest/admin/process-definitions?key=' + $scope.processDefinition.key + '&size=1000'
        }).success(function (response, status, headers, config) {
            var definitionList = response.data;
            var finalResult = [];
            for (var i = 0; i < definitionList.length; i++) {
                if (definitionList[i].id !== processDefinition.id) {
                    finalResult.push(definitionList[i]);
                }
            }
            $scope.filteredProcessDefinitions = finalResult;
            
        }).error(function (data, status, headers, config) {
        });
        
        $scope.targetProcessDefinitionChanged = function () {
            if ($scope.model.newProcessDefinition && $scope.model.newProcessDefinition.length > 0) {
                migrationScope.targetProcessDefinition = $scope.model.newProcessDefinition;
                $scope.model.targetNotSelected = false;
            }
        };
        
        $scope.ok = function () {
            $modalInstance.close(true);
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };
    }]);
    
flowableAdminApp.controller('ShowProcessDefinitionMigrationDiagramPopupCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'processDefinition', 'migrationScope', '$timeout',
    function ($rootScope, $scope, $modalInstance, $http, processDefinition, migrationScope, $timeout) {

        $scope.model = {
            id: processDefinition.id,
            name: processDefinition.name
        };

        $scope.status = {loading: false};

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.close(true);
            }
        };

        $timeout(function () {
            $("#bpmnModel").attr("data-definition-id", processDefinition.id);
            $("#bpmnModel").attr("data-server-id", $rootScope.activeServers['process'].id);
            $("#targetModel").attr("data-migration-definition-id", migrationScope.targetProcessDefinition);
            
            $("#bpmnModel").load("./display/displaymodel.html?definitionId=" + processDefinition.id);
        }, 200);

    }]);
