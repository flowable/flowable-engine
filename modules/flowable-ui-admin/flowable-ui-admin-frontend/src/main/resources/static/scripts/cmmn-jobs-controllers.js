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

/* Controllers */

flowableAdminApp.controller('CmmnJobsController', ['$scope', '$rootScope', '$http', '$timeout','$location','$translate', '$q', 'gridConstants', '$routeParams',
    function ($scope, $rootScope, $http, $timeout, $location, $translate, $q, gridConstants, $routeParams) {
		$rootScope.navigation = {main: 'cmmn-engine', sub: 'jobs'};
		$scope.jobType = {
			param: $routeParams.jobType
		}
		
		$scope.jobData = {};
	    $scope.selectedJobs = [];

	    var filterConfig = {
		    	url: '/app/rest/admin/cmmn-jobs',
		    	method: 'GET',
		    	success: function(data, status, headers, config) {
		    		if ($scope.definitionCacheLoaded) {
	                	$scope.processQueryResponse(data);
	                	
	                } else {
		                $rootScope.loadCaseDefinitionsCache().then(function(promise) {
  		        			$rootScope.caseDefinitionsCache = promise.data;
  		        			$scope.definitionCacheLoaded = true;
  		        			$scope.processQueryResponse(data);
  		        		});
	                }
	            },
	            error: function(data, status, headers, config) {
	                if (data && data.message) {
	                    // Extract error-message
	                    $rootScope.addAlert(data.message, 'error');
	                } else {
	                    // Use default error-message
	                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
	                }
	            },
	            
	            sortObjects: [
	                {name: 'JOBS.SORT.ID', id: 'id'},
	                {name: 'JOBS.SORT.DUE-DATE', id: 'dueDate'},
	                {name: 'JOBS.SORT.CASE-INSTANCE-ID', id: 'caseInstanceId'}
	            ],
	            
	            supportedProperties: [
	                {id: 'caseInstanceId', name: 'JOBS.FILTER.CASE-INSTANCE-ID'},
	                {id: 'tenantIdLike', name: 'JOBS.FILTER.TENANT-ID'},
	                {id: 'dueBefore', name: 'JOBS.FILTER.DUE-BEFORE', showByDefault: true},
	                {id: 'dueAfter', name: 'JOBS.FILTER.DUE-AFTER', showByDefault: true},
	                {id: 'withException', name: 'JOBS.FILTER.EXCEPTION'}
	            ]
		    };

    		
	    if ($rootScope.filters.forced.cmmnJobFilter) {
	    	// Always recreate the filter and add all properties
	    	$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
    		$rootScope.filters.cmmnJobFilter = $scope.filter;
    		
    		for (var prop in $rootScope.filters.forced.cmmnJobFilter) {
    			$scope.filter.addProperty({id: prop}, $rootScope.filters.forced.cmmnJobFilter[prop]);
    		}
    		
    		$rootScope.filters.forced.cmmnJobFilter = undefined;
    		
	    } else {
	    	if ($rootScope.filters && $rootScope.filters.cmmnJobFilter) {
	    		// Reuse the existing filter
	    		$scope.filter = $rootScope.filters.cmmnJobFilter;
	    		$scope.filter.config = filterConfig;
	    	} else {
	    		$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
	    		$rootScope.filters.cmmnJobFilter = $scope.filter;
	    	}
	    }
	    
	    if ($scope.jobType.param) {
	    	$scope.filter.jobType = $scope.jobType.param;
	    }
	    
	    if (!$scope.filter.jobType) {
	    	$scope.filter.jobType = 'executableJob';
	    }
	    
	    $scope.filter.properties.jobType = $scope.filter.jobType;
	    
	    $scope.jobSelected = function(job) {
	    	if (job && job.getProperty('id')) {
	    		$location.path('/cmmn-job/' + job.getProperty('id')).search({jobType: $scope.filter.jobType});
	    	}
	    };
	    
	    $q.all([$translate('JOBS.HEADER.ID'), 
	            $translate('JOBS.HEADER.DUE-DATE'),
	            $translate('JOBS.HEADER.CASE-DEFINITION'),
	            $translate('JOBS.HEADER.RETRIES'),
	            $translate('JOBS.HEADER.EXCEPTION')])
	    .then(function(headers) { 

          // Config for grid
          $scope.gridJobs = {
              data: 'jobData.data',
              enableRowReordering: true,
              enableColumnResize: true,
              multiSelect: false,
              keepLastSelected : false,
              enableSorting: false,
              rowHeight: 36,
              afterSelectionChange: $scope.jobSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], width: 50, cellTemplate: gridConstants.defaultTemplate},
                  { field: 'dueDate', displayName: headers[1], cellTemplate: gridConstants.dateTemplate},
                  { field: 'caseDefinition.name', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'retries', displayName: headers[3], width: 50, cellTemplate: gridConstants.defaultTemplate},
                  { field: 'exceptionMessage', displayName: headers[4], width: 150, cellTemplate: gridConstants.defaultTemplate}
              ]
          };
	    });
        
        $scope.processQueryResponse = function(jobsResponse) {
        	for (var i = 0; i < jobsResponse.data.length; i++) {
        		jobsResponse.data[i].caseDefinition = 
            		$rootScope.getCaseDefinitionFromCache(jobsResponse.data[i].caseDefinitionId);
            	
            }
        	$scope.jobData = jobsResponse;
        };
        
        $scope.jobTypeFilterChanged = function()  {
        	$scope.filter.properties.jobType = $scope.filter.jobType;
        	$scope.filter.refresh();
        };
        
        $scope.caseDefinitionFilterChanged = function()  {
        	if ($scope.filter.caseDefinition && $scope.filter.caseDefinition !== '-1') {
        		$scope.filter.properties.caseDefinitionId = $scope.filter.caseDefinition;
        		$scope.filter.refresh();
        		
        	} else {
        		var tempCaseDefinitionId = $scope.filter.properties.caseDefinitionId;
        		$scope.filter.properties.caseDefinitionId = null;
        		if (tempCaseDefinitionId && tempCaseDefinitionId.length > 0) {
        			$scope.filter.refresh();
        		}
        	}
        };
        
        $scope.executeWhenReady(function() {
          	$scope.filter.refresh();
        });
    }]);
