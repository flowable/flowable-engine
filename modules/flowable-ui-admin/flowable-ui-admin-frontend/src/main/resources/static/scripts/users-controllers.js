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

flowableAdminApp.controller('UsersController', ['$scope', '$rootScope', '$http', '$timeout','$location', '$modal', '$translate', '$q',
    function ($scope, $rootScope, $http, $timeout, $location, $modal, $translate, $q) {
		$rootScope.navigation = {selection: 'users'};
		
		$scope.selectedUsers = [];

	    $q.all([$translate('USERS.HEADER.LOGIN'),
              $translate('USERS.HEADER.FIRSTNAME'),
              $translate('USERS.HEADER.LASTNAME'),
              $translate('USERS.HEADER.EMAIL'),
              $translate('USERS.HEADER.CLUSTER_USER')])
              .then(function(headers) {

                  // Config for grid
                  $scope.gridUsers = {
                      data: 'usersData',
                      enableRowReordering: true,
                      enableColumnResize: true,
                      multiSelect: false,
                      keepLastSelected : false,
                      enableSorting: false,
                      rowHeight: 36,
                      selectedItems: $scope.selectedUsers,
                      columnDefs: [{ field: 'login', displayName: headers[0]},
                          { field: 'firstName', displayName: headers[1]},
                          { field: 'lastName', displayName: headers[2]},
                          { field: 'email', displayName: headers[3]},
                          { field: 'isClusterUser', displayName: headers[4]}
                      ]
                  };
        });

        $scope.loadUsers = function() {
        	$http({method: 'GET', url: '/app/rest/users'}).
		        success(function(data, status, headers, config) {
		        	$scope.usersData = data;

		        	// Indicate if the user is used for sending events
		        	if($scope.usersData !== null && $scope.usersData !== undefined) {
		        	    for (var userIndex = 0; userIndex < $scope.usersData.length; userIndex++) {
		        	        var userData = $scope.usersData[userIndex];
		        	        userData.isClusterUser = userData.clusterUser ? $translate.instant('GENERAL.YES') : $translate.instant('GENERAL.NO');
		        	    }
		        	}

		        }).
		        error(function(data, status, headers, config) {
		            console.log('Something went wrong when fetching users');
		        });
        };

        $scope.executeWhenReady(function() {
            $scope.loadUsers();
        });

        // Dialogs
		var resolve = {
			// Reference the current task
			user: function () {
			    return $scope.selectedUsers[0];
	        }
		};

		$scope.deleteUser = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/user-delete-popup.html',
				controller: 'DeleteUserModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (deleteUser) {
				if (deleteUser) {
				    $scope.addAlert($translate.instant('ALERT.USER.DELETED', $scope.selectedUsers[0]), 'info');

				    // Clear selection after delete, or actions will still point to deleted user
				    $scope.selectedUsers.splice(0,1);
				    $scope.loadUsers();
				}
			});
		};

		$scope.editUser = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/user-edit-popup.html',
				controller: 'EditUserModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (userUpdated) {
				if (userUpdated) {
				  $scope.addAlert($translate.instant('ALERT.USER.UPDATED', $scope.selectedUsers[0]), 'info');
					$scope.loadUsers();
				}
			});
		};

		$scope.changePassword = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/user-change-password-popup.html',
				controller: 'ChangePasswordModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (userUpdated) {
				if (userUpdated) {
				  $scope.addAlert($translate.instant('ALERT.USER.PASSWORD-CHANGED', $scope.selectedUsers[0]), 'info');
				}
			});
		};

		$scope.newUser = function() {
			var modalInstance = $modal.open({
				templateUrl: 'views/user-new-popup.html',
				controller: 'NewUserModalInstanceCtrl',
				resolve: resolve
			});

			modalInstance.result.then(function (userCreated) {
				if (userCreated) {
				  $scope.addAlert($translate.instant('ALERT.USER.CREATED', userCreated), 'info');
					$scope.loadUsers();
				}
			});
		};
    }]);

flowableAdminApp.controller('DeleteUserModalInstanceCtrl',
    ['$scope', '$modalInstance', '$http', 'user', function ($scope, $modalInstance, $http, user) {

  $scope.user = user;
  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'DELETE', url: '/app/rest/users/' + $scope.user.login}).
    	success(function(data, status, headers, config) {
    		$modalInstance.close(true);
	  		$scope.status.loading = false;
        }).
        error(function(data, status, headers, config) {
        	$modalInstance.close(false);
        	$scope.status.loading = false;
        });
  };

  $scope.cancel = function () {
	if(!$scope.status.loading) {
		$modalInstance.dismiss('cancel');
	}
  };
}]);

flowableAdminApp.controller('EditUserModalInstanceCtrl',
    ['$scope', '$modalInstance', '$http', 'user', function ($scope, $modalInstance, $http, user) {

  $scope.user = user;
  $scope.model = {
		  login: user.login,
		  firstName: user.firstName,
		  lastName: user.lastName,
		  email: user.email
  };

  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'PUT', url: '/app/rest/users/' + $scope.user.login, data: $scope.model}).
  	  success(function(data, status, headers, config) {
  		  $modalInstance.close(true);
  		  $scope.status.loading = false;
      }).error(function(data, status, headers, config) {
          $scope.status.loading = false;

          if(data.message) {
            $scope.model.errorMessage = data.message;
          }
      });
  };

  $scope.cancel = function () {
	if(!$scope.status.loading) {
		$modalInstance.dismiss('cancel');
	}
  };
}]);

flowableAdminApp.controller('ChangePasswordModalInstanceCtrl',
    ['$scope', '$modalInstance', '$http', 'user', function ($scope, $modalInstance, $http, user) {

	  $scope.user = user;
	  $scope.model = {
			  oldPassword: '',
			  newPassword: ''
	  };

	  $scope.status = {loading: false};

	  $scope.ok = function () {
		  $scope.status.loading = true;
		  $http({method: 'PUT', url: '/app/rest/users/' + $scope.user.login + '/change-password', data: $scope.model}).
	  	  success(function(data, status, headers, config) {
	  		  $modalInstance.close(true);
	  		  $scope.status.loading = false;
	      }).error(function(data, status, headers, config) {
	        $scope.status.loading = false;

	        if(data.message) {
	          $scope.model.errorMessage = data.message;
	        }
	      });
	  };

	  $scope.cancel = function () {
		if(!$scope.status.loading) {
			$modalInstance.dismiss('cancel');
		}
	  };
	}]);

flowableAdminApp.controller('NewUserModalInstanceCtrl',
    ['$scope', '$modalInstance', '$http', function ($scope, $modalInstance, $http) {

  $scope.model = {
		  login: '',
		  password: '',
		  firstName: '',
		  lastName: '',
		  email: ''
  };

  $scope.status = {loading: false};

  $scope.ok = function () {
	  $scope.status.loading = true;
	  $http({method: 'POST', url: '/app/rest/users', data: $scope.model, ignoreErrors: true}).
  	  success(function(data, status, headers, config) {
  		  $modalInstance.close($scope.model);
  		  $scope.status.loading = false;
      }).error(function(data, status, headers, config) {
    	  $scope.status.loading = false;

    	  if(data.message) {
    	    $scope.model.errorMessage = data.message;
    	  }
      });
  };

  $scope.cancel = function () {
	if(!$scope.status.loading) {
		$modalInstance.dismiss('cancel');
	}
  };
}]);
