/* 
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

flowableAdminApp.controller('EventSubscriptionController', ['$scope', '$rootScope', '$http', '$timeout','$location','$routeParams', '$modal', '$translate',
    function ($scope, $rootScope, $http, $timeout, $location, $routeParams, $modal, $translate) {
		$rootScope.navigation = {main: 'process-engine', sub: 'event-subscriptions'};
		
		$scope.returnToList = function() {
			$location.path("/event-subscriptions");
		};

		$scope.triggerEvent = function() {
			var dataForPost = {
				eventType: $scope.eventSubscription.eventType,
				eventName: $scope.eventSubscription.eventName,
			};
			
            if ($scope.eventSubscription.executionId !== undefined && $scope.eventSubscription.executionId.length > 0) {
                dataForPost.executionId = $scope.eventSubscription.executionId;
            }
            
            if ($scope.eventSubscription.tenantId !== undefined && $scope.eventSubscription.tenantId.length > 0) {
                dataForPost.tenantId = $scope.eventSubscription.tenantId;
            }
		
			$http({method: 'POST', 
				url: '/app/rest/admin/event-subscriptions/' + $scope.eventSubscription.id,
				data: dataForPost}).
        	success(function(data, status, headers, config) {
        	  $scope.addAlert($translate.instant('ALERT.EVENT-SUBSCRIPTION.TRIGGERED', $scope.eventSubscription), 'info');
        		$scope.returnToList();
        	})
        	.error(function(data, status, headers, config) {
        		$scope.loadEventSubscription();
        	});
		};

		$scope.openProcessInstance = function(processInstanceId) {
			if (processInstanceId) {
				$location.path("/process-instance/" + processInstanceId);
			}
		};

		$scope.openProcessDefinition = function(processDefinitionId) {
			if (processDefinitionId) {
				$location.path("/process-definition/" + processDefinitionId);
			}
		};

		$scope.loadEventSubscription = function() {
			$scope.eventSubscription = {};
			$http({method: 'GET', url: '/app/rest/admin/event-subscriptions/' + $routeParams.eventSubscriptionId}).
	    	success(function(data, status, headers, config) {
	            $scope.eventSubscription = data;
	    	}).
	    	error(function(data, status, headers, config) {
                if (data && data.message) {
                    // Extract error-message
                    $rootScope.addAlert(data.message, 'error');
                } else {
                    // Use default error-message
                    $rootScope.addAlert($translate.instant('ALERT.GENERAL.HTTP-ERROR'), 'error');
                }
            });
		};

		// Load job
		$scope.executeWhenReady(function() {
		    $scope.loadEventSubscription();
		});

}]);
