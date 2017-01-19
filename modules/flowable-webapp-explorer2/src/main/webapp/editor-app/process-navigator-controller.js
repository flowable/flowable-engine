'use strict'

angular.module('activitiModeler').controller('ProcessNavigatorController',['editorManager', '$scope',function(editorManager,$scope){
    //problem here the ORYX editor is bound to the rootscope. In theory this communication should be moved to a service.

    $scope.showSubProcess = function(child){
        var activiti = editorManager.getChildShapeByResourceId(child.resourceId);
        editorManager.setSelection([activiti],[],true);
    }

    $scope.treeview = {};
    $scope.isEditorReady = false;

    $scope.edit = function(resourceId){
        editorManager.edit(resourceId);
    };

    KISBPM.eventBus.addListener(KISBPM.eventBus.EVENT_TYPE_EDITOR_READY, function(event){
        $scope.isEditorReady = true;
        renderProcessHierarchy();

        editorManager.registerOnEvent(ORYX.CONFIG.ACTION_DELETE_COMPLETED,filterEvent);

        //always a single event.
        editorManager.registerOnEvent(ORYX.CONFIG.EVENT_UNDO_ROLLBACK,renderProcessHierarchy);
    })

    //if an element is added te properties will catch this event.
    KISBPM.eventBus.addListener(KISBPM.eventBus.EVENT_TYPE_PROPERTY_VALUE_CHANGED,filterEvent);
    KISBPM.eventBus.addListener(KISBPM.eventBus.EVENT_TYPE_ITEM_DROPPED,filterEvent);
    KISBPM.eventBus.addListener("EDITORMANAGER-EDIT-ACTION",function(){
        renderProcessHierarchy();
    });

    function filterEvent(event){
        //console.log(event);
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
        console.log("rendering treeview")

        $scope.treeview = editorManager.getTree();
    }

}]);