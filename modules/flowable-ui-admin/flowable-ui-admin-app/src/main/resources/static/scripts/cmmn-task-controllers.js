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

flowableAdminApp.controller('CmmnTaskController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate, $q, gridConstants) {

        $rootScope.navigation = {main: 'cmmn-engine', sub: 'tasks'};
		
		$scope.tabData = {
            tabs: [
               {id: 'subTasks', name: 'TASK.TITLE.SUBTASKS'},
               {id: 'variables', name: 'TASK.TITLE.VARIABLES'},
               {id: 'identityLinks', name: 'TASK.TITLE.IDENTITY-LINKS'}
            ],
        };

		$scope.tabData.activeTab = $scope.tabData.tabs[0].id;

		$scope.returnToList = function() {
			$location.path("/tasks");
		};

		$scope.openTask = function(taskId) {
			if (taskId) {
				$location.path("/task/" + taskId);
			}
		};

		$scope.openCaseInstance = function(caseInstanceId) {
			if (caseInstanceId) {
				$location.path("/case-instance/" + caseInstanceId);
			}
		};

		$scope.openCaseDefinition = function(caseDefinitionId) {
			if (caseDefinitionId) {
				$location.path("/case-definition/" + caseDefinitionId);
			}
		};

		$scope.loadTask = function() {
			$scope.task = undefined;
			// Load task
			$http({method: 'GET', url: '/app/rest/admin/cmmn-tasks/' + $routeParams.taskId}).
			success(function(data, status, headers, config) {
				$scope.task = data;

				if(data) {
					$scope.taskCompleted = data.endTime != undefined;
					$scope.taskPartOfCase = data.caseInstanceId != undefined;
				}

				// Load runtime-task, if available, for accurate delegation-state
				$scope.loadRuntimeTask();

				// Start loading children
				$scope.loadSubTasks();
				$scope.loadVariables();
				$scope.loadIdentityLinks();
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
		};

		$scope.loadRuntimeTask = function() {
			if($scope.task && !$scope.taskCompleted) {
				// Load runtime task, if available to fetch delegation state
				$http({method: 'GET', url: '/app/rest/admin/cmmn-tasks/' + $routeParams.taskId,
					params: {runtime: 'true'}}).
				success(function(data, status, headers, config) {
					// Workaround for pre 5.15 installs, historic assignee is not updated when set to null
					$scope.task.assignee = data.assignee;
					if(data.delegationState) {
						$scope.task.delegationState = data.delegationState;
					} else {
						// Use empty string to trigger delegate-button to show when delegationState is loaded
						$scope.task.delegationState = '';
					}
					$scope.task.delegationStateLoaded = true;
				});
			}
		};

		$scope.subTaskSelected = function(task) {
	    	if(task && task.getProperty('id')) {
	    		$scope.openTask(task.getProperty('id'));
	    	}
		};
	  // Config for subtasks grid
    $q.all([$translate('TASKS.HEADER.ID'),
             $translate('TASKS.HEADER.NAME'),
             $translate('TASKS.HEADER.ASSIGNEE'),
             $translate('TASKS.HEADER.OWNER')])
    .then(function(headers) {
        $scope.subTaskGridDefinitions = {
            data: 'subTasks.data',
            enableRowReordering: false,
            enableColumnResize: true,
            multiSelect: false,
            keepLastSelected : false,
            enableSorting: false,
            rowHeight: 36,
            afterSelectionChange: $scope.subTaskSelected,
            columnDefs: [
                { field: 'id', displayName: headers[0], width: 50, cellTemplate: gridConstants.defaultTemplate},
                { field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                { field: 'assignee', displayName: headers[2], width: 100, cellTemplate: gridConstants.defaultTemplate},
                { field: 'owner', displayName: headers[3], width: 100, cellTemplate: gridConstants.defaultTemplate}
            ]
        };
    });

    $q.all([$translate('VARIABLES.HEADER.NAME'),
            $translate('VARIABLES.HEADER.TYPE'),
            $translate('VARIABLES.HEADER.VALUE')])
   .then(function(headers) {
        var variableValueTemplate = '<div><div class="ngCellText">{{row.getProperty("variable.valueUrl") && "(Binary)" || row.getProperty(col.field)}}</div></div>';
        var variableTypeTemplate = '<div><div class="ngCellText">{{row.getProperty(col.field) && row.getProperty(col.field) || "null"}}</div></div>';
        // Config for variable grid
        $scope.variableGridDefinitions = {
            data: 'variables.data',
            enableRowReordering: false,
            enableColumnResize: true,
            multiSelect: false,
            keepLastSelected : false,
            enableSorting: false,
            rowHeight: 36,
            columnDefs: [
                { field: 'variable.name', displayName: headers[0]},
                { field: 'variable.type', displayName: headers[1], cellTemplate: variableTypeTemplate},
                { field: 'variable.value', displayName: headers[2], cellTemplate: variableValueTemplate}
            ]
        };
   });

    $q.all([$translate('IDENTITY-LINKS.HEADER.TYPE'),
            $translate('IDENTITY-LINKS.HEADER.GROUP-ID'),
            $translate('IDENTITY-LINKS.HEADER.USER-ID')])
   .then(function(headers) {
        // Config for variable grid
        $scope.identityLinkGridDefinitions = {
            data: 'identityLinks.data',
            enableRowReordering: false,
            multiSelect: false,
            keepLastSelected : false,
            enableSorting: false,
            rowHeight: 36,
            columnDefs: [
                { field: 'type', displayName: headers[0]},
                { field: 'groupId', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                { field: 'userId', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate}
            ]
        };
   });


        $scope.showAllSubtasks = function() {
        	// Populate the task-filter with parentId
        	$rootScope.filters.forced.cmmnTaskFilter = {
        		parentTaskId: $scope.task.id
        	};

        	$scope.returnToList();
        };

		$scope.loadSubTasks = function() {
			$scope.subTasks = undefined;
			$http({method: 'GET', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id +'/subtasks'}).
			success(function(data, status, headers, config) {
				$scope.subTasks = data;
				$scope.tabData.tabs[0].info = data.total;
			});
		};

		$scope.loadVariables = function() {
			$scope.variables = undefined;
			$http({method: 'GET', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id +'/variables'}).
			success(function(data, status, headers, config) {
				$scope.variables = data;
				$scope.tabData.tabs[1].info = data.total;
			});
		};

		$scope.loadIdentityLinks = function() {
			$scope.identityLinks = undefined;
			$http({method: 'GET', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id +'/identitylinks'}).
			success(function(data, status, headers, config) {
				$scope.identityLinks = {data: data, size: data.length};
				$scope.tabData.tabs[2].info = data.length;
			});
		};
		
		$scope.showTaskForm = function() {
		    if($scope.task.endTime) {
		        $http({method: 'GET', url: '/app/rest/admin/task-form-instance/' + $scope.task.id}).
	            success(function(data, status, headers, config) {
	                $rootScope.submittedForm = data.data[0]; // saving fetched submitted form in root scope to avoid another fetch in submitted form controller
	                $location.path("/form-instance/" + data.data[0].id);
	            });
		    }
		};

		// Initial load of task

		$scope.executeWhenReady(function() {
        $scope.loadTask();
    });


		// Dialogs
		var resolve = {
			// Reference the current task
			task: function () {
	          return $scope.task;
	        }
		};

		$scope.deleteTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-delete-popup.html',
				controller: 'CmmnDeleteTaskModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (deleteTask) {
				if(deleteTask) {
				  $scope.addAlert($translate.instant('ALERT.TASK.DELETED', $scope.task), 'info');
					$scope.returnToList();
				}
			});
		};

		$scope.completeTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-complete-popup.html',
				controller: 'CmmnCompleteModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (completeTask) {
				if(completeTask) {
				  $scope.addAlert($translate.instant('ALERT.TASK.COMPLETED', $scope.task), 'info');
					$scope.loadTask();
				}
			});
		};

		$scope.delegateTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-delegate-popup.html',
				controller: 'CmmnDelegateModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (user) {
				if(user) {
					$scope.addAlert($translate.instant('ALERT.TASK.DELEGATED', {id: $scope.task.id, user: user}), 'info');
					$scope.loadTask();
				}
			});
		};

		$scope.resolveTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-resolve-popup.html',
				controller: 'CmmnResolveTaskModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (user) {
				if(user) {
				  $scope.addAlert($translate.instant('ALERT.TASK.RESOLVED', $scope.task), 'info');
					$scope.loadTask();
				}
			});
		};

		$scope.assignTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-assign-popup.html',
				controller: 'CmmnAssignModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (user) {
				if(user !== undefined) {
					if(user == '') {
						$scope.addAlert($translate.instant('ALERT.TASK.UNASSIGNED', $scope.task), 'info');
					} else {
					  $scope.addAlert($translate.instant('ALERT.TASK.ASSIGNED', {id: $scope.task.id, user: user}), 'info');
					}
					$scope.loadTask();
				}
			});
		};

		$scope.editTask = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/task-edit-popup.html',
				controller: 'CmmnEditTaskModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (taskUpdated) {
				if(taskUpdated) {
				  $scope.addAlert($translate.instant('ALERT.TASK.UPDATED', $scope.task), 'info');
					$scope.loadTask();
				}
			});
		};
}]);


