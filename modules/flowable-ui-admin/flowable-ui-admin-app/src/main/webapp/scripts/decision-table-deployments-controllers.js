/* Licensed under the Apache License, Version 2.0 (the "License");
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

flowableAdminApp.controller('DecisionTableDeploymentsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

		$rootScope.navigation = {main: 'dmn-engine', sub: 'deployments'};
        
		$scope.filter = {};
		$scope.deploymentsData = {};

		// Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
	    $scope.selectedDefinitions = [];

	    var filterConfig = {
	    	url: '/app/rest/admin/decision-table-deployments',
	    	method: 'GET',
	    	success: function(data, status, headers, config) {
	    		$scope.deploymentsData = data;
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
                {name: 'DECISION-TABLE-DEPLOYMENTS.SORT.ID', id: 'id'},
                {name: 'DECISION-TABLE-DEPLOYMENTS.SORT.NAME', id: 'name'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'DECISION-TABLE-DEPLOYMENTS.FILTER.NAME', showByDefault: true},
                {id: 'tenantIdLike', name: 'DECISION-TABLE-DEPLOYMENTS.FILTER.TENANTID', showByDefault: true}
            ]
	    };

	    if($rootScope.filters && $rootScope.filters.decisionTableDeploymentFilter) {
	    	// Reuse the existing filter
	    	$scope.filter = $rootScope.filters.decisionTableDeploymentFilter;
	    	$scope.filter.config = filterConfig;
	    } else {
		    $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
		    $rootScope.filters.decisionTableDeploymentFilter = $scope.filter;
	    }

	    $scope.deploymentSelected = function(deployment) {
	    	if (deployment && deployment.getProperty('id')) {
	    		$location.path('/decision-table-deployment/' + deployment.getProperty('id'));
	    	}
	    };

	    $q.all([$translate('DECISION-TABLE-DEPLOYMENTS.HEADER.ID'),
              $translate('DECISION-TABLE-DEPLOYMENTS.HEADER.NAME'),
              $translate('DECISION-TABLE-DEPLOYMENTS.HEADER.DEPLOY-TIME'),
              $translate('DECISION-TABLE-DEPLOYMENTS.HEADER.CATEGORY'),
              $translate('DECISION-TABLE-DEPLOYMENTS.HEADER.TENANT')])
              .then(function(headers) {

          // Config for grid
          $scope.gridDeployments = {
              data: 'deploymentsData.data',
              enableRowReordering: true,
              multiSelect: false,
              keepLastSelected : false,
              rowHeight: 36,
              afterSelectionChange: $scope.deploymentSelected,
              columnDefs: [
                  { field: 'id', displayName: headers[0], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'name', displayName: headers[1], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'deploymentTime', displayName: headers[2], cellTemplate: gridConstants.dateTemplate},
                  { field: 'category', displayName: headers[3], cellTemplate: gridConstants.defaultTemplate},
                  { field: 'tenantId', displayName: headers[4], cellTemplate: gridConstants.defaultTemplate}]
          };
      });

      $scope.executeWhenReady(function() {
          $scope.filter.refresh();
      });

    }]);


/**\
 * Controller for the upload a model from the process Modeler.
 */
 flowableAdminApp.controller('UploadDecisionDeploymentCtrl',
    ['$scope', '$modalInstance', '$http', '$upload', function ($scope, $modalInstance, $http, $upload) {

    $scope.status = {loading: false};

    $scope.model = {};

    $scope.onFileSelect = function($files) {

        for (var i = 0; i < $files.length; i++) {
            var file = $files[i];
            $upload.upload({
                url: '/app/rest/admin/deployments',
                method: 'POST',
                file: file
            }).progress(function(evt) {
                    $scope.status.loading = true;
                    $scope.model.uploadProgress =  parseInt(100.0 * evt.loaded / evt.total);
                }).success(function(data, status, headers, config) {
                    $scope.status.loading = false;
                    $modalInstance.close(true);
                })
            .error(function(data, status, headers, config) {

                    if (data && data.message) {
                        $scope.model.errorMessage = data.message;
                    }

                    $scope.model.error = true;
                    $scope.status.loading = false;
                });
        }
    };

    $scope.cancel = function () {
        if (!$scope.status.loading) {
            $modalInstance.dismiss('cancel');
        }
    };

}]);
