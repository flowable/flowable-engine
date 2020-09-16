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
angular.module('flowableModeler')
  .controller('AppDefinitionCtrl', ['$rootScope', '$scope', '$translate', '$http', '$location', '$routeParams', '$modal', '$popover', '$timeout',
                              function ($rootScope, $scope, $translate, $http, $location, $routeParams, $modal, $popover, $timeout) {

    // Main page (needed for visual indicator of current page)
    $rootScope.setMainPageById('apps');

    // Initialize model
    $scope.model = {
        // Store the main model id, this points to the current version of a model,
        // even when we're showing history
        latestModelId: $routeParams.modelId,
        activeTab: 'bpmn'
    };
    
    $scope.tabs = [
        {
            id: 'bpmn',
            title: 'BPMN models'
        },
        {
            id: 'cmmn',
            title: 'CMMN models'
        }
    ];

    $scope.loadApp = function() {
    	var url;
    	var definitionUrl;

    	if ($routeParams.modelHistoryId) {
    		url = FLOWABLE.APP_URL.getModelHistoryUrl($routeParams.modelId, $routeParams.modelHistoryId);
    		definitionUrl = FLOWABLE.APP_URL.getAppDefinitionHistoryUrl($routeParams.modelId, $routeParams.modelHistoryId);
    	} else {
    		url = FLOWABLE.APP_URL.getModelUrl($routeParams.modelId);
    		definitionUrl = FLOWABLE.APP_URL.getAppDefinitionUrl($routeParams.modelId);

    		$scope.model.appExportUrl = FLOWABLE.APP_URL.getAppDefinitionExportUrl($routeParams.modelId);

    		$scope.model.appBarExportUrl = FLOWABLE.APP_URL.getAppDefinitionBarExportUrl($routeParams.modelId);
    	}

    	$http({method: 'GET', url: url}).
        	success(function(data, status, headers, config) {
        		$scope.model.app = data;
        		$scope.loadVersions();

        	}).error(function(data, status, headers, config) {
        		$scope.returnToList();
        	});

    	$http({method: 'GET', url: definitionUrl}).
            success(function(data, status, headers, config) {
                $scope.model.appDefinition = data;
            });
    };

    $scope.useAsNewVersion = function() {
        _internalCreateModal({
    		template: 'views/popup/model-use-as-new-version.html',
    		scope: $scope
    	}, $modal, $scope);
    };

    $scope.loadVersions = function() {

      var params = {
        includeLatestVersion: !$scope.model.app.latestVersion
      };

      $http({method: 'GET', url: FLOWABLE.APP_URL.getModelHistoriesUrl($scope.model.latestModelId), params: params}).
	      success(function(data, status, headers, config) {
	        if ($scope.model.app.latestVersion) {
	          if (!data.data) {
	            data.data = [];
	          }
	          data.data.unshift($scope.model.app);
	        }

	        $scope.model.versions = data;
	      });
    };

    $scope.showVersion = function(version) {
      if (version) {
        if (version.latestVersion) {
            $location.path("/apps/" +  $scope.model.latestModelId);
        } else {
          // Show latest version, no history-suffix needed in URL
          $location.path("/apps/" +  $scope.model.latestModelId + "/history/" + version.id);
        }
      }
    };

    $scope.returnToList = function() {
        $location.path("/apps/");
    };

    $scope.openEditor = function() {
        $location.path("/app-editor/" + $scope.model.latestModelId);
    };

    $scope.editApp = function() {
        _internalCreateModal({
    		template: 'views/popup/model-edit.html',
	        scope: $scope
    	}, $modal, $scope);
    };

    $scope.duplicateApp = function () {
      var modalInstance = _internalCreateModal({
          template: 'views/popup/app-definition-duplicate.html?version=' + Date.now()
      }, $modal, $scope);

      modalInstance.$scope.originalModel = $scope.model;
    };


    $scope.deleteApp = function() {
        // User is owner of the app definition and the app definition is deployed
        /*_internalCreateModal({
            template: 'views/popup/app-definition-delete.html?version=' + Date.now(),
            scope: $scope
        }, $modal, $scope);*/

      	_internalCreateModal({
            template: 'views/popup/model-delete.html?version=' + Date.now(),
            scope: $scope
        }, $modal, $scope);
    };

    $scope.publish = function() {
        _internalCreateModal({
            template: 'views/popup/app-definition-publish.html?version=' + Date.now(),
            scope: $scope
        }, $modal, $scope);
    };

    $scope.shareApp = function() {
        _internalCreateModal({
    		template: 'views/popup/model-share.html?version=' + Date.now(),
    		scope: $scope
    	}, $modal, $scope);
    };

    $scope.importAppDefinition = function () {
        _internalCreateModal({
            template: 'views/popup/app-definition-import.html?version=' + Date.now(),
            scope: $scope
        }, $modal, $scope);
    };

      $scope.toggleHistory = function($event) {
        if(!$scope.historyState) {
          var state = {};
          $scope.historyState = state;

          // Create popover
          state.popover = $popover(angular.element($event.target), {
            template: 'views/popover/history.html',
            placement: 'bottom-right',
            show: true,
            scope: $scope,
            container: 'body'
          });

          var destroy = function() {
            state.popover.destroy();
            delete $scope.historyState;
          };

          // When popup is hidden or scope is destroyed, hide popup
          state.popover.$scope.$on('tooltip.hide', destroy);
          $scope.$on('$destroy', destroy);
        }
      };

    $scope.loadApp();
}]);

