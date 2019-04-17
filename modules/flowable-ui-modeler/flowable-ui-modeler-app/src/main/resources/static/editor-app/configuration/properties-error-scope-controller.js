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
angular.module('flowableModeler').controller('FlowableErrorRefCtrl', [ '$scope', function($scope) {

    // Find the parent shape on which the error definitions are defined
    var errorDefinitionsProperty = undefined;
    var parent = $scope.selectedShape;
    while (parent !== null && parent !== undefined && errorDefinitionsProperty === undefined) {
        if (parent.properties && parent.properties.get('oryx-errordefinitions')) {
            errorDefinitionsProperty = parent.properties.get('oryx-errordefinitions');
        } else {
            parent = parent.parent;
        }
    }

    try {
        errorDefinitionsProperty = JSON.parse(errorDefinitionsProperty);
        if (typeof errorDefinitionsProperty == 'string') {
            errorDefinitionsProperty = JSON.parse(errorDefinitionsProperty);
        }
    } catch (err) {
        // Do nothing here, just to be sure we try-catch it
    }

    $scope.errorDefinitions = errorDefinitionsProperty;


    $scope.errorChanged = function() {
    	$scope.updatePropertyInModel($scope.property);
    };
}]);