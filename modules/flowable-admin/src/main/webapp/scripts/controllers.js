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

activitiAdminApp.controller('AdminController', ['$scope',
    function ($scope) {
    }]);

activitiAdminApp.controller('LoginController', ['$scope', '$location', 'AuthenticationSharedService', '$timeout',
    function ($scope, $location, AuthenticationSharedService, $timeout) {
        $scope.login = function () {
            AuthenticationSharedService.login({
                username: $scope.username,
                password: $scope.password,
                success: function () {
                }
            });
        };
        
        
        // Fix for browser auto-fill of saved passwords, by default it does not trigger a change
        // and the model is not updated see https://github.com/angular/angular.js/issues/1460
        $timeout(function() {
          jQuery('#username').trigger('change');
          jQuery('#password').trigger('change');
        }, 200);
    }]);

activitiAdminApp.controller('LogoutController', ['$location', 'AuthenticationSharedService',
    function ($location, AuthenticationSharedService) {
        AuthenticationSharedService.logout({
            success: function () {
                $location.path('');
            }
        });
    }]);

activitiAdminApp.controller('ConfirmPopupCrtl', ['$scope', '$modalInstance', 'model', 
    function ($scope, $modalInstance, model) {
        $scope.model = model;
        $scope.ok = function () {
            $modalInstance.close(true);
        };
    }]);