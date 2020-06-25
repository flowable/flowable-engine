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

/**
 * Created by Pardo David on 3/01/2017.
 * For this service to work the user must call bootEditor method
 */
angular.module("flowableModeler").factory("editorManager", ["$http", function ($http) {
    var editorManager = Class.create({
        initialize: function () {
            this.treeFilteredElements = ["SubProcess", "CollapsedSubProcess"];
            this.canvasTracker = new Hash();
            this.structualIcons = {
                "SubProcess": "expanded.subprocess.png",
                "CollapsedSubProcess": "subprocess.png",
                "EventSubProcess": "event.subprocess.png"
            };

            this.current = this.modelId;
            this.loading = true;
        },
        getModelId: function () {
            return this.modelId;
        },
        setModelId: function (modelId){
            this.modelId = modelId;
        },
        getCurrentModelId: function () {
        	return this.current;
        },
        setStencilData: function(stencilData){
            //we don't want a references!
            this.stencilData = jQuery.extend(true, {},stencilData);
        },
        getStencilData: function () {
            return this.stencilData;
        },
        getSelection: function () {
            return this.editor.selection;
        },
        getSubSelection: function () {
            return this.editor._subSelection;
        },
        handleEvents: function (events) {
            this.editor.handleEvents(events);
        },
        setSelection: function (selection) {
            this.editor.setSelection(selection);
        },
        registerOnEvent: function (event, callback) {
            this.editor.registerOnEvent(event, callback);
        },
        getChildShapeByResourceId: function (resourceId) {
            return this.editor.getCanvas().getChildShapeByResourceId(resourceId);
        },
        getJSON: function () {
            return this.editor.getJSON();
        },
        getStencilSets: function () {
            return this.editor.getStencilSets();
        },
        getEditor: function () {
            return this.editor; //TODO: find out if we can avoid exposing the editor object to angular.
        },
        executeCommands: function (commands) {
            this.editor.executeCommands(commands);
        },
        getCanvas: function () {
            return this.editor.getCanvas();
        },
        getRules: function () {
            return this.editor.getRules();
        },
        eventCoordinates: function (coordinates) {
            return this.editor.eventCoordinates(coordinates);
        },
        eventCoordinatesXY: function (x, y) {
            return this.editor.eventCoordinatesXY(x, y);
        },
        updateSelection: function () {
            this.editor.updateSelection();
        },
        /**
         * @returns the modeldata as received from the server. This does not represent the current editor data.
         */
        getBaseModelData: function () {
            return this.modelData;
        },
        edit: function (resourceId) {
            //Save the current canvas in the canvastracker if it is the root process.
            this.syncCanvasTracker();

            this.loading = true;

            var shapes = this.getCanvas().getChildren();
            shapes.each(function (shape) {
                this.editor.deleteShape(shape);
            }.bind(this));

            shapes = this.canvasTracker.get(resourceId);
            if(!shapes){
                shapes = JSON.stringify([]);
            }

            this.editor.loadSerialized({
                childShapes: shapes
            });

            this.getCanvas().update();

            this.current = resourceId;

            this.loading = false;
            FLOWABLE.eventBus.dispatch("EDITORMANAGER-EDIT-ACTION", {});
            FLOWABLE.eventBus.dispatch(FLOWABLE.eventBus.EVENT_TYPE_UNDO_REDO_RESET, {});
        },
        getTree: function () {
            //build a tree of all subprocesses and there children.
            var result = new Hash();
            var parent = this.getModel();
            result.set("name", parent.properties["name"] || "No name provided");
            result.set("id", this.modelId);
            result.set("type", "root");
            result.set("current", this.current === this.modelId)
            var childShapes = parent.childShapes;
            var children = this._buildTreeChildren(childShapes);
            result.set("children", children);
            return result.toObject();
        },
        _buildTreeChildren: function (childShapes) {
            var children = [];
            for (var i = 0; i < childShapes.length; i++) {
                var childShape = childShapes[i];
                var stencilId = childShape.stencil.id;
                //we are currently only interested in the expanded subprocess and collapsed processes
                if (stencilId && this.treeFilteredElements.indexOf(stencilId) > -1) {
                    var child = new Hash();
                    child.set("name", childShape.properties.name || "No name provided");
                    child.set("id", childShape.resourceId);
                    child.set("type", stencilId);
                    child.set("current", childShape.resourceId === this.current);
                    
                    //check if childshapes

                    if (stencilId === "CollapsedSubProcess") {
                        //the save function stores the real object as a childshape
                        //it is possible that there is no child element because the user did not open the collapsed subprocess.
                        if (childShape.childShapes.length === 0) {
                            child.set("children", []);
                        } else {
                            child.set("children", this._buildTreeChildren(childShape.childShapes));
                        }
                        child.set("editable", true);
                    } else {
                        child.set("children", this._buildTreeChildren(childShape.childShapes));
                        child.set("editable", false);
                    }
                    child.set("icon", this.structualIcons[stencilId]);
                    children.push(child.toObject());
                }
            }
            return children;
        },
        syncCanvasTracker: function () {
            var shapes = this.getCanvas().getChildren();
            var jsonShapes = [];
            shapes.each(function (shape) {
                //toJson is an summary object but its not a json string.!!!!!
                jsonShapes.push(shape.toJSON());
            });
            this.canvasTracker.set(this.current, JSON.stringify(jsonShapes));
        },
        getModel: function () {
            this.syncCanvasTracker();
            
            var modelMetaData = this.getBaseModelData();
            
            var stencilId = undefined;
            var stencilSetNamespace = undefined;
            var stencilSetUrl = undefined;
            if (modelMetaData.model.stencilset.namespace == 'http://b3mn.org/stencilset/cmmn1.1#') {
                stencilId = 'CMMNDiagram';
                stencilSetNamespace = 'http://b3mn.org/stencilset/cmmn1.1#';
                stencilSetUrl = '../editor/stencilsets/cmmn1.1/cmmn1.1.json';
            } else if (modelMetaData.model.stencilset.namespace == 'http://b3mn.org/stencilset/dmn1.2#') {
                stencilId = 'DMNDiagram';
                stencilSetNamespace = 'http://b3mn.org/stencilset/dmn1.2#';
                stencilSetUrl = '../editor/stencilsets/dmn1.1/dmn1.2.json';
            } else {
                stencilId = 'BPMNDiagram';
                stencilSetNamespace = 'http://b3mn.org/stencilset/bpmn2.0#';
                stencilSetUrl = '../editor/stencilsets/bpmn2.0/bpmn2.0.json';
            }

            //this is an object.
            var editorConfig = this.editor.getJSON();
            var model = {
                modelId: this.modelId,
                bounds: editorConfig.bounds,
                properties: editorConfig.properties,
                childShapes: JSON.parse(this.canvasTracker.get(this.modelId)),
                stencil: {
                    id: stencilId,
                },
                stencilset: {
                    namespace: stencilSetNamespace,
                    url: stencilSetUrl
                }
            };

            this._mergeCanvasToChild(model);

            return model;
        },
        setModelData: function(response){
            this.modelData = response.data;
        },
        bootEditor: function () {
            //TODO: populate the canvas with correct json sections.
            //resetting the state
            this.canvasTracker = new Hash();
            var config = jQuery.extend(true, {}, this.modelData); //avoid a reference to the original object.
            if(!config.model.childShapes){
                config.model.childShapes = [];
            }
            
            this.findAndRegisterCanvas(config.model.childShapes); //this will remove any childshapes of a collapseable subprocess.
            this.canvasTracker.set(config.modelId, JSON.stringify(config.model.childShapes)); //this will be overwritten almost instantly.

            this.editor = new ORYX.Editor(config);
            this.current = this.editor.id;
            this.loading = false;

            FLOWABLE.eventBus.editor = this.editor;
            FLOWABLE.eventBus.dispatch("ORYX-EDITOR-LOADED", {});
            FLOWABLE.eventBus.dispatch(FLOWABLE.eventBus.EVENT_TYPE_EDITOR_BOOTED, {});
        },
        findAndRegisterCanvas: function (childShapes) {
            for (var i = 0; i < childShapes.length; i++) {
                var childShape = childShapes[i];
                if (childShape.stencil.id === "CollapsedSubProcess") {
                    if (childShape.childShapes.length > 0) {
                        //the canvastracker will auto correct itself with a new canvasmodel see this.edit()...
                        this.findAndRegisterCanvas(childShape.childShapes);
                        //a canvas can't be nested as a child because the editor would crash on redundant information.
                        this.canvasTracker.set(childShape.resourceId, JSON.stringify(childShape.childShapes));
                        //reference to config will clear the value.
                        childShape.childShapes = [];
                    } else {
                        this.canvasTracker.set(childShape.resourceId, '[]');
                    }
                }
            }
        },
        _mergeCanvasToChild: function (parent) {
            for (var i = 0; i < parent.childShapes.length; i++) {
                var childShape = parent.childShapes[i]
                if(childShape.stencil.id === "CollapsedSubProcess"){
                    
                    var elements = this.canvasTracker.get(childShape.resourceId);
                    if(elements){
                        elements = JSON.parse(elements);
                    }else{
                        elements = [];
                    }
                    childShape.childShapes = elements;
                    this._mergeCanvasToChild(childShape);
                }else if(childShape.stencil.id === "SubProcess"){
                    this._mergeCanvasToChild(childShape);
                }else{
                    //do nothing?
                }
            }
        },
        dispatchOryxEvent: function (event) {
            FLOWABLE.eventBus.dispatchOryxEvent(event);
        },
        isLoading: function(){
            return this.loading;
        },
        navigateTo: function(resourceId){
            //TODO: this could be improved by check if the resourceId is not equal to the current tracker...
            this.syncCanvasTracker();
            var found = false;
            this.canvasTracker.each(function(pair){
                var key = pair.key;
                var children = JSON.parse(pair.value);
                var targetable = this._findTarget(children, resourceId);
                if (!found && targetable){
                    this.edit(key);
                    var flowableShape = this.getCanvas().getChildShapeByResourceId(targetable);
                    this.setSelection([flowableShape],[],true);
                    found = true;
                }
            },this);
        },
        _findTarget: function(children,resourceId){
            for(var i =0; i < children.length; i++){
                var child = children[i];
                if(child.resourceId === resourceId){
                    return child.resourceId;
                }else if(child.properties && child.properties["overrideid"] === resourceId){
                    return child.resourceId;
                }else{
                    var result = this._findTarget(child.childShapes,resourceId);
                    if(result){
                        return result;
                    }
                }
            }
            return false;
        }
    });

    return new editorManager();
}]);
