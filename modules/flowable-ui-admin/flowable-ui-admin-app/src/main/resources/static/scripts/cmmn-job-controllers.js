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

flowableAdminApp.controller('CmmnJobController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate) {
		$rootScope.navigation = {main: 'cmmn-engine', sub: 'jobs'};
		$scope.jobType = {
			param: $routeParams.jobType
		}
		
		$scope.returnToList = function() {
			$location.path("/cmmn-jobs").search({jobType: $scope.jobType.param});
		};

		$scope.openDefinition = function(definitionId) {
			if (definitionId) {
				$location.path("/case-definition/" + definitionId);
			}
		};

		$scope.executeJob = function() {
			$http({method: 'POST', url: '/app/rest/admin/cmmn-jobs/' + $scope.job.id}).
        	success(function(data, status, headers, config) {
        	  	$scope.addAlert($translate.instant('ALERT.JOB.EXECUTED', $scope.job), 'info');
        		$scope.returnToList();
        	})
        	.error(function(data, status, headers, config) {
        		$scope.loadJob();
        	});
		};
		
		$scope.moveJob = function() {
			$http({method: 'POST', url: '/app/rest/admin/move-cmmn-jobs/' + $scope.job.id + '?jobType=' + $scope.jobType.param}).
        	success(function(data, status, headers, config) {
        	  	$scope.addAlert($translate.instant('ALERT.JOB.MOVED', $scope.job), 'info');
        		$scope.returnToList();
        	})
        	.error(function(data, status, headers, config) {
        		$scope.loadJob();
        	});
		};

		$scope.deleteJob = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/job-delete-popup.html',
				controller: 'DeleteCmmnJobModalInstanceCtrl',
				resolve: {
					job: function() {
						return $scope.job;
					},
					jobType: function() {
						return $scope.jobType;
					}
				}
			});

			modalInstance.result.then(function (deletejob) {
				if(deletejob) {
					$scope.addAlert($translate.instant('ALERT.JOB.DELETED', $scope.job), 'info');
					$scope.returnToList();
				}
			});
		};

		$scope.openCaseInstance = function(caseInstanceId) {
			if (processInstanceId) {
				$location.path("/case-instance/" + caseInstanceId);
			}
		};

		$scope.openCaseDefinition = function(caseDefinitionId) {
			if (processDefinitionId) {
				$location.path("/case-definition/" + caseDefinitionId);
			}
		};

		$scope.loadJob = function() {
			$scope.job = {};
			$http({method: 'GET', url: '/app/rest/admin/cmmn-jobs/' + $routeParams.jobId + '?jobType=' + $scope.jobType.param}).
	    	success(function(data, status, headers, config) {
	            $scope.job = data;

	            if ($scope.job.exceptionMessage) {
	            	// Fetch the full stacktrace, associated with this job
	            	$http({method: 'GET', url: '/app/rest/admin/cmmn-jobs/' + $scope.job.id + '/stacktrace?jobType=' + $scope.jobType.param}).
	            	success(function(data, status, headers, config) {
	    	            $scope.job.exceptionStack = data;
	            	});
	            }
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

		// Load job
		$scope.executeWhenReady(function() {
		    $scope.loadJob();
		});

}]);

flowableAdminApp.controller('DeleteCmmnJobModalInstanceCtrl',
    ['$rootScope', '$scope', '$modalInstance', '$http', 'job', 'jobType', function ($rootScope, $scope, $modalInstance, $http, job, jobType) {

	  $scope.job = job;
	  $scope.jobType = jobType;
	  $scope.status = {loading: false};

	  $scope.ok = function () {
		  $scope.status.loading = true;
		  $http({method: 'DELETE', url: '/app/rest/admin/cmmn-jobs/' + $scope.job.id + '?jobType=' + jobType.param}).
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
