/**
 * Created by Pardo David on 3/01/2017.
 */
angular.module("activitiModeler").factory("editorManager", ["$http", function ($http) {
    var editorManager = Class.create({
        initialize: function () {
            this.treeFilteredElements = ["SubProcess", "CollapsedSubProcess"];
            this.canvasTracker = new Hash();
            this.structualIcons = {
                "SubProcess": "expanded.subprocess.png",
                "CollapsedSubProcess": "subprocess.png",
                "EventSubProcess": "event.subprocess.png"
            };

            var self = this;
            this.modelId = EDITOR.UTIL.getParameterByName('modelId');
            //we first initialize the stencilset used by the editor. The editorId is always the modelId.
            $http.get(KISBPM.URL.getStencilSet()).then(function (response) {
                var baseUrl = "http://b3mn.org/stencilset/";
                self.stencilData = jQuery.extend(true, {}, response.data); //we don't want a references!
                var stencilSet = new ORYX.Core.StencilSet.StencilSet(baseUrl, response.data); //the stencilset alters the data ref!
                ORYX.Core.StencilSet.loadStencilSet(baseUrl, stencilSet, self.modelId);
                //after the stencilset is loaded we make sure the plugins.xml is loaded.
                return $http.get(ORYX.CONFIG.PLUGINS_CONFIG);
            }).then(function (response) {
                ORYX._loadPlugins(response.data);
                return $http.get(KISBPM.URL.getModel(self.modelId));
            }).then(function (response) {
                self.bootEditor(response);
            }).catch(function (error) {
                console.log("Problem officer " + error);
            });
        },
        getModelId: function(){
          return this.modelId;
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
            //TODO: check if the resourceId match an editable thing.
            //store the curent editor state
            this.canvasTracker.set(this.current, this.editor.getSerializedJSON());
            this.current = resourceId;

            //check if there is already a model for the resource
            var canvasModel = this.canvasTracker.get(resourceId);
            if (!canvasModel) {
                //TODO: fetch the name of the subprocess.
                canvasModel = this.createModel(resourceId, "");
            } else {
                canvasModel = JSON.parse(canvasModel);
            }

            //clear the canvas.
            var shapes = this.getCanvas().getChildShapes();
            var editor = this.editor;
            shapes.each(function (shape) {
                editor.deleteShape(shape);
            });

            this.editor.loadSerialized(canvasModel);
            this.editor.setSelection([this.getCanvas()]);


            this.getCanvas().update();

            KISBPM.eventBus.dispatch("EDITORMANAGER-EDIT-ACTION", {});


        },
        createModel: function (modelId, name) {
            if (!name || name.length === 0) {
                name = "New subprocess";
            }
            var model = {
                bounds: {
                    lowerRight: {
                        x: 1485.0,
                        y: 700.0
                    },
                    upperLeft: {
                        x: 0.0,
                        y: 0.0
                    }
                },
                resourceId: "canvas",
                stencil: {
                    id: "SubProcessDiagram"
                },
                stencilset: {
                    namespace: "http://b3mn.org/stencilset/bpmn2.0#",
                    url: "../editor/stencilsets/bpmn2.0/bpmn2.0.json"
                },
                properties: {
                    process_id: modelId,
                    name: name
                }
            }

            return model;
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
                    console.log(childShape.resourceId);
                    var child = new Hash();
                    child.set("name", childShape.properties.name || "No name provided");
                    child.set("id", childShape.resourceId);
                    child.set("type", stencilId);
                    child.set("current", childShape.resourceId === this.current);
                    console.log(childShape.resourceId);
                    //check if childshapes

                    if (stencilId === "CollapsedSubProcess") {
                        //the save function stores the real object as a childshape
                        //it is possible that there is no child element because the user did not open the collapsed subprocess.
                        if (childShape.childShapes.length === 0) {
                            child.set("children", []);
                        } else {
                            var collapsedSubprocessDiagram = childShape.childShapes[0];
                            child.set("children", this._buildTreeChildren(collapsedSubprocessDiagram.childShapes));
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
        syncCanvasTracker:function(){
            this.canvasTracker.set(this.current, this.editor.getSerializedJSON());
        },
        getModel: function () {
            this.syncCanvasTracker();

            var root = JSON.parse(this.canvasTracker.get(this.modelId));
            //attach all subprocesses canvas as childshape
            this._mergeCanvasToChild(root);
            return root;
        },
        bootEditor: function (response) {
            this.canvasTracker = new Hash();

            var config = jQuery.extend(true,{},response.data); //avoid a reference to the original object.
            this.canvasTracker.set(config.modelId,JSON.stringify(config.model)); //this will be overwritten almost instantly.
            this.findAndRegisterCanvas(config.model.childShapes);

            //TODO: boot the editor.
            this.modelData = response.data;
            this.editor = new ORYX.Editor(config);
            this.current = this.editor.id;

            KISBPM.eventBus.editor = this.editor;
            KISBPM.eventBus.dispatch("ORYX-EDITOR-LOADED", {});
        },
        findAndRegisterCanvas: function (childShapes) {
              for(var i = 0; i < childShapes.length; i++){
                  var childShape =  childShapes[i];
                  if(childShape.stencil.id === "CollapsedSubProcess"){
                      //TODO: at this point there is an edge case that there is no canvas child if the collapsed subprocess was never opened.
                      var subChildShapes = childShape.childShapes;
                      if( subChildShapes && subChildShapes.length === 1){
                          this.canvasTracker.set(childShape.resourceId,JSON.stringify(subChildShapes[0]));
                          //a canvas can't be nested as a child because the editor would crash on redundant information.
                          childShape.childShapes = []; //reference to config will clear the value.
                      }else{
                          //the canvastracker will auto correct itself with a new canvasmodel see this.edit()...
                      }
                  }
                  this.findAndRegisterCanvas(childShape.childShapes);
              }
        },
        _mergeCanvasToChild: function (parent) {
            for (var i = 0; i < parent.childShapes.length; i++) {
                var childShape = parent.childShapes[i];
                var tracked = this.canvasTracker.get(childShape.resourceId);
                if (tracked) {
                    tracked = JSON.parse(tracked);
                    this._mergeCanvasToChild(tracked);
                    childShape.childShapes.push(tracked);
                }
            }
        },
        saveAsJson: function () {
            return JSON.stringify(this.getModel());
        },
        dispatchOryxEvent: function(event){
            KISBPM.eventBus.dispatchOryxEvent(event);
        },
        getModelMetaData:function(){
            return this.modelData;
        }
    });

    return new editorManager();
}]);
