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
'use strict';

var FLOWABLE = FLOWABLE || {};
FLOWABLE.TOOLBAR = {
    ACTIONS: {
    	
        saveModel: function (services) {

            _internalCreateModal({
                backdrop: true,
                keyboard: true,
                template: 'editor-app/popups/save-model.html?version=' + Date.now(),
                scope: services.$scope
            }, services.$modal, services.$scope);
        },
        
        validate: function(services) {
        	
        	_internalCreateModal({
                backdrop: true,
                keyboard: true,
                template: 'editor-app/popups/validate-model.html?version=' + Date.now(),
                scope: services.$scope
            }, services.$modal, services.$scope);
		},

        undo: function (services) {

            // Get the last commands
            var lastCommands = services.$scope.undoStack.pop();

            if (lastCommands) {
                // Add the commands to the redo stack
                services.$scope.redoStack.push(lastCommands);

                // Force refresh of selection, might be that the undo command
                // impacts properties in the selected item
                if (services.$rootScope && services.$rootScope.forceSelectionRefresh) 
                {
                	services.$rootScope.forceSelectionRefresh = true;
                }
                
                // Rollback every command
                for (var i = lastCommands.length - 1; i >= 0; --i) {
                    lastCommands[i].rollback();
                }
                
                // Update and refresh the canvas
                services.editorManager.handleEvents({
                    type: ORYX.CONFIG.EVENT_UNDO_ROLLBACK,
                    commands: lastCommands
                });
                
                // Update
                services.editorManager.getCanvas().update();
                services.editorManager.updateSelection();
            }
            
            var toggleUndo = false;
            if (services.$scope.undoStack.length == 0)
            {
            	toggleUndo = true;
            }
            
            var toggleRedo = false;
            if (services.$scope.redoStack.length > 0)
            {
            	toggleRedo = true;
            }

            if (toggleUndo || toggleRedo) {
                for (var i = 0; i < services.$scope.items.length; i++) {
                    var item = services.$scope.items[i];
                    if (toggleUndo && item.action === 'FLOWABLE.TOOLBAR.ACTIONS.undo') {
                        services.$scope.safeApply(function () {
                            item.enabled = false;
                        });
                    }
                    else if (toggleRedo && item.action === 'FLOWABLE.TOOLBAR.ACTIONS.redo') {
                        services.$scope.safeApply(function () {
                            item.enabled = true;
                        });
                    }
                }
            }
        },

        redo: function (services) {

            // Get the last commands from the redo stack
            var lastCommands = services.$scope.redoStack.pop();

            if (lastCommands) {
                // Add this commands to the undo stack
                services.$scope.undoStack.push(lastCommands);
                
                // Force refresh of selection, might be that the redo command
                // impacts properties in the selected item
                if (services.$rootScope && services.$rootScope.forceSelectionRefresh) 
                {
                	services.$rootScope.forceSelectionRefresh = true;
                }

                // Execute those commands
                lastCommands.each(function (command) {
                    command.execute();
                });

                // Update and refresh the canvas
                services.editorManager.handleEvents({
                    type: ORYX.CONFIG.EVENT_UNDO_EXECUTE,
                    commands: lastCommands
                });

                // Update
                services.editorManager.getCanvas().update();
                services.editorManager.updateSelection();
            }

            var toggleUndo = false;
            if (services.$scope.undoStack.length > 0) {
                toggleUndo = true;
            }

            var toggleRedo = false;
            if (services.$scope.redoStack.length == 0) {
                toggleRedo = true;
            }

            if (toggleUndo || toggleRedo) {
                for (var i = 0; i < services.$scope.items.length; i++) {
                    var item = services.$scope.items[i];
                    if (toggleUndo && item.action === 'FLOWABLE.TOOLBAR.ACTIONS.undo') {
                        services.$scope.safeApply(function () {
                            item.enabled = true;
                        });
                    }
                    else if (toggleRedo && item.action === 'FLOWABLE.TOOLBAR.ACTIONS.redo') {
                        services.$scope.safeApply(function () {
                            item.enabled = false;
                        });
                    }
                }
            }
        },

        cut: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxEditPlugin(services).editCut();
            for (var i = 0; i < services.$scope.items.length; i++) {
                var item = services.$scope.items[i];
                if (item.action === 'FLOWABLE.TOOLBAR.ACTIONS.paste') {
                    services.$scope.safeApply(function () {
                        item.enabled = true;
                    });
                }
            }
        },

        copy: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxEditPlugin(services).editCopy();
            for (var i = 0; i < services.$scope.items.length; i++) {
                var item = services.$scope.items[i];
                if (item.action === 'FLOWABLE.TOOLBAR.ACTIONS.paste') {
                    services.$scope.safeApply(function () {
                        item.enabled = true;
                    });
                }
            }
        },

        paste: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxEditPlugin(services).editPaste();
        },

        deleteItem: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxEditPlugin(services).editDelete();
        },

        addBendPoint: function (services) {

            // Show the tutorial the first time
            FLOWABLE_EDITOR_TOUR.sequenceFlowBendpoint(services.$scope, services.$translate, services.$q, true);

            var dockerPlugin = FLOWABLE.TOOLBAR.ACTIONS._getOryxDockerPlugin(services);

            var enableAdd = !dockerPlugin.enabledAdd();
            dockerPlugin.setEnableAdd(enableAdd);
            if (enableAdd)
            {
            	dockerPlugin.setEnableRemove(false);
            	document.body.style.cursor = 'pointer';
            }
            else
            {
            	document.body.style.cursor = 'default';
            }
        },

        removeBendPoint: function (services) {

            // Show the tutorial the first time
            FLOWABLE_EDITOR_TOUR.sequenceFlowBendpoint(services.$scope, services.$translate, services.$q, true);

            var dockerPlugin = FLOWABLE.TOOLBAR.ACTIONS._getOryxDockerPlugin(services);

            var enableRemove = !dockerPlugin.enabledRemove();
            dockerPlugin.setEnableRemove(enableRemove);
            if (enableRemove)
            {
            	dockerPlugin.setEnableAdd(false);
            	document.body.style.cursor = 'pointer';
            }
            else
            {
            	document.body.style.cursor = 'default';
            }
        },

        /**
         * Helper method: fetches the Oryx Edit plugin from the provided scope,
         * if not on the scope, it is created and put on the scope for further use.
         *
         * It's important to reuse the same EditPlugin while the same scope is active,
         * as the clipboard is stored for the whole lifetime of the scope.
         */
        _getOryxEditPlugin: function (services) {
        	var $scope = services.$scope;
			var editorManager = services.editorManager;
            if ($scope.oryxEditPlugin === undefined || $scope.oryxEditPlugin === null) {
                $scope.oryxEditPlugin = new ORYX.Plugins.Edit(editorManager.getEditor());
            }
            return $scope.oryxEditPlugin;
        },

        zoomIn: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxViewPlugin(services).zoom([1.0 + ORYX.CONFIG.ZOOM_OFFSET]);
        },

        zoomOut: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxViewPlugin(services).zoom([1.0 - ORYX.CONFIG.ZOOM_OFFSET]);
        },
        
        zoomActual: function (services) {
            FLOWABLE.TOOLBAR.ACTIONS._getOryxViewPlugin(services).setAFixZoomLevel(1);
        },
        
        zoomFit: function (services) {
        	FLOWABLE.TOOLBAR.ACTIONS._getOryxViewPlugin(services).zoomFitToModel();
        },
        
        alignVertical: function (services) {
        	FLOWABLE.TOOLBAR.ACTIONS._getOryxArrangmentPlugin(services).alignShapes([ORYX.CONFIG.EDITOR_ALIGN_CENTER]);
        },
        
        alignHorizontal: function (services) {
        	FLOWABLE.TOOLBAR.ACTIONS._getOryxArrangmentPlugin(services).alignShapes([ORYX.CONFIG.EDITOR_ALIGN_MIDDLE]);
        },
        
        sameSize: function (services) {
        	FLOWABLE.TOOLBAR.ACTIONS._getOryxArrangmentPlugin(services).alignShapes([ORYX.CONFIG.EDITOR_ALIGN_SIZE]);
        },

        help: function (services) {
            FLOWABLE_EDITOR_TOUR.gettingStarted(services.$scope, services.$translate, services.$q);
        },
        
        /**
         * Helper method: fetches the Oryx View plugin from the provided scope,
         * if not on the scope, it is created and put on the scope for further use.
         */
        _getOryxViewPlugin: function (services) {
        	var $scope = services.$scope;
			var editorManager = services.editorManager;
            if ($scope.oryxViewPlugin === undefined || $scope.oryxViewPlugin === null) {
                $scope.oryxViewPlugin = new ORYX.Plugins.View(editorManager.getEditor());
            }
            return $scope.oryxViewPlugin;
        },
        
        _getOryxArrangmentPlugin: function (services) {
        	var $scope = services.$scope;
			var editorManager = services.editorManager;
            if ($scope.oryxArrangmentPlugin === undefined || $scope.oryxArrangmentPlugin === null) {
                $scope.oryxArrangmentPlugin = new ORYX.Plugins.Arrangement(editorManager.getEditor());
            }
            return $scope.oryxArrangmentPlugin;
        },

        _getOryxDockerPlugin: function (services) {
        	var $scope = services.$scope;
			var editorManager = services.editorManager;
            if ($scope.oryxDockerPlugin === undefined || $scope.oryxDockerPlugin === null) {
                $scope.oryxDockerPlugin = new ORYX.Plugins.AddDocker(editorManager.getEditor());
            }
            return $scope.oryxDockerPlugin;
        }
    }
};

