/*
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
'use strict';

/* Controllers */

activitiAdminApp.controller('EngineController', ['$rootScope', '$scope', '$http', '$timeout', '$modal', '$translate',
    function ($rootScope, $scope, $http, $timeout, $modal, $translate) {

        // Set root navigation
        $rootScope.navigation = {selection: 'engine'};

        // Static data
        $scope.options = {
            schemaUpdate: ['true', 'false'],
            history: ['none', 'activity', 'audit', 'full']
        };

        // Empty model
        $scope.model = {};


        // Show popup to edit the Flowable endpoint
        $scope.editProcessEndpointConfig = function () {
            editEndpointConfig(1);
        };

        $scope.editDmnEndpointConfig = function () {
            editEndpointConfig(2);
        };

        $scope.editFormEndpointConfig = function () {
            editEndpointConfig(3);
        };

        var editEndpointConfig = function (endpointType) {

            var selectedServer;

            if (endpointType === 1) {
                selectedServer = $rootScope.activeServers['process'];
            } else if (endpointType === 2) {
                selectedServer = $rootScope.activeServers['dmn'];
            } else if (endpointType === 3) {
                selectedServer = $rootScope.activeServers['form'];
            }

            if (selectedServer) {
                showEditpointConfigModel(selectedServer);
            } else {
                // load default endpoint configs properties
                $http({method: 'GET', url: '/app/rest/server-configs/default/'+endpointType}).
                success(function(defaultServerconfig, status, headers, config) {
                    showEditpointConfigModel(defaultServerconfig);
                });
            }

            function showEditpointConfigModel(server) {
                var cloneOfModel = {};
                for (var prop in server) {
                    cloneOfModel[prop] = server[prop];
                }

                var modalInstance = $modal.open({
                    templateUrl: 'views/engine-edit-endpoint-popup.html',
                    controller: 'EditEndpointConfigModalInstanceCrtl',
                    resolve: {
                        server: function () {
                            return cloneOfModel;
                        }
                    }
                });

                modalInstance.result.then(function (result) {
                    if (result) {
                        $scope.addAlert($translate.instant('ALERT.ENGINE.ENDPOINT-UPDATED', result), 'info');
                        if (endpointType === 1) {
                            $rootScope.activeServers['process'] = result;
                        } else if (endpointType === 2) {
                            $rootScope.activeServers['dmn'] = result;
                        } else if (endpointType === 3) {
                            $rootScope.activeServers['form'] = result;
                        }
                    }
                });
            }
        };

        $scope.checkProcessEndpointConfig = function () {
            checkEndpointConfig(1);
        };

        $scope.checkDmnEndpointConfig = function () {
            checkEndpointConfig(2);
        };

        $scope.checkFormEndpointConfig = function () {
            checkEndpointConfig(3);
        };

        var checkEndpointConfig = function (endpointType) {
            $http({
                method: 'GET',
                url: '/app/rest/activiti/engine-info/'+endpointType,
                ignoreErrors: true
            }).success(function (data) {
                $scope.addAlert($translate.instant('ALERT.ENGINE.ENDPOINT-VALID', data), 'info');
            }).error(function () {
                $scope.addAlert($translate.instant('ALERT.ENGINE.ENDPOINT-INVALID', $rootScope.activeServer), 'error');
            });
        };
    }]);


activitiAdminApp.controller('EditEndpointConfigModalInstanceCrtl',
    ['$scope', '$modalInstance', '$http', 'server', function ($scope, $modalInstance, $http, server) {

        $scope.model = {server: server};

        $scope.status = {loading: false};

        $scope.ok = function () {
            $scope.status.loading = true;

            delete $scope.model.error;

            var serverConfigUrl = '/app/rest/server-configs';
            var method = 'PUT';
            if ($scope.model.server && $scope.model.server.id) {
                serverConfigUrl += '/' + $scope.model.server.id;
            } else {
                method = 'POST';
            }

            $http({
                method: method,
                url: serverConfigUrl,
                data: $scope.model.server
            }).success(function (data, status, headers, config) {
                $scope.status.loading = false;
                $modalInstance.close($scope.model.server);
            }).error(function (data, status, headers, config) {
                $scope.status.loading = false;
                $scope.model.error = {
                    statusCode: status,
                    message: data
                };
            });
        };

        $scope.cancel = function () {
            if (!$scope.status.loading) {
                $modalInstance.dismiss('cancel');
            }
        };
    }]);
