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

flowableAdminApp.controller('CaseDefinitionsController', ['$rootScope', '$scope', '$http', '$timeout', '$location','$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

		$rootScope.navigation = {main: 'cmmn-engine', sub: 'definitions'};
        
		$scope.filter = {};
		$scope.caseDefinitionsData = {};

		// Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
	    $scope.selectedDefinitions = [];

	    var filterConfig = {
	    	url: '/app/rest/admin/case-definitions',
	    	method: 'GET',
	    	success: function(data, status, headers, config) {
	    		$scope.caseDefinitionsData = data;
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
                {name: 'CASE-DEFINITIONS.SORT.ID', id: 'id'},
                {name: 'CASE-DEFINITIONS.SORT.NAME', id: 'name'},
                {name: 'CASE-DEFINITIONS.SORT.VERSION', id: 'version'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'CASE-DEFINITIONS.FILTER.NAME', showByDefault: false},
                {id: 'keyLike', name: 'CASE-DEFINITIONS.FILTER.KEY', showByDefault: true},
                {id: 'deploymentId', name: 'CASE-DEFINITIONS.FILTER.DEPLOYMENT-ID', showByDefault: false},
                {id: 'tenantIdLike', name: 'CASE-DEFINITIONS.FILTER.TENANT-ID', showByDefault: false},
                {id: 'latest', name: 'CASE-DEFINITIONS.FILTER.LATEST', showByDefault: true, defaultValue: true}
            ]
	    };

	    if ($rootScope.filters.forced.caseDefinitionFilter) {
	        // Always recreate the filter and add all properties
	        $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
	        $rootScope.filters.caseDefinitionFilter = $scope.filter;

	        for (var prop in $rootScope.filters.forced.processDefinitionFilter) {
	            $scope.filter.addProperty({id: prop}, $rootScope.filters.forced.caseDefinitionFilter[prop]);
	            if (prop == 'deploymentId') {
	                $scope.filter.removeProperty({id: 'latest'});
	            }
	        }

	        $rootScope.filters.forced.caseDefinitionFilter = undefined;

	    } else if ($rootScope.filters && $rootScope.filters.caseDefinitionFilter) {
	    	// Reuse the existing filter
	    	$scope.filter = $rootScope.filters.caseDefinitionFilter;
	    	$scope.filter.config = filterConfig;
	    } else {
		    $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
		    $rootScope.filters.caseDefinitionFilter = $scope.filter;
	    }

	    $scope.definitionSelected = function(definition) {
	    	if (definition && definition.getProperty('id')) {
	    		$location.path('/case-definition/' + definition.getProperty('id'));
	    	}
	    };

	    $scope.toggleLatestVersion = function() {
	      if($scope.filter.properties.latest === true) {
	        $scope.filter.properties.nameLike = undefined;
	        $scope.filter.properties.deploymentId = undefined;
	      }

	      $scope.filter.refresh();
	    };

	    $q.all([$translate('CASE-DEFINITIONS.HEADER.ID'),
              $translate('CASE-DEFINITIONS.HEADER.NAME'),
              $translate('CASE-DEFINITIONS.HEADER.VERSION'),
              $translate('CASE-DEFINITIONS.HEADER.KEY'),
              $translate('CASE-DEFINITIONS.HEADER.TENANT')])
              .then(function(headers) {

          // Config for grid
          $scope.gridDefinitions = {
              data: 'caseDefinitionsData.data',
              enableRowReordering: true,
              enableColumnResize: true,
              multiSelect: false,
              keepLastSelected : false,
              rowHeight: 36,
              selectedItems: $scope.selectedDefinitions,
              afterSelectionChange: $scope.definitionSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'version', displayName: headers[2], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'key', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'tenantId', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate}]
          };
       });


	   // Hook in initial fetching of the definitions
	   $scope.executeWhenReady(function() {
	       $scope.filter.refresh();
	   });
        

    }]);