/** Custom controller for the save dialog */
angular.module('flowableModeler').controller('SaveModelCtrl', [ '$rootScope', '$scope', '$http', '$route', '$location', 'editorManager',
    function ($rootScope, $scope, $http, $route, $location, editorManager) {

	if (editorManager.getCurrentModelId() != editorManager.getModelId()) {
		editorManager.edit(editorManager.getModelId());
	}
	
    var modelMetaData = editorManager.getBaseModelData();

    var description = '';
    if (modelMetaData.description) {
    	description = modelMetaData.description;
    }
    
    var saveDialog = { 
    	'name' : modelMetaData.name,
    	'key' : modelMetaData.key,
        'description' : description,
        'newVersion' : false,
        'comment' : ''
    };
    
    $scope.saveDialog = saveDialog;
    
    $scope.status = {
        loading: false
    };

    $scope.close = function () {
    	$scope.$hide();
    };

    $scope.saveAndClose = function () {
    	$scope.save(function() {
            if (editorManager.getStencilData()) {
                var stencilNameSpace = editorManager.getStencilData().namespace;
                if (stencilNameSpace !== undefined && stencilNameSpace !== null && stencilNameSpace.indexOf('cmmn1.1') !== -1) {
                    $location.path("/casemodels");
                	return;
            	}
        	}
        	$location.path('/processes');
    	});
    };
    
    $scope.save = function (successCallback) {

        if (!$scope.saveDialog.name || $scope.saveDialog.name.length == 0 ||
        	!$scope.saveDialog.key || $scope.saveDialog.key.length == 0) {
        	
            return;
        }

        // Indicator spinner image
        $scope.status = {
        	loading: true
        };
        
        modelMetaData.name = $scope.saveDialog.name;
        modelMetaData.key = $scope.saveDialog.key;
        modelMetaData.description = $scope.saveDialog.description;

        var json = editorManager.getModel();

        var params = {
            modeltype: modelMetaData.model.modelType,
            json_xml: JSON.stringify(json),
            name: $scope.saveDialog.name,
            key: $scope.saveDialog.key,
            description: $scope.saveDialog.description,
            newversion: $scope.saveDialog.newVersion,
            comment: $scope.saveDialog.comment,
            lastUpdated: modelMetaData.lastUpdated
        };

        if ($scope.error && $scope.error.isConflict) {
            params.conflictResolveAction = $scope.error.conflictResolveAction;
            if ($scope.error.conflictResolveAction === 'saveAs') {
                params.saveAs = $scope.error.saveAs;
            }
        }

        // Update
        $http({    method: 'POST',
            data: params,
            ignoreErrors: true,
            headers: {'Accept': 'application/json',
                      'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
            transformRequest: function (obj) {
                var str = [];
                for (var p in obj) {
                    str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                }
                return str.join("&");
            },
            url: FLOWABLE.URL.putModel(modelMetaData.modelId)})

            .success(function (data, status, headers, config) {
                editorManager.handleEvents({
                    type: ORYX.CONFIG.EVENT_SAVED
                });
                $scope.modelData.name = $scope.saveDialog.name;
                $scope.modelData.key = $scope.saveDialog.key;
                $scope.modelData.lastUpdated = data.lastUpdated;
                
                $scope.status.loading = false;
                $scope.$hide();

                // Fire event to all who is listening
                var saveEvent = {
                    type: FLOWABLE.eventBus.EVENT_TYPE_MODEL_SAVED,
                    model: params,
                    modelId: modelMetaData.modelId,
		            eventType: 'update-model'
                };
                FLOWABLE.eventBus.dispatch(FLOWABLE.eventBus.EVENT_TYPE_MODEL_SAVED, saveEvent);

                // Reset state
                $scope.error = undefined;
                $scope.status.loading = false;

                // Execute any callback
                if (successCallback) {
                    successCallback();
                }

            })
            .error(function (data, status, headers, config) {
                if (status == 409) {
                	$scope.error = {};
                    $scope.error.isConflict = true;
                    $scope.error.userFullName = data.customData.userFullName;
                    $scope.error.isNewVersionAllowed = data.customData.newVersionAllowed;
                    $scope.error.saveAs = modelMetaData.name + "_2";
                } else {
                	$scope.error = undefined;
                    $scope.saveDialog.errorMessage = data.message;
                }
                $scope.status.loading = false;
            });
    };

    $scope.isOkButtonDisabled = function() {
        if ($scope.status.loading) {
            return false;
        } else if ($scope.error && $scope.error.conflictResolveAction) {
            if ($scope.error.conflictResolveAction === 'saveAs') {
                return !$scope.error.saveAs || $scope.error.saveAs.length == 0;
            } else {
                return false;
            }
        }
        return true;
    };

    $scope.okClicked = function() {
        if ($scope.error) {
            if ($scope.error.conflictResolveAction === 'discardChanges') {
                $scope.close();
                $route.reload();
            } else if ($scope.error.conflictResolveAction === 'overwrite'
                || $scope.error.conflictResolveAction === 'newVersion') {
                $scope.save();
            } else if($scope.error.conflictResolveAction === 'saveAs') {
                $scope.save(function() {
                    $rootScope.ignoreChanges = true;  // Otherwise will get pop up that changes are not saved.
                    if (editorManager.getStencilData()) {
                        var stencilNameSpace = editorManager.getStencilData().namespace;
                        if (stencilNameSpace !== undefined && stencilNameSpace !== null && stencilNameSpace.indexOf('cmmn1.1') !== -1) {
                            $location.path("/casemodels");
                            return;
                        }
                    }
                    $location.path('/processes');
            	});
            }
        }
    };

}]);

angular.module('flowableModeler').controller('ValidateModelCtrl',['$scope', '$http', 'editorManager',
    function ($scope, $http, editorManager) {
    
        var editor = editorManager.getEditor();
        var model = editorManager.getModel();
        
        $scope.status = {
            loading: true
        };

        $scope.model = {
        	errors: []
        };
        
        $scope.errorGrid = {
            data: $scope.model.errors,
            headerRowHeight: 28,
        	enableRowSelection: true,
        	enableRowHeaderSelection: false,
        	multiSelect: false,
        	modifierKeysToMultiSelect: false,
        	enableHorizontalScrollbar: 0,
			enableColumnMenus: false,
			enableSorting: false,
            columnDefs: [
                {field: 'activityName', displayName: 'Name', width:125},
                {field: 'defaultDescription', displayName: 'Description'},
                {field: 'warning', displayName: 'Critical', cellTemplate:'editor-app/configuration/properties/errorgrid-critical.html', width: 100}
            ]
        };
        
        $scope.errorGrid.onRegisterApi = function(gridApi) {
	        //set gridApi on scope
	        $scope.gridApi = gridApi;
	        gridApi.selection.on.rowSelectionChanged($scope, function(row) {
	            if (row.isSelected) {
	                editorManager.navigateTo(row.entity.activityId);
            		$scope.$hide();
	            }
	        });
	    };

        $http({
            url: FLOWABLE.URL.validateModel(),
            method: 'POST',
            cache: false,
            headers: {
                "Content-Type":"application/json;charset=utf-8"
            },
            data: model
            
        }).then(function(response){
        	$scope.status.loading = false;
            response.data.forEach(function (row) {
                $scope.model.errors.push(row);
            });
            
        },function(response){
            console.log(response);
        });
    }
]);
