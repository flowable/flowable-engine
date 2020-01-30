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

flowableAdminApp.controller('AppDefinitionsController', ['$rootScope', '$scope', '$http', '$timeout', '$location','$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

		$rootScope.navigation = {main: 'app-engine', sub: 'definitions'};
        
		$scope.filter = {};
		$scope.appDefinitionsData = {};

		// Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
	    $scope.selectedDefinitions = [];

	    var filterConfig = {
	    	url: '/app/rest/admin/app-definitions',
	    	method: 'GET',
	    	success: function(data, status, headers, config) {
	    		$scope.appDefinitionsData = data;
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
                {name: 'APP-DEFINITIONS.SORT.ID', id: 'id'},
                {name: 'APP-DEFINITIONS.SORT.NAME', id: 'name'},
                {name: 'APP-DEFINITIONS.SORT.VERSION', id: 'version'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'APP-DEFINITIONS.FILTER.NAME', showByDefault: false},
                {id: 'keyLike', name: 'APP-DEFINITIONS.FILTER.KEY', showByDefault: true},
                {id: 'deploymentId', name: 'APP-DEFINITIONS.FILTER.DEPLOYMENT-ID', showByDefault: false},
                {id: 'tenantIdLike', name: 'APP-DEFINITIONS.FILTER.TENANT-ID', showByDefault: false},
                {id: 'latest', name: 'APP-DEFINITIONS.FILTER.LATEST', showByDefault: true, defaultValue: true}
            ]
	    };

	    if ($rootScope.filters.forced.appDefinitionFilter) {
	        // Always recreate the filter and add all properties
	        $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
	        $rootScope.filters.appDefinitionFilter = $scope.filter;

	        for (var prop in $rootScope.filters.forced.appDefinitionFilter) {
	            $scope.filter.addProperty({id: prop}, $rootScope.filters.forced.appDefinitionFilter[prop]);
	            if (prop == 'deploymentId') {
	                $scope.filter.removeProperty({id: 'latest'});
	            }
	        }

	        $rootScope.filters.forced.appDefinitionFilter = undefined;

	    } else if ($rootScope.filters && $rootScope.filters.appDefinitionFilter) {
	    	// Reuse the existing filter
	    	$scope.filter = $rootScope.filters.appDefinitionFilter;
	    	$scope.filter.config = filterConfig;
	    } else {
		    $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
		    $rootScope.filters.appDefinitionFilter = $scope.filter;
	    }

	    $scope.definitionSelected = function(definition) {
	    	if (definition && definition.getProperty('id')) {
	    		$location.path('/app-definition/' + definition.getProperty('id'));
	    	}
	    };

	    $scope.toggleLatestVersion = function() {
	      if($scope.filter.properties.latest === true) {
	        $scope.filter.properties.nameLike = undefined;
	        $scope.filter.properties.deploymentId = undefined;
	      }

	      $scope.filter.refresh();
	    };

	    $q.all([$translate('APP-DEFINITIONS.HEADER.ID'),
              $translate('APP-DEFINITIONS.HEADER.NAME'),
              $translate('APP-DEFINITIONS.HEADER.VERSION'),
              $translate('APP-DEFINITIONS.HEADER.KEY'),
              $translate('APP-DEFINITIONS.HEADER.TENANT')])
              .then(function(headers) {

          // Config for grid
          $scope.gridDefinitions = {
              data: 'appDefinitionsData.data',
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
