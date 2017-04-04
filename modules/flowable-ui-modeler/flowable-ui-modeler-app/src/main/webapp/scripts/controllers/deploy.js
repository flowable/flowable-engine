angular.module('flowableModeler')
  .controller('LoginController', ['$scope', '$http', 'Base64', '$log', '$window', '$modal', 
                              function ($scope, $http, Base64, $log, $window, $modal) {
    var vm = this;
    vm.loginDetail = {
      "name" : " ",
      "password" : ""
    };
    vm.deploy = deploy;
    vm.cancel = cancel;
    
    var deployModelId = $scope.model.process.id

    var deployUrl = FLOWABLE.CONFIG.deployUrl ? FLOWABLE.CONFIG.deployUrl : "http://localhost:8080/runtime/workflow/deploy";
    function deploy() {
      if (vm.loginDetail.name && vm.loginDetail.password) {
        vm.deploying = true;
        vm.errorMsg = "Deployment in progress!";

        var authdata = Base64.encode(vm.loginDetail.name + ':' + vm.loginDetail.password);

        $http.get("app/rest/models/" + deployModelId + "/exportForDeploy").then(function(response) {
          $http.defaults.headers.common['Authorization'] = 'Basic ' + authdata;
          $http({
            method : "POST",
            url : deployUrl,
            data : response.data
          }).then(function(deployResponse) {
            $log.debug('deployed!', deployResponse);
            vm.deploying = false;
            $window.alert("Deployed successfully!");
            $scope.$hide();
          }, function(deployResponse) {
            vm.deploying = false;
            $log.debug('deploy error!', deployResponse);
            if (deployResponse.status == "401") {
              vm.errorMsg = "Wrong username or password! try again";
            } else if (deployResponse.status == "400") {
              vm.errorMsg = deployResponse.data.error;
            } else {
              vm.errorMsg = "Something went wrong, please ask system administrator for help!";
            }
          });
          delete $http.defaults.headers.common['Authorization'];
        });

      } else {
        vm.errorMsg = "Please give username and password for deployment!";
      }
    }

    function cancel() {
      $scope.$hide();
    }
    
}]);