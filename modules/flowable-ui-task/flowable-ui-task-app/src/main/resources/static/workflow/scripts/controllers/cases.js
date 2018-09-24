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

angular.module('flowableApp')
    .controller('CasesController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$modal', '$routeParams', '$popover', 'appResourceRoot', 'AppDefinitionService', 'CaseService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, $routeParams, $popover, appResourceRoot, AppDefinitionService, CaseService) {

        var caseId = $routeParams.caseId;

        // Ensure correct main page is set
        $rootScope.setMainPageById('cases');

        // Initialize model
        $scope.model = {
            page: 0,
            initialLoad: false,
            mode: 'case-list'
        };

        $scope.model.contentSummary = {
            loading: false
        };

         $scope.model.runtimeSorts = [
            { 'id': 'created-desc', 'title': 'CASE.FILTER.CREATED-DESC'},
            { 'id': 'created-asc', 'title': 'CASE.FILTER.CREATED-ASC' }
        ];

        $scope.model.completedSorts = [];
        $scope.model.completedSorts.push($scope.model.runtimeSorts[0]); // needs to be same reference!
        $scope.model.completedSorts.push($scope.model.runtimeSorts[1]); // needs to be same reference!
        $scope.model.completedSorts.push({ 'id': 'ended-asc', 'title': 'CASE.FILTER.ENDED-DESC' });
        $scope.model.completedSorts.push({ 'id': 'ended-desc', 'title': 'CASE.FILTER.ENDED-ASC' });

        $scope.model.sorts = $scope.model.runtimeSorts;

        $scope.model.stateFilterOptions = [
            { 'id': 'running', 'title': 'CASE.FILTER.STATE-RUNNING' },
            { 'id': 'completed', 'title': 'CASE.FILTER.STATE-COMPLETED' },
            { 'id': 'all', 'title': 'CASE.FILTER.STATE-ALL' }
        ];

        $scope.model.filter = {
            loading: false,
            expanded: false,
            param: {
                state: $scope.model.stateFilterOptions[0],
                sort: $scope.model.sorts[0].id
            }
        };

        $scope.appDefinitionKey = $routeParams.appDefinitionKey;
        $scope.missingAppdefinition = $scope.appDefinitionKey === false;

        // In case of viewing case instances in an app-context, need to make filter aware of this
        $scope.model.filter.param.appDefinitionKey = $scope.appDefinitionKey;

        // The filter is stored on the rootScope, which allows the user to switch back and forth without losing the filter.
        if ($rootScope.caseFilter !== null && $rootScope.caseFilter !== undefined) {
            $scope.model.filter.param = $rootScope.caseFilter.param;
        } else {
            $rootScope.caseFilter = { param: $scope.model.filter.param }
        }

        // Update app on rootScope. If app id present, it will fetch definition if not already fetched to update view and navigation accordingly
        AppDefinitionService.setActiveAppDefinitionKey($scope.appDefinitionKey);

        $scope.selectCaseInstance = function (caseInstance) {
            $scope.selectedCaseInstance = caseInstance;
            $scope.state = {noCases:false};
        };

        $scope.expandFilter = function () {
            $scope.model.filter.expanded = true;
        };

        $scope.collapseFilter = function () {
            $scope.model.filter.expanded = false;
        };

        $scope.$watch("model.filter.param", function (newValue) {
            if (newValue) {
                if ($scope.model.initialLoad) {
                    $scope.loadCaseInstances();
                }

                if (newValue.state.id === 'completed' || newValue.state.id === 'all') {
                    $scope.model.sorts = $scope.model.completedSorts;
                } else {
                    $scope.model.sorts = $scope.model.runtimeSorts;
                    if (newValue.sort === 'ended-asc' || newValue.sort === 'ended-desc') {
                        $scope.model.filter.param.sort = $scope.model.sorts[0].id;
                    }
                }
            }
        }, true);

        $scope.nextPage = function () {
            $scope.loadCaseInstances(true);
        };

        // TODO: move to service
        $scope.loadCaseInstances = function (nextPage) {

            $scope.model.filter.loading = true;

            var params = $scope.model.filter.param;

            if (nextPage) {
                $scope.model.page += 1;
            } else {
                $scope.model.page = 0;
            }

            var instanceQueryData = {
                sort: params.sort,
                page: $scope.model.page
            };

            if (params.appDefinitionKey) {
                instanceQueryData.appDefinitionKey = params.appDefinitionKey;
            }

            if (params.state) {
                instanceQueryData.state = params.state.id;
            }


            $http({method: 'POST', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/query/case-instances', data: instanceQueryData}).
                success(function (response, status, headers, config) {
                    $scope.model.initialLoad = true;
                    var instances = response.data;

                    if (response.start > 0) {
                        // Add results instead of removing existing ones
                        for (var i = 0; i < instances.length; i++) {
                            $scope.model.caseInstances.push(instances[i]);
                        }

                        $scope.state = {noCases: false};
                    } else {
                        $scope.model.caseInstances = instances;
                        $scope.state = {noCases: (!response.data || response.data.length == 0)};
                    }

                    if (response.start + response.size < response.total) {
                        // More pages available
                        $scope.model.hasNextPage = true;
                    } else {
                        $scope.model.hasNextPage = false;
                    }

                    var isSelected = false;

                    if ($rootScope.root.selectedCaseId) {
                        for (var i = 0; i < instances.length; i++) {
                            if (instances[i].id == $rootScope.root.selectedCaseId) {
                                isSelected = true;
                                $scope.selectedCaseInstance = instances[i];
                                break;
                            }
                        }
                        $rootScope.root.selectedCaseId = undefined;
                    }
                    if (!isSelected && instances.length > 0) {
                        if (!$scope.selectedCaseInstance) {
                            $scope.selectedCaseInstance = instances[0];
                        }
                    }

                    // If there is a new case instance, we want it to be selected
                    if ($scope.newCaseInstance !== null && $scope.newCaseInstance !== undefined) {
                        if ($scope.newCaseInstance.id !== null && $scope.newCaseInstance.id !== undefined) {
                            for (var instanceIndex = 0; instanceIndex < $scope.model.caseInstances.length; instanceIndex++) {
                                if ($scope.model.caseInstances[instanceIndex].id === $scope.newCaseInstance.id) {
                                    $scope.selectedCaseInstance = $scope.model.caseInstances[instanceIndex];
                                    break;
                                }
                            }
                        }
                       // Always reset when loading case instance
                        $scope.newCaseInstance = undefined;
                    }

                    $scope.model.filter.loading = false;
                    $rootScope.window.forceRefresh = true;
                }).
                error(function (response, status, headers, config) {
                    console.log('Something went wrong: ' + response);
                });
        };

            $scope.selectCaseDefinition = function (definition) {
            $scope.newCaseInstance.caseDefinitionId = definition.id;
            $scope.newCaseInstance.name = definition.name + ' - ' + new moment().format('MMMM Do YYYY');
            $scope.newCaseInstance.caseDefinition = definition;

            $timeout(function () {
                angular.element('#start-case-name').focus();
            }, 20);
        };

            $scope.dragOverContent = function (over) {
                if (over && !$scope.model.contentSummary.addContent) {
                    $scope.model.contentSummary.addContent = true;
                }
            };

            $scope.toggleCreateContent = function () {
                $scope.model.contentSummary.addContent = !$scope.model.contentSummary.addContent;
            };

            $scope.onContentUploaded = function (content) {
                if ($scope.model.content && $scope.model.content.data) {
                    $scope.model.content.data.push(content);
                    RelatedContentService.addUrlToContent(content);
                    $scope.model.selectedContent = content;
                }
                $rootScope.addAlertPromise($translate('TASK.ALERT.RELATED-CONTENT-ADDED', content), 'info');
                $scope.toggleCreateContent();
            };

            $scope.onContentDeleted = function (content) {
                if ($scope.model.content && $scope.model.content.data) {
                    $scope.model.content.data.forEach(function (value, i, arr) {
                        if (content === value) {
                            arr.splice(i, 1);
                        }
                    })
                }
            };

            $scope.selectContent = function (content) {
                if ($scope.model.selectedContent == content) {
                    $scope.model.selectedContent = undefined;
                } else {
                    $scope.model.selectedContent = content;
                }
            };

            $scope.selectStateFilter = function (state) {
            if (state != $scope.model.filter.param.state) {
                $scope.model.filter.param.state = state;
                $scope.collapseFilter();
                $scope.selectedCaseInstance = undefined;
            }
        };

        $scope.sortChanged = function() {
            $scope.selectedCaseInstance = undefined;
        };

        $scope.selectDefaultDefinition = function() {
            // Select first non-default definition, if any
            CaseService.getCaseDefinitions($scope.appDefinitionKey).then(function(response) {
            	$rootScope.root.caseDefinitions = response.data;
	            if ($scope.root.caseDefinitions && $scope.root.caseDefinitions.length > 0) {
	                for (var i=0; i< $scope.root.caseDefinitions.length; i++) {
	                    var def = $scope.root.caseDefinitions[i];
	                    if (def.id != 'default') {
	                        $scope.selectCaseDefinition(def);
	                        break;
	                    }
	                }
	            }
	        });

        };

        $scope.backToList = function(reloadCaseInstances) {

            $scope.newCaseInstance = undefined;

            $scope.model.mode = 'case-list';
            $scope.startFormError = undefined;

            // If param is true: reload, no questions asked
            if (reloadCaseInstances) {

                // Reset selection
                $scope.selectedCaseInstance = undefined;

                // Reset filters
                $scope.model.filter.param.state = $scope.model.stateFilterOptions[0];
                $scope.model.filter.param.sort = $scope.model.sorts[0].id;

                $scope.loadCaseInstances();
            }

            // In case we're coming from the task page, no case instances have been loaded
            if ($scope.model.caseInstances === null || $scope.model.caseInstances === undefined) {
                $scope.loadCaseInstances();
            }

        };

        $scope.createCaseInstance = function () {

            // Reset state
            $rootScope.root.showStartForm = false;

            $scope.model.mode = 'case-create';
            $scope.newCaseInstance = {};
            $scope.selectDefaultDefinition();
        };


        // Called after form is submitted
        $scope.$on('case-started', function (event, data) {
            $scope.newCaseInstance.id = data.id;
            $scope.backToList(true);
        });

        $scope.startCaseInstanceWithoutForm = function() {
            $scope.newCaseInstance.loading = true;
            var createInstanceData = {caseDefinitionId: $scope.newCaseInstance.caseDefinition.id, name: $scope.newCaseInstance.name};
            $http({method: 'POST', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances', data: createInstanceData}).
                success(function (response, status, headers, config) {
                    $scope.newCaseInstance.id = response.id;
                    $scope.newCaseInstance.loading = false;
                    $scope.backToList(true);

                }).
                error(function (response, status, headers, config) {
                    $scope.newCaseInstance.loading = false;

                    if(response && response.messageKey) {
                        $translate(response.messageKey, response.customData).then(function(message) {
                            $scope.errorMessage = message;
                            console.log(message);
                        });
                    }
                });
        };

        $rootScope.loadCaseDefinitions($scope.appDefinitionKey);

        // If 'createCaseInstance' is set (eg from the task page)
        if ($rootScope.createCaseInstance) {
            $rootScope.createCaseInstance = false;
            $scope.createCaseInstance();
        } else {
            $scope.loadCaseInstances();
        }

    }]);
