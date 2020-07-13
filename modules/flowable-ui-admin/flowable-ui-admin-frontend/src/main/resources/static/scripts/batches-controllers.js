/* 
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

flowableAdminApp.controller('BatchesController', ['$scope', '$rootScope', '$http', '$timeout','$location','$translate', '$q', 'gridConstants', '$routeParams',
    function ($scope, $rootScope, $http, $timeout, $location, $translate, $q, gridConstants, $routeParams) {
		$rootScope.navigation = {main: 'process-engine', sub: 'batches'};
		
		$scope.batchData = {};
	    $scope.selectedBatches = [];

	    var filterConfig = {
		    	url: '/app/rest/admin/batches',
		    	method: 'GET',
		    	success: function(data, status, headers, config) {
		    		$scope.processQueryResponse(data);
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
	                {name: 'BATCHES.SORT.ID', id: 'id'},
	                {name: 'BATCHES.SORT.CREATE-TIME', id: 'createTime'}
	            ],
	            
	            supportedProperties: [
	                {id: 'batchType', name: 'BATCHES.FILTER.BATCH-TYPE', showByDefault: true},
	                {id: 'searchKey', name: 'BATCHES.FILTER.SEARCH-KEY', showByDefault: true},
	                {id: 'searchKey2', name: 'BATCHES.FILTER.SEARCH-KEY2', showByDefault: true},
	                {id: 'createTimeBefore', name: 'BATCHES.FILTER.CREATE-TIME-BEFORE', showByDefault: true},
	                {id: 'createTimeAfter', name: 'JOBS.FILTER.CREATE-TIME-AFTER', showByDefault: true}
	            ]
		    };

    		
	    if ($rootScope.filters.forced.batchFilter) {
	    	// Always recreate the filter and add all properties
	    	$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
    		$rootScope.filters.batchFilter = $scope.filter;
    		
    		for (var prop in $rootScope.filters.forced.batchFilter) {
    			$scope.filter.addProperty({id: prop}, $rootScope.filters.forced.batchFilter[prop]);
    		}
    		
    		$rootScope.filters.forced.batchFilter = undefined;
    		
	    } else {
	    	if ($rootScope.filters && $rootScope.filters.batchFilter) {
	    		// Reuse the existing filter
	    		$scope.filter = $rootScope.filters.batchFilter;
	    		$scope.filter.config = filterConfig;
	    	} else {
	    		$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
	    		$rootScope.filters.batchFilter = $scope.filter;
	    	}
	    }
	    
	    $scope.batchSelected = function(batch) {
	    	if (batch && batch.getProperty('id')) {
	    		$location.path('/batch/' + batch.getProperty('id'));
	    	}
	    };
	    
	    $q.all([$translate('BATCHES.HEADER.ID'), 
	            $translate('BATCHES.HEADER.CREATE-TIME'),
	            $translate('BATCHES.HEADER.SEARCH-KEY'),
	            $translate('BATCHES.HEADER.SEARCH-KEY2')])
	    .then(function(headers) { 

          // Config for grid
          $scope.gridBatches = {
              data: 'batchData.data',
              enableRowReordering: true,
              enableColumnResize: true,
              multiSelect: false,
              keepLastSelected : false,
              enableSorting: false,
              rowHeight: 36,
              afterSelectionChange: $scope.batchSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], width: 50, cellTemplate: gridConstants.defaultTemplate},
                  { field: 'createTime', displayName: headers[1], cellTemplate: gridConstants.dateTemplate},
                  { field: 'searchKey', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'searchKey2', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate}
              ]
          };
	    });
        
        $scope.processQueryResponse = function(batchesResponse) {
        	$scope.batchData = batchesResponse;
        };
        
        $scope.executeWhenReady(function() {
          	$scope.filter.refresh();
        });
    }]);
