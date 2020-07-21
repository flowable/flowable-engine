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
    .controller('CaseStartFormController', ['$rootScope', '$scope', '$translate', '$http', '$timeout','$location', '$route', '$modal', '$routeParams', '$popover',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $route, $modal, $routeParams, $popover) {
   
    $scope.model.initializing = true;    
        
    $scope.$watch('selectedCaseInstance', function(newValue) {
       
        if (newValue && newValue.id && (newValue.id != $rootScope.root.selectedCaseId || !$scope.model.initializing)) {
            $scope.model.caseInstance = newValue;

            $scope.getCaseInstance(newValue.id);
            $rootScope.root.showStartForm = false;
            $scope.model.formData = undefined;
            $scope.model.initializing = false;
        }
    });
        
    $scope.getCaseInstance = function(caseInstanceId) {
        $http({method: 'GET', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId}).
            success(function(response, status, headers, config) {
                $scope.model.caseInstance = response;
                $scope.loadStartForm(caseInstanceId);
            }).
            error(function(response, status, headers, config) {
                console.log('Something went wrong: ' + response);
            });
    };

    $scope.loadStartForm = function(caseInstanceId) {
        $http({method: 'GET', url: FLOWABLE.CONFIG.contextRoot + '/app/rest/case-instances/' + caseInstanceId + '/start-form'}).
            success(function(response, status, headers, config) {
                $scope.model.formData = response;
            }).
            error(function(response, status, headers, config) {
                console.log('Something went wrong: ' + response);
            });
    };
    
    $scope.openCaseInstance = function() {
        $rootScope.root.showStartForm = false;
        $scope.model.formData = undefined;
    };
    
    $scope.getCaseInstance($rootScope.root.selectedCaseId);
}]);
