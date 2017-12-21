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

flowableAdminApp.controller('ContentItemController', ['$rootScope', '$scope', '$http', '$timeout', '$location', '$translate', '$q', '$modal', '$routeParams',
    function ($rootScope, $scope, $http, $timeout, $location, $translate, $q, $modal, $routeParams) {

        $rootScope.navigation = {main: 'content-engine', sub: 'content-items'};

        $scope.returnToList = function () {
            $location.path("/content-items");
        };

        $scope.showContentItem = function () {
            $modal.open({
                templateUrl: 'views/form-render-popup.html',
                windowClass: 'modal modal-full-width',
                controller: 'ShowFormRenderPopupCtrl',
                resolve: {
                    form: function () {
                        return $scope.contentItem;
                    }
                }
            });
        };

        $scope.executeWhenReady(function () {
            if ($rootScope.contentItem) {
                $scope.contentItem = $rootScope.contentItem;
                $rootScope.contentItem = undefined;
                return;
            }

            // Load submitted form
            $http({method: 'GET', url: '/app/rest/admin/content-items/' + $routeParams.contentItemId}).
            success(function (data, status, headers, config) {
                $scope.contentItem = data;
            }).
            error(function (data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
        });
    }]);
