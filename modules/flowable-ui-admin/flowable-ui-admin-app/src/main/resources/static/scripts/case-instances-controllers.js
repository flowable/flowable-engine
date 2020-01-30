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

flowableAdminApp.controller('CaseInstancesController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, gridConstants) {

		$rootScope.navigation = {main: 'cmmn-engine', sub: 'instances'};
		
		$scope.filter = {};
		$scope.caseInstances = {};
		$scope.definitionCacheLoaded = false;

		$scope.variableFilterTypes = FlowableAdmin.Utils.variableFilterTypes;
		$scope.variableFilterOperators = FlowableAdmin.Utils.variableFilterOperators;

	    var filterConfig = {
	    	url: '/app/rest/admin/case-instances',
	    	method: 'POST',
	    	success: function(data, status, headers, config) {
	    		if ($scope.definitionCacheLoaded) {
                	$scope.processQueryResponse(data);
                }
                else {
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
                {name: 'CASE-INSTANCES.SORT.ID', id: 'caseInstanceId'},
                {name: 'CASE-INSTANCES.SORT.START-TIME', id: 'startTime'}
            ],
            
            options: {
                finished: [
                    {name: 'CASE-INSTANCES.FILTER.STATUS-ANY', value: ''},
                    {name: 'CASE-INSTANCES.FILTER.STATUS-ACTIVE', value: 'false'},
                    {name: 'CASE-INSTANCES.FILTER.STATUS-COMPLETE', value: 'true'}
                ]
            },

            supportedProperties: [
                {id: 'finished', name: 'CASE-INSTANCES.FILTER.STATUS', showByDefault: true},
                {id: 'caseBusinessKey', name: 'CASE-INSTANCES.FILTER.BUSINESS-KEY'},
                {id: 'startedBefore', name: 'CASE-INSTANCES.FILTER.STARTED-BEFORE'},
                {id: 'startedAfter', name: 'CASE-INSTANCES.FILTER.STARTED-AFTER'},
                {id: 'finishedBefore', name: 'CASE-INSTANCES.FILTER.ENDED-BEFORE'},
                {id: 'finishedAfter', name: 'CASE-INSTANCES.FILTER.ENDED-AFTER'},
                {id: 'variable', name: 'CASE-INSTANCES.FILTER.VARIABLE'},
                {id: 'tenantIdLike', name: 'CASE-INSTANCES.FILTER.TENANT-ID'}
            ]
	    };

	    if ($rootScope.filters.forced.caseInstanceFilter) {
	        // Always recreate the filter and add all properties
            $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.caseInstanceFilter = $scope.filter;

    		for (var prop in $rootScope.filters.forced.caseInstanceFilter) {
    			$scope.filter.addProperty({id: prop}, $rootScope.filters.forced.caseInstanceFilter[prop]);
    		}

    		$rootScope.filters.forced.caseInstanceFilter = undefined;

	    } else if ($rootScope.filters && $rootScope.filters.caseInstanceFilter) {
            // Reuse the existing filter
            $scope.filter = $rootScope.filters.caseInstanceFilter;
            $scope.filter.config = filterConfig;

	    } else {
            $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
            $rootScope.filters.caseInstanceFilter = $scope.filter;
	    }

	    $scope.caseInstanceSelected = function(caseInstance) {
            if (caseInstance && caseInstance.getProperty('id')) {
                $location.path('/case-instance/' + caseInstance.getProperty('id'));
            }
        };

	    if (!$scope.filter.properties.variables) {
	        $scope.filter.properties.variables = [];
	    }

	    // Set value-filter callback to convert variables to nice format
	    $scope.filter.config.valueFilter = function(prop, value) {
	      if (prop == 'variables') {
	        var actualValue = [];
	        var variable;
	        for (var i=0; i<value.length; i++) {
	          variable = value[i];

	          if (variable.name && variable.type && variable.value !== undefined && variable.value !== '' && variable.operator) {
	              var varPayload =  {
	                  name: variable.name,
	                  value: variable.value,
	                  operation: variable.operator.id,
	                  type: variable.type.id
	              };

	              if (variable.type.id == 'long' || variable.type.id == 'short' || variable.type.id == 'double' || variable.type.id == 'integer') {
	                  varPayload.value = parseFloat(varPayload.value);
	                  if (!isNaN(varPayload.value)) {
	                      // Return valid value for number
	                      actualValue.push(varPayload);
	                  }
	              } else {
	                  // Return valid value
	                  actualValue.push(varPayload);
	              }
	          }
	        }
	        return actualValue;
	      } else {
	        return value;
	      }
	    };

	    $scope.clearFilters = function() {
	      $scope.filter.clear();
	      $scope.filter.properties.variables = [];
	    };

	    $scope.setVariableFilterType = function(varFilter, type) {

	      varFilter.value = undefined;
	      if (type.id == 'boolean') {
	        varFilter.value = true;

	        if (varFilter.operator.id != 'equals') {
	          varFilter.operator = $scope.variableFilterOperators[0];
	        }
	      }

	      varFilter.type = type;
	      $scope.highlightVariableValue(varFilter);
	      $scope.filter.refresh();
	    };

	    $scope.setVariableFilterOperator = function(varFilter, operator) {
	      if(operator.id == 'like') {
	        varFilter.value = '';
	        varFilter.type = $scope.variableFilterTypes[0];
	      }

	      if(varFilter.type.id == 'boolean' && operator.id != 'equals') {
	        varFilter.operator = $scope.variableFilterOperators[0];
	      } else {
	        varFilter.operator = operator;
	      }
	      $scope.highlightVariableValue(varFilter);
	      $scope.filter.refresh();
	    };

	    $scope.highlightVariableValue = function(varFilter) {
	       var index = -1;
            for (var i=0; i<$scope.filter.properties.variables.length; i++) {
                if (varFilter == $scope.filter.properties.variables[i]) {
                    index = i;
                    break;
                }
            }

            $timeout(function() {
                var formField = $('#filter-variable-value-' + index);
                formField.focus();
            }, 100);
	    };

	    $scope.addFilterProperty = function(prop) {
	      if(prop.id != 'variable') {
	        $scope.filter.addProperty(prop);
	      } else {
	        // Add additional variable
	        $scope.filter.properties.variables.push({
	          type: $scope.variableFilterTypes[0],
	          operator: $scope.variableFilterOperators[0],
	          scope: 'variable'
	        });

	        $timeout(function() {
	          var formField = $('#filter-variable-name-' + ($scope.filter.properties.variables.length - 1));
	          formField.focus();
	        }, 100);
	      }
	    };

	    $q.all([$translate('CASE-INSTANCES.HEADER.ID'),
              $translate('CASE-INSTANCES.HEADER.BUSINESS-KEY'),
              $translate('CASE-INSTANCES.HEADER.CASE-DEFINITION'),
              $translate('CASE-INSTANCES.HEADER.CREATE-TIME'),
              $translate('CASE-INSTANCES.HEADER.END-TIME'),])
              .then(function(headers) {

          $scope.gridInstances = {
              data: 'caseInstances.data',
              enableRowReordering: true,
              enableColumnResize: true,
              multiSelect: false,
              keepLastSelected : false,
              rowHeight: 36,
              afterSelectionChange: $scope.caseInstanceSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'businessKey', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'caseDefinition.name', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'startTime', displayName: headers[3], cellTemplate: gridConstants.dateTemplate},
                  { field: 'endTime', displayName: headers[4], cellTemplate: gridConstants.dateTemplate}]
          };
        });

        $scope.processQueryResponse = function(caseInstancesResponse) {
        	for (var i = 0; i < caseInstancesResponse.data.length; i++) {
				caseInstancesResponse.data[i].caseDefinition =
            		$rootScope.getCaseDefinitionFromCache(caseInstancesResponse.data[i].caseDefinitionId);

				// Fallback to id, of case definition doesn't have a name (getCaseDefinitionFromCache returns null if not found)
				if ((caseInstancesResponse.data[i].caseDefinition === null || caseInstancesResponse.data[i].caseDefinition === undefined) && caseInstancesResponse.data[i].caseDefinitionId) {
				    caseInstancesResponse.data[i].caseDefinition = { id: caseInstancesResponse.data[i].caseDefinitionId, name: caseInstancesResponse.data[i].caseDefinitionId }
				}

            }
			$scope.caseInstances = caseInstancesResponse;
        };

        $scope.caseDefinitionFilterChanged = function() {
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
