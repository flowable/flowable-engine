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

angular.module('flowableModeler').controller('FlowableCaseReferenceCtrl',
    [ '$scope', '$modal', '$http', function($scope, $modal, $http) {
	
     // Config for the modal window
     var opts = {
         template:  'editor-app/configuration/properties/case-reference-popup.html?version=' + Date.now(),
         scope: $scope
     };

     // Open the dialog
        _internalCreateModal(opts, $modal, $scope);
}]);

angular.module('flowableModeler').controller('FlowableCaseReferencePopupCtrl', [ '$scope', '$http', 'editorManager', '$location', function($scope, $http, editorManager, $location) {
	
    $scope.state = {'loadingCases' : true, 'error' : false};
    
    // Close button handler
    $scope.close = function() {
    	$scope.property.mode = 'read';
        $scope.$hide();
    };
    
    // Selecting/deselecting a case
    $scope.selectCase = function(caseModel, $event) {
   	 	$event.stopPropagation();
   	 	if ($scope.selectedCase && $scope.selectedCase.id && caseModel.id == $scope.selectedCase.id) {
   	 		// un-select the current selection
   	 		$scope.selectedCase = null;
   	 	} else {
   	 		$scope.selectedCase = caseModel;
   	 	}
    };

	$scope.open = function() {
		if ($scope.selectedCase) {
			$location.path("/editor/" + $scope.selectedCase.id);
		}
	};
    
    // Saving the selected value
    $scope.save = function() {
   	 	if ($scope.selectedCase) {
   	 		$scope.property.value = {'id' : $scope.selectedCase.id, 'name' : $scope.selectedCase.name, 'key': $scope.selectedCase.key};
   	 	} else {
   	 		$scope.property.value = null; 
   	 	}
   	 	$scope.updatePropertyInModel($scope.property);
   	 	$scope.close();
    };
    
    $scope.loadCases = function() {
   	    var modelMetaData = editorManager.getBaseModelData();
    	$http.get(FLOWABLE.APP_URL.getCaseModelsUrl('?excludeId=' + modelMetaData.modelId))
    		.success(
    			function(response) {
    				$scope.state.loadingCases = false;
    				$scope.state.caseError = false;
    				$scope.caseModels = response.data;
    			})
    		.error(
    			function(data, status, headers, config) {
    				$scope.state.loadingCases = false;
    				$scope.state.caseError = true;
    			});
    };
    
    if ($scope.property && $scope.property.value && $scope.property.value.id) {
   	 	$scope.selectedCase = $scope.property.value;
    }
    
    $scope.loadCases();  
}]);
