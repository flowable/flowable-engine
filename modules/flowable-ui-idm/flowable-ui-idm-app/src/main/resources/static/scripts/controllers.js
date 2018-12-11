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
flowableApp.controller('LoginController', ['$scope', '$location', '$cookies', 'AuthenticationSharedService', '$timeout',
    function ($scope, $location, $cookies, AuthenticationSharedService, $timeout) {

        $scope.model = {
            loading: false
        };

        // default values
        if ( !('redirectUrl' in $location.search()) ) {
            delete $cookies.redirectUrl;
        }
        $scope.hasExternalAuth = false;

        AuthenticationSharedService.getSsoUrl({
            success: function(data) {
                $scope.ssoUrl = data;
                $scope.hasExternalAuth = true;

                if( 'redirectUrl' in $location.search() ){
                    var redirectUrl = $location.search()['redirectUrl'];
                    if( redirectUrl != null && redirectUrl.length > 0 ){
                        $cookies.redirectUrl = redirectUrl;
                    }
                }
            }
        });

        $scope.login = function () {

            $scope.model.loading = true;

            jQuery('#username').trigger('change');
            jQuery('#password').trigger('change');

            $timeout(function() {
                AuthenticationSharedService.login({
                    username: $scope.username,
                    password: $scope.password,
                    success: function () {
                        $scope.model.loading = false;
                    },
                    error: function() {
                        $scope.model.loading = false;
                    }
                });
            });


        };
    }]
);