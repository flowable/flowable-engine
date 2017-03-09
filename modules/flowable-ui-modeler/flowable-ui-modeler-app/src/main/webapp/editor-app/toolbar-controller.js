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

angular.module('flowableModeler')
    .controller('ToolbarController', ['$scope', '$http', '$modal', '$q', '$rootScope', '$translate', '$location', 'editorManager',
    		function ($scope, $http, $modal, $q, $rootScope, $translate, $location, editorManager) {

    	$scope.editorFactory.promise.then(function () {
	        var toolbarItems = FLOWABLE.TOOLBAR_CONFIG.items;
	        $scope.items = [];
	        
	        for (var i = 0; i < toolbarItems.length; i++)
	        {
	        	if ($rootScope.modelData.model.modelType === 'form')
		        {
		        	if (!toolbarItems[i].disableInForm)
		        	{
		        		$scope.items.push(toolbarItems[i]);
		        	}
		        }
	        	else
	        	{
	        		$scope.items.push(toolbarItems[i]);
	        	}
	        }
    	});
        
        $scope.secondaryItems = FLOWABLE.TOOLBAR_CONFIG.secondaryItems;

        // Call configurable click handler (From http://stackoverflow.com/questions/359788/how-to-execute-a-javascript-function-when-i-have-its-name-as-a-string)
        var executeFunctionByName = function(functionName, context /*, args */) {
            var args = Array.prototype.slice.call(arguments).splice(2);
            var namespaces = functionName.split(".");
            var func = namespaces.pop();
            for(var i = 0; i < namespaces.length; i++) {
                context = context[namespaces[i]];
            }
            return context[func].apply(this, args);
        };

        // Click handler for toolbar buttons
        $scope.toolbarButtonClicked = function(buttonIndex) {

            // Default behaviour
            var buttonClicked = $scope.items[buttonIndex];
            var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
            executeFunctionByName(buttonClicked.action, window, services);

            // Other events
            var event = {
                type : FLOWABLE.eventBus.EVENT_TYPE_TOOLBAR_BUTTON_CLICKED,
                toolbarItem : buttonClicked
            };
            FLOWABLE.eventBus.dispatch(event.type, event);
        };
        
        // Click handler for secondary toolbar buttons
        $scope.toolbarSecondaryButtonClicked = function(buttonIndex) {
            var buttonClicked = $scope.secondaryItems[buttonIndex];
            var services = { '$scope' : $scope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, '$location': $location, 'editorManager' : editorManager};
            executeFunctionByName(buttonClicked.action, window, services);
        };
        
        /* Key bindings */
        Mousetrap.bind('mod+z', function(e) {
        	var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
        	FLOWABLE.TOOLBAR.ACTIONS.undo(services);
            return false;
        });
        
        Mousetrap.bind('mod+y', function(e) {
        	var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
        	FLOWABLE.TOOLBAR.ACTIONS.redo(services);
            return false;
        });
        
        Mousetrap.bind('mod+c', function(e) {
        	var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
        	FLOWABLE.TOOLBAR.ACTIONS.copy(services);
            return false;
        });
        
        Mousetrap.bind('mod+v', function(e) {
        	var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
        	FLOWABLE.TOOLBAR.ACTIONS.paste(services);
            return false;
        });
        
        Mousetrap.bind(['del'], function(e) {
        	var services = { '$scope' : $scope, '$rootScope' : $rootScope, '$http' : $http, '$modal' : $modal, '$q' : $q, '$translate' : $translate, 'editorManager' : editorManager};
        	FLOWABLE.TOOLBAR.ACTIONS.deleteItem(services);
            return false;
        });

        /* Undo logic */

        $scope.undoStack = [];
        $scope.redoStack = [];
        
        FLOWABLE.eventBus.addListener(FLOWABLE.eventBus.EVENT_TYPE_UNDO_REDO_RESET,function($scope){
			this.undoStack = [];
			this.redoStack = [];
			if (this.items) {
				for(var i = 0; i < this.items.length; i++) {
					var item = this.items[i];
					if (item.action === 'FLOWABLE.TOOLBAR.ACTIONS.undo' || item.action === "FLOWABLE.TOOLBAR.ACTIONS.redo"){
						item.enabled = false;
					}
				}
			}
			
		},$scope);

        $scope.editorFactory.promise.then(function() {

            // Catch all command that are executed and store them on the respective stacks
            editorManager.registerOnEvent(ORYX.CONFIG.EVENT_EXECUTE_COMMANDS, function( evt ){

                // If the event has commands
                if( !evt.commands ){ return; }

                $scope.undoStack.push( evt.commands );
                $scope.redoStack = [];
                
                for(var i = 0; i < $scope.items.length; i++) 
        		{
                    var item = $scope.items[i];
                    if (item.action === 'FLOWABLE.TOOLBAR.ACTIONS.undo')
                    {
                    	item.enabled = true;
                    }
                    else if (item.action === 'FLOWABLE.TOOLBAR.ACTIONS.redo')
                    {
                    	item.enabled = false;
                    }
        		}

                // Update
                editorManager.getCanvas().update();
                editorManager.updateSelection();

            });

        });
        
        // Handle enable/disable toolbar buttons 
        $scope.editorFactory.promise.then(function() {
        	editorManager.registerOnEvent(ORYX.CONFIG.EVENT_SELECTION_CHANGED, function( evt ){
        		var elements = evt.elements;
        		
        		for(var i = 0; i < $scope.items.length; i++)  {
                    var item = $scope.items[i];
                    if (item.enabledAction && item.enabledAction === 'element') {
                    	var minLength = 1;
                    	if (item.minSelectionCount) {
                    		minLength = item.minSelectionCount;
                    	}
                    	
                    	if (elements.length >= minLength && !item.enabled) {
                    		$scope.safeApply(function () {
                    			item.enabled = true;
                            });
                    	} else if (elements.length == 0 && item.enabled) {
                    		$scope.safeApply(function () {
                    			item.enabled = false;
                            });
                    	}
                    }
                }
        	});
        	
        });

    }]);