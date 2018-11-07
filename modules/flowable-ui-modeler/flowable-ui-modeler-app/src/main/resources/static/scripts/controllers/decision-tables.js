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

angular.module('flowableModeler')
  .controller('DecisionTablesController', ['$rootScope', '$scope', '$translate', '$http', '$timeout','$location', '$modal', function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal) {

	  $rootScope.setMainPageById('decision-tables');
	  $rootScope.decisionTableItems = undefined;

      // get latest thumbnails
      $scope.imageVersion = Date.now();

	  $scope.model = {
        filters: [
            {id: 'decisionTables', labelKey: 'DECISION-TABLES'}
		],

		sorts: [
		        {id: 'modifiedDesc', labelKey: 'MODIFIED-DESC'},
		        {id: 'modifiedAsc', labelKey: 'MODIFIED-ASC'},
		        {id: 'nameAsc', labelKey: 'NAME-ASC'},
		        {id: 'nameDesc', labelKey: 'NAME-DESC'}
		]
	  };

	  if ($rootScope.decisionTableFilter) {
		  $scope.model.activeFilter = $rootScope.decisionTableFilter.filter;
		  $scope.model.activeSort = $rootScope.decisionTableFilter.sort;
		  $scope.model.filterText = $rootScope.decisionTableFilter.filterText;
		  $scope.model.pendingFilterText = $scope.model.filterText; // The search textfield uses this

	  } else {
		  // By default, show first filter and use first sort
	      $scope.model.activeFilter = $scope.model.filters[0];
	      $scope.model.activeSort = $scope.model.sorts[0];
	      $rootScope.decisionTableFilter = {
	        filter: $scope.model.activeFilter,
	        sort: $scope.model.activeSort,
	        filterText: ''
	      };
	  }

	  $scope.activateFilter = function(filter) {
		  $scope.model.activeFilter = filter;
		  $rootScope.decisionTableFilter.filter = filter;
		  $scope.loadDecisionTables();
	  };

	  $scope.activateSort = function(sort) {
		  $scope.model.activeSort = sort;
		  $rootScope.decisionTableFilter.sort = sort;
		  $scope.loadDecisionTables();
	  };

	  $scope.importDecisionTable = function () {
          _internalCreateModal({
              template: 'views/popup/decision-table-import.html?version=' + Date.now()
          }, $modal, $scope);
      };

	  $scope.loadDecisionTables = function() {
		  $scope.model.loading = true;

		  var params = {
		      filter: $scope.model.activeFilter.id,
		      sort: $scope.model.activeSort.id,
		      modelType: 4
		  };

		  if ($scope.model.filterText && $scope.model.filterText != '') {
		    params.filterText = $scope.model.filterText;
		  }

		  $http({method: 'GET', url: FLOWABLE.APP_URL.getModelsUrl(), params: params}).
		  	success(function(data, status, headers, config) {
	    		$scope.model.decisionTables = data;
	    		$scope.model.loading = false;
	        }).
	        error(function(data, status, headers, config) {
	           $scope.model.loading = false;
	        });
	  };

	  var timeoutFilter = function() {
	    $scope.model.isFilterDelayed = true;
	    $timeout(function() {
	        $scope.model.isFilterDelayed = false;
	        if($scope.model.isFilterUpdated) {
	          $scope.model.isFilterUpdated = false;
	          timeoutFilter();
	        } else {
	          $scope.model.filterText = $scope.model.pendingFilterText;
	          $rootScope.decisionTableFilter.filterText = $scope.model.filterText;
	          $scope.loadDecisionTables();
	        }
	    }, 500);
	  };

	  $scope.filterDelayed = function() {
	    if($scope.model.isFilterDelayed) {
	      $scope.model.isFilterUpdated = true;
	    } else {
	      timeoutFilter();
	    }
	  };

	  $scope.createDecisionTable = function() {
		  $rootScope.currentKickstartModel = undefined;
	      $rootScope.currentDecisionTableModel = undefined;
		  $scope.createDecisionTableCallback = function(result) {
		      $rootScope.editorHistory = [];
		      $location.url("/decision-table-editor/" + encodeURIComponent(result.id));
		  };

          _internalCreateModal({
			  template: 'views/popup/decision-table-create.html?version=' + Date.now(),
			  scope: $scope
		  }, $modal, $scope);
	  };

	  $scope.showDecisionTableDetails = function(decisionTable) {
	      if (decisionTable) {
	      	  $rootScope.editorHistory = [];
			  $rootScope.currentKickstartModel = undefined;
	          $location.url("/decision-tables/" + encodeURIComponent(decisionTable.id));
	      }
	  };

	  $scope.editDecisionTableDetails = function(decisionTable) {
		  if (decisionTable) {
		  	  $rootScope.editorHistory = [];
			  $location.url("/decision-table-editor/" + encodeURIComponent(decisionTable.id));
		  }
	  };

	  // Finally, load initial decisionTables
	  $scope.loadDecisionTables();
  }]);


