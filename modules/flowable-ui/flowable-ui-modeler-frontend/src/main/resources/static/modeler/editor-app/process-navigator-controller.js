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

angular.module('flowableModeler').controller('ProcessNavigatorController',['editorManager', '$scope',function(editorManager, $scope){
    //problem here the ORYX editor is bound to the rootscope. In theory this communication should be moved to a service.

    $scope.showSubProcess = function(child){
        var flowableShapes = editorManager.getChildShapeByResourceId(child.resourceId);
        editorManager.setSelection([flowableShapes],[],true);
    }

    $scope.treeview = {};
    $scope.isEditorReady = false;

    $scope.edit = function(resourceId){
        editorManager.edit(resourceId);
    };

    FLOWABLE.eventBus.addListener(FLOWABLE.eventBus.EVENT_TYPE_EDITOR_READY, function(event){
        $scope.isEditorReady = true;
        renderProcessHierarchy();

        editorManager.registerOnEvent(ORYX.CONFIG.ACTION_DELETE_COMPLETED, filterEvent);

        //always a single event.
        editorManager.registerOnEvent(ORYX.CONFIG.EVENT_UNDO_ROLLBACK, renderProcessHierarchy);
    })

    //if an element is added te properties will catch this event.
    FLOWABLE.eventBus.addListener(FLOWABLE.eventBus.EVENT_TYPE_PROPERTY_VALUE_CHANGED,filterEvent);
    FLOWABLE.eventBus.addListener(FLOWABLE.eventBus.EVENT_TYPE_ITEM_DROPPED,filterEvent);
    FLOWABLE.eventBus.addListener("EDITORMANAGER-EDIT-ACTION",function(){
        renderProcessHierarchy();
    });

    function filterEvent(event){
        //this event is fired when the user changes a property by the property editor.
        if(event.type === "event-type-property-value-changed"){
            if(event.property.key === "oryx-overrideid" || event.property.key === "oryx-name"){
                renderProcessHierarchy()
            }
            //this event is fired when the stencil / shape's text is changed / updated.
        }else if(event.type === "propertyChanged"){
            if(event.name === "oryx-overrideid" || event.name === "oryx-name"){
                renderProcessHierarchy();
            }
        }else if(event.type === ORYX.CONFIG.ACTION_DELETE_COMPLETED){
            renderProcessHierarchy();
            //for some reason the new tree does not trigger an ui update.
            //$scope.$apply();
        }else if(event.type === "event-type-item-dropped"){
            renderProcessHierarchy();
        }
    }

    function renderProcessHierarchy(){
        //only start calculating when the editor has done all his constructor work.
        if(!$scope.isEditorReady){
            return false;
        }
        
        if (!editorManager.isLoading()){
            //the current implementation of has a lot of eventlisteners. when calling getTree() it could manipulate
            //the canvastracker while the canvas is stille loading stuff.
            //TODO: check if its possible to trigger the re-rendering by a single event instead of registering on 10 events...
            $scope.treeview = editorManager.getTree();
        }

    }

}]);
