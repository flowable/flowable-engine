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
    .controller('DecisionsController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$modal', 'modelType', function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, modelType) {

        $rootScope.setMainPageById('decisions');
        $rootScope.decisionTableItems = undefined;

        // get latest thumbnails
        $scope.imageVersion = Date.now();

        $scope.model = {
            filters: [
                {id: 'decisionTables', labelKey: 'DECISION-TABLES', type: 'decision-tables'},
                {id: 'decisionServices', labelKey: 'DECISION-SERVICES', type: 'decision-services'}
            ],

            sorts: [
                {id: 'modifiedDesc', labelKey: 'MODIFIED-DESC'},
                {id: 'modifiedAsc', labelKey: 'MODIFIED-ASC'},
                {id: 'nameAsc', labelKey: 'NAME-ASC'},
                {id: 'nameDesc', labelKey: 'NAME-DESC'}
            ]
        };

        if (modelType && modelType === 6) {
            $scope.model.activeFilter = $scope.model.filters[1];
            $scope.model.activeSort = $scope.model.sorts[0];
            $rootScope.decisionFilter = $scope.model.activeFilter;
        } else {
            $scope.model.activeFilter = $scope.model.filters[0];
            $scope.model.activeSort = $scope.model.sorts[0];
            $rootScope.decisionFilter = $scope.model.activeFilter;
        }

        $scope.activateSort = function (sort) {
            $scope.model.activeSort = sort;
        };

        $scope.importDecisionTable = function () {
            _internalCreateModal({
                template: 'views/popup/decision-table-import.html?version=' + Date.now()
            }, $modal, $scope);
        };

        $scope.importDecisionService = function () {
            _internalCreateModal({
                template: 'views/popup/decision-service-import.html?version=' + Date.now()
            }, $modal, $scope);
        };

        $scope.loadDecisionTables = function () {
            $scope.model.loading = true;

            var params = {
                filter: $scope.model.activeFilter.id,
                sort: $scope.model.activeSort.id,
                modelType: 4
            };

            if ($scope.model.filterText && $scope.model.filterText != '') {
                params.filterText = $scope.model.filterText;
            }

            $http({method: 'GET', url: FLOWABLE.APP_URL.getModelsUrl(), params: params}).success(function (data, status, headers, config) {
                $scope.model.decisions = data;
                $scope.model.loading = false;
            }).error(function (data, status, headers, config) {
                $scope.model.loading = false;
            });
        };

        $scope.loadDecisionServices = function () {
            $scope.model.loading = true;

            var params = {
                filter: $scope.model.activeFilter.id,
                sort: $scope.model.activeSort.id,
                modelType: 6
            };

            if ($scope.model.filterText && $scope.model.filterText != '') {
                params.filterText = $scope.model.filterText;
            }

            $http({method: 'GET', url: FLOWABLE.APP_URL.getModelsUrl(), params: params}).success(function (data, status, headers, config) {
                $scope.model.decisions = data;
                $scope.model.loading = false;
            }).error(function (data, status, headers, config) {
                $scope.model.loading = false;
            });
        };

        var timeoutFilter = function () {
            $scope.model.isFilterDelayed = true;
            $timeout(function () {
                $scope.model.isFilterDelayed = false;
                if ($scope.model.isFilterUpdated) {
                    $scope.model.isFilterUpdated = false;
                    timeoutFilter();
                } else {
                    $scope.model.filterText = $scope.model.pendingFilterText;
                    $rootScope.decisionFilter.filterText = $scope.model.filterText;
                    $scope.loadDecisionTables();
                }
            }, 500);
        };

        $scope.filterDelayed = function () {
            if ($scope.model.isFilterDelayed) {
                $scope.model.isFilterUpdated = true;
            } else {
                timeoutFilter();
            }
        };

        $scope.createDecision = function () {
            if ($scope.model.activeFilter.type === "decision-services") {
                return $scope.createDecisionService();
            } else {
                return $scope.createDecisionTable();
            }
        }

        $scope.createDecisionTable = function () {
            $rootScope.currentKickstartModel = undefined;
            $rootScope.currentDecisionTableModel = undefined;
            $scope.createDecisionTableCallback = function (result) {
                $rootScope.editorHistory = [];
                $location.url("/decision-table-editor/" + encodeURIComponent(result.id));
            };

            _internalCreateModal({
                template: 'views/popup/decision-table-create.html?version=' + Date.now(),
                scope: $scope
            }, $modal, $scope);
        };

        $scope.createDecisionService = function () {
            $rootScope.currentKickstartModel = undefined;
            $rootScope.currentDRDModel = undefined;
            $scope.createDecisionServiceCallback = function (result) {
                $rootScope.editorHistory = [];
                $location.url("/decision-service-editor/" + encodeURIComponent(result.id));
            };

            _internalCreateModal({
                template: 'views/popup/decision-service-create.html?version=' + Date.now(),
                scope: $scope
            }, $modal, $scope);
        };


        $scope.showDecisionDetails = function (decision) {
            if (decision) {
                $rootScope.editorHistory = [];
                $rootScope.currentKickstartModel = undefined;
                if (decision.modelType === 4) {
                    $location.url("/decision-tables/" + encodeURIComponent(decision.id));
                } else if (decision.modelType === 6) {
                    $location.url("/decision-services/" + encodeURIComponent(decision.id));
                }
            }
        };

        $scope.editDecisionDetails = function (decision) {
            if (decision) {
                $rootScope.editorHistory = [];
                if (decision.modelType === 4) {
                    $location.url("/decision-table-editor/" + encodeURIComponent(decision.id));
                } else if (decision.modelType === 6) {
                    $location.url("/decision-service-editor/" + encodeURIComponent(decision.id));
                }
            }
        };

        if ($rootScope.decisionFilter &&
			$rootScope.decisionFilter &&
			$rootScope.decisionFilter.id === 'decisionServices') {
            $scope.loadDecisionServices();
        } else {
			$scope.loadDecisionTables();
        }
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

            $http({method: 'POST', url: FLOWABLE.APP_URL.getModelsUrl(), data: $scope.model.decisionTable}).success(function (data, status, headers, config) {
                $scope.$hide();
                $scope.model.loading = false;

                if ($scope.createDecisionTableCallback) {
                    $scope.createDecisionTableCallback(data);
                    $scope.createDecisionTableCallback = undefined;
                }

            }).error(function (data, status, headers, config) {
                $scope.model.loading = false;
                $scope.model.errorMessage = data.message;
            });
        };

        $scope.cancel = function () {
            if (!$scope.model.loading) {
                $scope.$hide();
            }
        };
    }]);

