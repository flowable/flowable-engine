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
flowableApp.controller('PrivilegeMgmtController', ['$rootScope', '$scope', '$translate', '$http', '$timeout','$location', '$modal', '$popover', 'IdmService',
    function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, $popover, IdmService) {

        $rootScope.setMainPageById('privilegeMgmt');

        $scope.model = {
            loading: true,
            loadingPrivilege: false,
            expanded: {}
        };

        $scope.selectPrivilege = function(privilege) {
            $scope.model.loadingPrivilege = true;
            IdmService.getPrivilege(privilege.id).then(function(data) {
                $scope.model.selectedPrivilege = data;
                $scope.model.loadingPrivilege = false;
            });
        };

        $scope.addUserPrivilege = function(user) {
            IdmService.addUserPrivilege($scope.model.selectedPrivilege.id, user.id).then(function(data) {
                $scope.selectPrivilege($scope.model.selectedPrivilege);
            });
        };

        $scope.deleteUserPrivilege = function(user) {
            IdmService.deleteUserPrivilege($scope.model.selectedPrivilege.id, user.id).then(function(data) {
                $scope.selectPrivilege($scope.model.selectedPrivilege);
            });
        }

        $scope.addGroupPrivilege = function(group) {
            IdmService.addGroupPrivilege($scope.model.selectedPrivilege.id, group.id).then(function(data) {
                $scope.selectPrivilege($scope.model.selectedPrivilege);
            });
        };

        $scope.deleteGroupPrivilege = function(group) {
            IdmService.deleteGroupPrivilege($scope.model.selectedPrivilege.id, group.id).then(function(data) {
                $scope.selectPrivilege($scope.model.selectedPrivilege);
            });
        }

        // Load privileges when page is loaded
        IdmService.getPrivileges().then(function(data) {
            $scope.model.privileges = data;
            $scope.model.loading = false;
         });

    }]);

    

