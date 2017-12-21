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

flowableAdminApp.controller('FormDeploymentsController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', 'gridConstants',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, gridConstants) {

		$rootScope.navigation = {main: 'form-engine', sub: 'deployments'};
        
		$scope.filter = {};
		$scope.formDeploymentsData = {};

		// Array to contain selected properties (yes - we only can select one, but ng-grid isn't smart enough)
	    $scope.selectedDeployments = [];

	    var filterConfig = {
	    	url: '/app/rest/admin/form-deployments',
	    	method: 'GET',
	    	success: function(data, status, headers, config) {
	    		$scope.formDeploymentsData = data;
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
                {name: 'FORM-DEPLOYMENTS.SORT.ID', id: 'id'},
                {name: 'FORM-DEPLOYMENTS.SORT.NAME', id: 'name'}
            ],

            supportedProperties: [
                {id: 'nameLike', name: 'FORM-DEPLOYMENTS.FILTER.NAME', showByDefault: true},
                {id: 'tenantIdLike', name: 'FORM-DEPLOYMENTS.FILTER.TENANTID', showByDefault: true}
            ]
	    };

	    if($rootScope.filters && $rootScope.filters.formDeploymentFilter) {
	    	// Reuse the existing filter
	    	$scope.filter = $rootScope.filters.formDeploymentFilter;
	    	$scope.filter.config = filterConfig;
	    } else {
		    $scope.filter = new FlowableAdmin.Utils.Filter(filterConfig, $http, $timeout, $rootScope);
		    $rootScope.filters.formDeploymentFilter = $scope.filter;
	    }

	    $scope.deploymentSelected = function(deployment) {
	    	if (deployment && deployment.getProperty('id')) {
	    		$location.path('/form-deployment/' + deployment.getProperty('id'));
	    	}
	    };

	    $q.all([$translate('FORM-DEPLOYMENTS.HEADER.ID'),
              $translate('FORM-DEPLOYMENTS.HEADER.NAME'),
              $translate('FORM-DEPLOYMENTS.HEADER.DEPLOY-TIME'),
              $translate('FORM-DEPLOYMENTS.HEADER.CATEGORY'),
              $translate('FORM-DEPLOYMENTS.HEADER.TENANT')])
              .then(function(headers) {

          // Config for grid
          $scope.gridFormDeployments = {
              data: 'formDeploymentsData.data',
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


      /*
       * ACTIONS
       */


      $scope.uploadDeployment = function () {
          var modalInstance = $modal.open({
              templateUrl: 'views/upload-deployment.html',
              controller: 'UploadFormDeploymentCtrl'
          });
          modalInstance.result.then(function (result) {
              // Refresh page if closed successfully
              if (result) {
                  $scope.deploymentsData = {};
                  $scope.filter.refresh();
              }
          });
      };


    }]);


/**\
 * Controller for the upload a model from the process Modeler.
 */
 flowableAdminApp.controller('UploadFormDeploymentCtrl',
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