// Popup controllers
flowableAdminApp.controller('CmmnCompleteModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

  $scope.task = task;
  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'POST', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id, data: {action: 'complete'}}).
  	  success(function(data, status, headers, config) {
  		$modalInstance.close(true);
  		$scope.status.loading = false;
      }).error(function(data, status, headers, config) {
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

flowableAdminApp.controller('CmmnResolveTaskModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

  $scope.task = task;
  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'POST', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id, data: {action: 'resolve'}}).
  	  success(function(data, status, headers, config) {
  		$modalInstance.close(true);
  		$scope.status.loading = false;
      }).error(function(data, status, headers, config) {
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

flowableAdminApp.controller('CmmnDeleteTaskModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

  $scope.task = task;
  $scope.status = {loading: false};


  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'DELETE', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id}).
    	success(function(data, status, headers, config) {
    		$modalInstance.close(true);
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


flowableAdminApp.controller('CmmnDelegateModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

  $scope.task = task;
  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'POST', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id, data: {action: 'delegate', assignee: $scope.status.user}}).
  	  success(function(data, status, headers, config) {
  	    if ($scope.newAssignee && $scope.newAssignee.name) {
  	      $modalInstance.close($scope.newAssignee.name);
  	    } else {
  	      $modalInstance.close($scope.status.user);
  	    }
  		$scope.status.loading = false;
      }).error(function(data, status, headers, config) {
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

flowableAdminApp.controller('CmmnAssignModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

	  $scope.task = task;
	  $scope.status = {loading: false};

	  $scope.ok = function () {
		  $scope.status.loading = true;
		  var rawUserValue = $scope.status.user;
		  var resultUserValue = $scope.status.user;
		  if(!$scope.status.user) {
			  rawUserValue = null;
			  resultUserValue = '';
		  } else if ($scope.newAssignee && $scope.newAssignee.name) {
		      resultUserValue = $scope.newAssignee.name;
		  }
		  $http({method: 'PUT', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id, data: {assignee: rawUserValue}}).
	  	  success(function(data, status, headers, config) {
	  		$modalInstance.close(resultUserValue);
	  		$scope.status.loading = false;
	      }).error(function(data, status, headers, config) {
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

flowableAdminApp.controller('CmmnEditTaskModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'task', function ($rootScope, $scope, $modalInstance, $http, task) {

      $scope.task = task;
	  $scope.model = {
		  name: task.name,
		  description: task.description,
		  owner: task.owner,
		  assignee: task.assignee,
		  dueDate: task.dueDate,
		  priority: task.priority,
		  category: task.category,
	  };

	  $scope.status = {loading: false};

	  $scope.ok = function () {
		  $scope.status.loading = true;
		  $http({method: 'PUT', url: '/app/rest/admin/cmmn-tasks/' + $scope.task.id, data: $scope.model}).
	  	  success(function(data, status, headers, config) {
	  		$modalInstance.close(true);
	  		$scope.status.loading = false;
	      }).error(function(data, status, headers, config) {
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
