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

flowableAdminApp.controller('EventSubscriptionsController', ['$scope', '$rootScope', '$http', '$timeout','$location','$translate', '$q', 'gridConstants',
    function ($scope, $rootScope, $http, $timeout, $location, $translate, $q, gridConstants) {
		$rootScope.navigation = {main: 'process-engine', sub: 'event-subscriptions'};
		
		$scope.eventSubscriptionData = {};
	    $scope.selectedEvents = [];

	    var filterConfig = {
		    	url: '/app/rest/admin/event-subscriptions',
		    	method: 'GET',
		    	success: function(data, status, headers, config) {
		    		if ($scope.definitionCacheLoaded) {
	                	$scope.processQueryResponse(data);
	                	
	                } else {
		                $rootScope.loadProcessDefinitionsCache().then(function(promise) {
  		        			$rootScope.processDefinitionsCache = promise.data;
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
	                {name: 'EVENT-SUBSCRIPTIONS.SORT.ID', id: 'id'},
	                {name: 'EVENT-SUBSCRIPTIONS.SORT.CREATE-DATE', id: 'dueDate'},
	                {name: 'EVENT-SUBSCRIPTIONS.SORT.PROCESS-INSTANCE-ID', id: 'processInstanceId'}
	            ],
	            
	            supportedProperties: [
	                {id: 'processInstanceId', name: 'EVENT-SUBSCRIPTIONS.FILTER.PROCESS-INSTANCE-ID'},
	                {id: 'tenantIdLike', name: 'EVENT-SUBSCRIPTIONS.FILTER.TENANT-ID'},
	                {id: 'createdBefore', name: 'EVENT-SUBSCRIPTIONS.FILTER.CREATED-BEFORE', showByDefault: true},
	                {id: 'createdAfter', name: 'EVENT-SUBSCRIPTIONS.FILTER.CREATED-AFTER', showByDefault: true}
	            ]
		    };

    		
	    if ($rootScope.filters.forced.eventSubscriptionsFilter) {
	    	// Always recreate the filter and add all properties
	    	$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
    		$rootScope.filters.eventSubscriptionsFilter = $scope.filter;
    		
    		for (var prop in $rootScope.filters.forced.eventSubscriptionsFilter) {
    			$scope.filter.addProperty({id: prop}, $rootScope.filters.forced.eventSubscriptionsFilter[prop]);
    		}
    		
    		$rootScope.filters.forced.eventSubscriptionsFilter = undefined;
    		
	    } else {
	    	if ($rootScope.filters && $rootScope.filters.eventSubscriptionsFilter) {
	    		// Reuse the existing filter
	    		$scope.filter = $rootScope.filters.eventSubscriptionsFilter;
	    		$scope.filter.config = filterConfig;
	    	} else {
	    		$scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
	    		$rootScope.filters.eventSubscriptionsFilter = $scope.filter;
	    	}
	    }
	    
	    $scope.eventSubscriptionSelected = function(event) {
	    	if (event && event.getProperty('id')) {
	    		$location.path('/event-subscriptions/' + event.getProperty('id'));
	    	}
	    };
	    
	    $q.all([$translate('EVENT-SUBSCRIPTIONS.HEADER.ID'),
	            $translate('EVENT-SUBSCRIPTIONS.HEADER.EVENT-TYPE'),
	            $translate('EVENT-SUBSCRIPTIONS.HEADER.EVENT-NAME'), 
	            $translate('EVENT-SUBSCRIPTIONS.HEADER.CREATE-DATE'),
	            $translate('EVENT-SUBSCRIPTIONS.HEADER.PROCESS-DEFINITION')])
	    .then(function(headers) { 

          // Config for grid
          $scope.gridEventSubscriptions = {
              data: 'eventSubscriptionData.data',
              enableRowReordering: true,
              enableColumnResize: true,
              multiSelect: false,
              keepLastSelected : false,
              enableSorting: false,
              rowHeight: 36,
              afterSelectionChange: $scope.eventSubscriptionSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], width: 50, cellTemplate: gridConstants.defaultTemplate},
                  { field: 'eventType', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'eventName', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'created', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                  { field: 'processDefinition.name', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate}
              ]
          };
	     });
        
        $scope.processQueryResponse = function(eventsResponse) {
        	for (var i = 0; i < eventsResponse.data.length; i++) {
        		eventsResponse.data[i].processDefinition = 
            		$rootScope.getProcessDefinitionFromCache(eventsResponse.data[i].processDefinitionId);
            }
        	$scope.eventSubscriptionData = eventsResponse;
        };
        
        $scope.processDefinitionFilterChanged = function() 
        {
        	if ($scope.filter.processDefinition && $scope.filter.processDefinition !== '-1') {
        		$scope.filter.properties.processDefinitionId = $scope.filter.processDefinition;
        		$scope.filter.refresh();
        		
        	} else {
        		var tempProcessDefinitionId = $scope.filter.properties.processDefinitionId;
        		$scope.filter.properties.processDefinitionId = null;
        		if (tempProcessDefinitionId && tempProcessDefinitionId.length > 0) {
        			$scope.filter.refresh();
        		}
        	}
        };
        
        $scope.executeWhenReady(function() {
          	$scope.filter.refresh();
        });
    }]);