angular.module('flowableModeler')
    .controller('CreateNewDecisionServiceCtrl', ['$rootScope', '$scope', '$http', function ($rootScope, $scope, $http) {

        $scope.model = {
            loading: false,
            decisionService: {
                name: '',
                key: '',
                description: '',
                modelType: 6
            }
        };

        $scope.ok = function () {

            if (!$scope.model.decisionService.name || $scope.model.decisionService.name.length == 0 ||
                !$scope.model.decisionService.key || $scope.model.decisionService.key.length == 0) {

                return;
            }

            $scope.model.loading = true;

            $http({method: 'POST', url: FLOWABLE.APP_URL.getModelsUrl(), data: $scope.model.decisionService}).success(function (data, status, headers, config) {
                $scope.$hide();
                $scope.model.loading = false;

                if ($scope.createDecisionServiceCallback) {
                    $scope.createDecisionServiceCallback(data);
                    $scope.createDecisionServiceCallback = undefined;
                }

            }).error(function (data, status, headers, config) {
                $scope.model.loading = false;
                $scope.model.errorMessage = data.message;
            });
        };

        $scope.cancel = function () {
            if (!$scope.model.loading) {
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

                $http({
                    method: 'POST',
                    url: FLOWABLE.APP_URL.getCloneModelsUrl($scope.model.decisionTable.id),
                    data: $scope.model.decisionTable
                }).success(function (data, status, headers, config) {
                    $scope.$hide();
                    $scope.model.loading = false;

                    if ($scope.duplicateDecisionTableCallback) {
                        $scope.duplicateDecisionTableCallback(data);
                        $scope.duplicateDecisionTableCallback = undefined;
                    }

                }).error(function (data, status, headers, config) {
                    $scope.model.loading = false;
                    $scope.model.errorMessage = data.message;
                });
            };

            $scope.cancel = function () {
                if (!$scope.model.loading) {
                    $scope.$hide();
                }
            };
        }]);

angular.module('flowableModeler')
    .controller('DuplicateDecisionServiceCtrl', ['$rootScope', '$scope', '$http',
        function ($rootScope, $scope, $http) {

            $scope.model = {
                loading: false,
                decisionService: {
                    id: '',
                    name: '',
                    description: '',
                    modelType: null
                }
            };

            if ($scope.originalModel) {
                //clone the model
                $scope.model.decisionService.name = $scope.originalModel.decisionService.name;
                $scope.model.decisionService.key = $scope.originalModel.decisionService.key;
                $scope.model.decisionService.description = $scope.originalModel.decisionService.description;
                $scope.model.decisionService.modelType = $scope.originalModel.decisionService.modelType;
                $scope.model.decisionService.id = $scope.originalModel.decisionService.id;
            }

            $scope.ok = function () {

                if (!$scope.model.decisionService.name || $scope.model.decisionService.name.length == 0) {
                    return;
                }

                $scope.model.loading = true;

                $http({
                    method: 'POST',
                    url: FLOWABLE.APP_URL.getCloneModelsUrl($scope.model.decisionService.id),
                    data: $scope.model.decisionService
                }).success(function (data, status, headers, config) {
                    $scope.$hide();
                    $scope.model.loading = false;

                    if ($scope.duplicateDecisionServiceCallback) {
                        $scope.duplicateDecisionServiceCallback(data);
                        $scope.duplicateDecisionServiceCallback = undefined;
                    }

                }).error(function (data, status, headers, config) {
                    $scope.model.loading = false;
                    $scope.model.errorMessage = data.message;
                });
            };

            $scope.cancel = function () {
                if (!$scope.model.loading) {
                    $scope.$hide();
                }
            };
        }]);

angular.module('flowableModeler')
    .controller('ImportDecisionTableModelCtrl', ['$rootScope', '$scope', '$http', 'Upload', '$location', function ($rootScope, $scope, $http, Upload, $location) {

        $scope.model = {
            loading: false
        };

        $scope.onFileSelect = function ($files, isIE) {

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
                }).progress(function (evt) {
                    $scope.model.loading = true;
                    $scope.model.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);

                }).success(function (data, status, headers, config) {
                    $scope.model.loading = false;

                    $location.path("/decision-table-editor/" + data.id);
                    $scope.$hide();

                }).error(function (data, status, headers, config) {

                    if (data && data.message) {
                        $scope.model.errorMessage = data.message;
                    }

                    $scope.model.error = true;
                    $scope.model.loading = false;
                });
            }
        };

        $scope.cancel = function () {
            if (!$scope.model.loading) {
                $scope.$hide();
            }
        };
    }]);

