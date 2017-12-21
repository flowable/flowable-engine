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

angular.module('flowableModeler').controller('FlowablePlanItemDropdownCtrl', [ '$scope', function($scope) {

    // Find all planitems
    var selectedShape = $scope.selectedShape;
    if (selectedShape) {
        
        // Go up in parent chain until plan model is found
        var planModel;        
        var parent = selectedShape.parent;
        if (parent) {
            while (planModel === undefined && parent !== null && parent !== undefined) {
                if (parent.resourceId !== null && parent.resourceId !== undefined && 'casePlanModel' === parent.resourceId) {
                    planModel = parent;                
                } else {
                    parent = parent.parent;
                }
            }
        }
        
        var planItems = [];
        if (planModel !== null && planModel !== undefined) {
        
            var toVisit = [];
            for (var i=0; i<planModel.children.length; i++) {
                toVisit.push(planModel.children[i]);
            }
            
            while (toVisit.length > 0) {
                var child = toVisit.pop();
                if (typeof child.getStencil === 'function' 
                    && (child.getStencil()._jsonStencil.groups.indexOf('Activities') >= 0 || (child.getStencil()._jsonStencil.title === 'Stage') )) {
                    planItems.push(child);
                }
                if (child.children !== null && child.children !== undefined) {
                     for (var i=0; i<child.children.length; i++) {
                        toVisit.push(child.children[i]);
                    }
                }
            }
        }
        
        var simplifiedPlanItems = [];
        for (var i=0; i<planItems.length; i++) {
            simplifiedPlanItems.push({ id: planItems[i].resourceId, name: planItems[i].properties.get('oryx-name') });
        }
        
        if (simplifiedPlanItems.length > 0) {
            simplifiedPlanItems.sort(function(a,b) {
                if(a.name < b.name) {
                    return -1;
                } else if (a.name > b.name) {
                    return 1;
                } else {
                    return 0;
                }
            });
        }
        $scope.planItems = simplifiedPlanItems;
        
    }

    if ($scope.property.value == undefined && $scope.property.value == null) {
    	$scope.property.value = '';
    }
        
    $scope.planItemChanged = function() {
    	$scope.updatePropertyInModel($scope.property);
    };
}]);