angular.module('flowableModeler')
.controller('CreateNewDecisionTableCtrl', ['$rootScope', '$scope', '$http', function ($rootScope, $scope, $http) {

    $scope.model = {
       loading: false,
       decisionTable: {
            name: '',
            key: '',
            description: '',
            modelType: 4
       }
    };

    $scope.ok = function () {

        if (!$scope.model.decisionTable.name || $scope.model.decisionTable.name.length == 0 ||
        	!$scope.model.decisionTable.key || $scope.model.decisionTable.key.length == 0) {
        	
            return;
        }

        $scope.model.loading = true;

        $http({method: 'POST', url: FLOWABLE.APP_URL.getModelsUrl(), data: $scope.model.decisionTable}).
            success(function(data, status, headers, config) {
                $scope.$hide();
                $scope.model.loading = false;

                if ($scope.createDecisionTableCallback) {
                	$scope.createDecisionTableCallback(data);
                	$scope.createDecisionTableCallback = undefined;
                }

            }).
            error(function(data, status, headers, config) {
                $scope.model.loading = false;
                $scope.model.errorMessage = data.message;
            });
    };

    $scope.cancel = function () {
        if(!$scope.model.loading) {
            $scope.$hide();
        }
    };
}]);

angular.module('flowableModeler')
	.controller('DuplicateDecisionTableCtrl', ['$rootScope', '$scope', '$http',
		function ($rootScope, $scope, $http) {

			$scope.model = {
				loading: false,
                decisionTable: {
					id: '',
					name: '',
					description: '',
                    modelType: null
				}
			};

			if ($scope.originalModel) {
				//clone the model
				$scope.model.decisionTable.name = $scope.originalModel.decisionTable.name;
				$scope.model.decisionTable.key = $scope.originalModel.decisionTable.key;
				$scope.model.decisionTable.description = $scope.originalModel.decisionTable.description;
				$scope.model.decisionTable.modelType = $scope.originalModel.decisionTable.modelType;
				$scope.model.decisionTable.id = $scope.originalModel.decisionTable.id;
			}

			$scope.ok = function () {

				if (!$scope.model.decisionTable.name || $scope.model.decisionTable.name.length == 0) {
					return;
				}

				$scope.model.loading = true;

				$http({method: 'POST', url: FLOWABLE.APP_URL.getCloneModelsUrl($scope.model.decisionTable.id), data: $scope.model.decisionTable}).
					success(function(data, status, headers, config) {
						$scope.$hide();
						$scope.model.loading = false;

						if ($scope.duplicateDecisionTableCallback) {
							$scope.duplicateDecisionTableCallback(data);
							$scope.duplicateDecisionTableCallback = undefined;
						}

					}).
					error(function(data, status, headers, config) {
						$scope.model.loading = false;
						$scope.model.errorMessage = data.message;
					});
			};

			$scope.cancel = function () {
				if(!$scope.model.loading) {
					$scope.$hide();
				}
			};
		}]);

angular.module('flowableModeler')
.controller('ImportDecisionTableModelCtrl', ['$rootScope', '$scope', '$http', 'Upload', '$location', function ($rootScope, $scope, $http, Upload, $location) {

  $scope.model = {
       loading: false
  };

  $scope.onFileSelect = function($files, isIE) {

      for (var i = 0; i < $files.length; i++) {
          var file = $files[i];

          var url;
          if (isIE) {
              url = FLOWABLE.APP_URL.getDecisionTableTextImportUrl();
          } else {
              url = FLOWABLE.APP_URL.getDecisionTableImportUrl();
          }

          Upload.upload({
              url: url,
              method: 'POST',
              file: file
          }).progress(function(evt) {
              $scope.model.loading = true;
              $scope.model.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);

          }).success(function(data, status, headers, config) {
              $scope.model.loading = false;

              $location.path("/decision-table-editor/" + data.id);
              $scope.$hide();

          }).error(function(data, status, headers, config) {

              if (data && data.message) {
                  $scope.model.errorMessage = data.message;
              }

              $scope.model.error = true;
              $scope.model.loading = false;
          });
      }
  };

  $scope.cancel = function () {
      if(!$scope.model.loading) {
          $scope.$hide();
      }
  };
}]);