angular.module('flowableModeler')
.controller('PublishAppDefinitionPopupCtrl', ['$rootScope', '$scope', '$http', '$route', '$translate', function ($rootScope, $scope, $http, $route, $translate) {

    $scope.popup = {
        loading: false,
        comment: ''
    };

    $scope.ok = function (force) {
        $scope.popup.loading = true;
        var data = {
            comment: $scope.popup.comment
        };

        if (force !== undefined && force !== null && force === true) {
            data.force = true;
        }

        delete $scope.popup.error;

        $http({method: 'POST', url: FLOWABLE.APP_URL.getAppDefinitionPublishUrl($scope.model.app.id), data: data}).
            success(function(data, status, headers, config) {
                $scope.$hide();

                if (data.error) {
                    $scope.popup.loading = false;
                    $scope.addAlert(data.errorDescription, 'error');
                } else {
                    $scope.popup.loading = false;
                    $route.reload();
                    $scope.addAlertPromise($translate('APP.ALERT.PUBLISH-CONFIRM'), 'info');
                }
            }).
            error(function(data, status, headers, config) {
                $scope.popup.loading = false;
                $scope.$hide();
                $scope.addAlertPromise($translate('APP.ALERT.PUBLISH-ERROR'), 'error');
            });
    };

    $scope.cancel = function () {
        if (!$scope.popup.loading) {
            $scope.$hide();
        }
    };
}]);

angular.module('flowableModeler')
.controller('DeleteAppDefinitionPopupCtrl', ['$rootScope', '$scope', '$http', '$translate', function ($rootScope, $scope, $http, $translate) {

    $scope.popup = {
        loading: false,
        cascade: 'false'
    };

    $scope.ok = function () {
        $scope.popup.loading = true;
        var params = {
            // Explicit string-check because radio-values cannot be js-booleans
            cascade : $scope.popup.cascade === 'true',
            deleteRuntimeApp: true
        };

        $http({method: 'DELETE', url: FLOWABLE.APP_URL.getModelUrl($scope.model.app.id), params: params}).
            success(function(data, status, headers, config) {
                $scope.$hide();
                $scope.popup.loading = false;
                $scope.addAlertPromise($translate('APP.ALERT.DELETE-CONFIRM'), 'info');
                $scope.returnToList();
            }).
            error(function(data, status, headers, config) {
                $scope.$hide();
                $scope.popup.loading = false;
            });
    };

    $scope.cancel = function () {
        if (!$scope.popup.loading) {
            $scope.$hide();
        }
    };
}]);

angular.module('flowableModeler')
.controller('ImportNewVersionAppDefinitionCtrl', ['$rootScope', '$scope', '$http', 'Upload', '$route', function ($rootScope, $scope, $http, Upload, $route) {

  $scope.popup = {
       loading: false,
       renewIdmIds: false
  };

  $scope.onFileSelect = function($files, isIE) {

      $scope.popup.loading = true;

      for (var i = 0; i < $files.length; i++) {
          var file = $files[i];

          var url;
          if (isIE) {
              url = FLOWABLE.APP_URL.getAppDefinitionModelTextImportUrl($scope.model.app.id, $scope.popup.renewIdmIds);
          } else {
              url = FLOWABLE.APP_URL.getAppDefinitionModelImportUrl($scope.model.app.id, $scope.popup.renewIdmIds);
          }

          Upload.upload({
              url: url,
              method: 'POST',
              file: file
          }).progress(function(evt) {
              $scope.popup.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);

          }).success(function(data, status, headers, config) {
              $scope.popup.loading = false;

              $route.reload();
              $scope.$hide();

          }).error(function(data, status, headers, config) {

              if (data && data.message) {
                  $scope.popup.errorMessage = data.message;
              }

              $scope.popup.error = true;
              $scope.popup.loading = false;
          });
      }
  };

  $scope.cancel = function () {
      if (!$scope.popup.loading) {
          $scope.$hide();
      }
  };
}]);