angular.module('flowableModeler')
    .controller('ImportDecisionServiceModelCtrl', ['$rootScope', '$scope', '$http', 'Upload', '$location', function ($rootScope, $scope, $http, Upload, $location) {

        $scope.model = {
            loading: false
        };

        $scope.onFileSelect = function ($files, isIE) {

            for (var i = 0; i < $files.length; i++) {
                var file = $files[i];

                var url;
                if (isIE) {
                    url = FLOWABLE.APP_URL.getDecisionServiceTextImportUrl();
                } else {
                    url = FLOWABLE.APP_URL.getDecisionServiceImportUrl();
                }

                Upload.upload({
                    url: url,
                    method: 'POST',
                    file: file
                }).progress(function (evt) {
                    $scope.model.loading = true;
                    $scope.model.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);

                }).success(function (data, status, headers, config) {
                    $scope.model.loading = false;

                    $location.path("/decision-service-editor/" + data.id);
                    $scope.$hide();

                }).error(function (data, status, headers, config) {

                    if (data && data.message) {
                        $scope.model.errorMessage = data.message;
                    }

                    $scope.model.error = true;
                    $scope.model.loading = false;
                });
            }
        };

        $scope.cancel = function () {
            if (!$scope.model.loading) {
                $scope.$hide();
            }
        };
    }]);