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
var NORMAL_STROKE = 1;
var ASSOCIATION_STROKE = 2;
var TASK_STROKE = 1;
var TASK_HIGHLIGHT_STROKE = 2;

var COMPLETED_COLOR = "#2632aa";
var TEXT_COLOR= "#373e48";
var CURRENT_COLOR= "#017501";
var AVAILABLE_COLOR = "#e3da82";
var HOVER_COLOR= "#666666";
var ACTIVITY_STROKE_COLOR = "#bbbbbb";
var ACTIVITY_FILL_COLOR = "#f9f9f9";
var WHITE_FILL_COLOR = "#ffffff";
var MAIN_STROKE_COLOR = "#585858";

var TEXT_PADDING = 3;
var ARROW_WIDTH = 4;
var MARKER_WIDTH = 12;

var TASK_FONT = {font: "11px Arial", opacity: 1, fill: Raphael.rgb(0, 0, 0)};

// icons
var ICON_SIZE = 16;
var ICON_PADDING = 4;

var INITIAL_CANVAS_WIDTH;
var INITIAL_CANVAS_HEIGHT;

var paper;
var viewBox;
var viewBoxWidth;
var viewBoxHeight;

var canvasWidth;
var canvasHeight;

var modelDiv = jQuery('#cmmnModel');
var modelId = modelDiv.attr('data-model-id');
var historyModelId = modelDiv.attr('data-history-id');
var caseDefinitionId = modelDiv.attr('data-definition-id');
var instanceId = modelDiv.attr('data-instance-id');
var modelType = modelDiv.attr('data-model-type');

var migrationDefinitionId = $('#targetModel').attr('data-migration-definition-id');

var elementsAdded = new Array();
var elementsRemoved = new Array();

var changeStateElementIds = new Array();
var changeStateElements = new Array();
var changeStateGlowElements = new Array();

var migrationMappedElements = new Array();
var migrationMappedElementsText = new Array();

function _showTip(htmlNode, element)
{
    // Default tooltip, no custom tool tip set
    if (documentation === undefined) {
        var documentation = "";
        if (element.name && element.name.length > 0) {
            documentation += "<b>Name</b>: <i>" + element.name + "</i><br/><br/>";
        }

        if (element.properties) {
            for (var i = 0; i < element.properties.length; i++) {
                var propName = element.properties[i].name;
                if (element.properties[i].type && element.properties[i].type === 'list') {
                    documentation += '<b>' + propName + '</b>:<br/>';
                    for (var j = 0; j < element.properties[i].value.length; j++) {
                        documentation += '<i>' + element.properties[i].value[j] + '</i><br/>';
                    }
                }
                else {
                    documentation += '<b>' + propName + '</b>: <i>' + element.properties[i].value + '</i><br/>';
                }
            }
        }
    }

    var text = element.type + " ";
    if (element.name && element.name.length > 0) {
        text += element.name;
    } else {
        text += element.id;
    }

    htmlNode.qtip({
        content: {
            text: documentation,
            title: {
                text: text
            }
        },
        position: {
            my: 'top left',
            at: 'bottom center',
            viewport: jQuery('#cmmnModel')
        },
        hide: {
            fixed: true, delay: 500,
            event: 'click mouseleave'
        },
        style: {
            classes: 'ui-tooltip-flowable-cmmn'
        }
    });
}

function _addHoverLogic(element, type, defaultColor, isMigrationModelElement, currentPaper)
{
    var strokeColor = _cmmnGetColor(element, defaultColor);
    var topBodyRect = null;
    if (type === "rect") {
        topBodyRect = currentPaper.rect(element.x, element.y, element.width, element.height);
        
    } else if (type === "circle") {
        var x = element.x + (element.width / 2);
        var y = element.y + (element.height / 2);
        topBodyRect = currentPaper.circle(x, y, 15);
        
    } else if (type === "rhombus") {
        topBodyRect = currentPaper.path("M" + element.x + " " + (element.y + (element.height / 2)) +
            "L" + (element.x + (element.width / 2)) + " " + (element.y + element.height) +
            "L" + (element.x + element.width) + " " + (element.y + (element.height / 2)) +
            "L" + (element.x + (element.width / 2)) + " " + element.y + "z"
        );
    }
    
    if (isMigrationModelElement) {
        	    
        topBodyRect.attr({
            "opacity": 0,
            "stroke" : "none",
            "fill" : "#ffffff"
        });
    
        topBodyRect.click(function() {
            var elementIndex = $.inArray(element.id, changeStateElementIds);
            if (elementIndex >= 0) {
            
                var glowElement = changeStateGlowElements[elementIndex];
                glowElement.remove();

				changeStateGlowElements.splice(elementIndex, 1);
                changeStateElementIds.splice(elementIndex, 1);
                changeStateElements.splice(elementIndex, 1);
                
            } else {
                var glowElement = topBodyRect.glow({'color': 'red'});
                changeStateGlowElements.push(glowElement);
                changeStateElementIds.push(element.id);
                changeStateElements.push(element);
            }
        });
    
    } else {
	    var opacity = 0;
	    var fillColor = "#ffffff";
	    if (jQuery.inArray(element.id, elementsAdded) >= 0) {
	        opacity = 0.2;
	        fillColor = "green";
	    }
	
	    if (jQuery.inArray(element.id, elementsRemoved) >= 0) {
	        opacity = 0.2;
	        fillColor = "red";
	    }
	
	    topBodyRect.attr({
	        "opacity": opacity,
	        "stroke" : "none",
	        "fill" : fillColor
	    });
	    _showTip(jQuery(topBodyRect.node), element);
	    
	    topBodyRect.click(function() {
		    if (migrationDefinitionId && migrationDefinitionId.length > 0) {
		        var elementIndex = $.inArray(element.id, changeStateElementIds);
	            if (elementIndex >= 0) {
	            
	                var glowElement = changeStateGlowElements[elementIndex];
                	glowElement.remove();

					changeStateGlowElements.splice(elementIndex, 1);
                	changeStateElementIds.splice(elementIndex, 1);
                	changeStateElements.splice(elementIndex, 1);
	                
	            } else {
	                var glowElement = topBodyRect.glow({'color': 'red'});
                	changeStateGlowElements.push(glowElement);
                	changeStateElementIds.push(element.id);
                	changeStateElements.push(element);
	            }
	            
		    } else {
	            var elementIndex = $.inArray(element.id, changeStateElementIds);
	            if (elementIndex >= 0) {
	                
	                var glowElement = changeStateGlowElements[elementIndex];
	                glowElement.remove();
	                
	                changeStateGlowElements.splice(elementIndex, 1);
                	changeStateElementIds.splice(elementIndex, 1);
                	changeStateElements.splice(elementIndex, 1);
	                
	            } else {
	                var startGlowElement = topBodyRect.glow({'color': 'blue'});
                   	changeStateGlowElements.push(startGlowElement);
                   	changeStateElementIds.push(element.id);
                   	changeStateElements.push(element);
                   	
                   	if (element.current) {
                   		$('#changeStateToActivateButton').hide();
                   		$('#changeStateToAvailableButton').hide();
                   		$('#changeStateToTerminateButton').show();
                   		
                   	} else if (element.available) {
                   		$('#changeStateToAvailableButton').hide();
                   		$('#changeStateToActivateButton').show();
	               		$('#changeStateToTerminateButton').show();
	                
	                } else if (element.completed) {
	                	$('#changeStateToAvailableButton').show();
	                	$('#changeStateToActivateButton').hide();
	               		$('#changeStateToTerminateButton').hide();
	                
	                } else {
	                	$('#changeStateToAvailableButton').show();
	                	$('#changeStateToActivateButton').show();
	               		$('#changeStateToTerminateButton').hide();
	                }
	            }
	            
	            if (changeStateElements.length == 0) {
	               $('#changeStateToActivateButton').hide();
	               $('#changeStateToAvailableButton').hide();
	               $('#changeStateToTerminateButton').hide();
	            }
	        }
	    });
	}

    topBodyRect.mouseover(function() {
        currentPaper.getById(element.id).attr({"stroke":HOVER_COLOR});
    });

    topBodyRect.mouseout(function() {
        currentPaper.getById(element.id).attr({"stroke":strokeColor});
    });
}

function _zoom(zoomIn, currentPaper)
{
    var tmpCanvasWidth, tmpCanvasHeight;
    if (zoomIn) {
        tmpCanvasWidth = canvasWidth * (1.0/0.90);
        tmpCanvasHeight = canvasHeight * (1.0/0.90);
        
    } else {
        tmpCanvasWidth = canvasWidth * (1.0/1.10);
        tmpCanvasHeight = canvasHeight * (1.0/1.10);
    }

    if (tmpCanvasWidth != canvasWidth || tmpCanvasHeight != canvasHeight) {
        canvasWidth = tmpCanvasWidth;
        canvasHeight = tmpCanvasHeight;
        currentPaper.setSize(canvasWidth, canvasHeight);
    }
}

function _showCaseInstanceDiagram() {
	var modelUrl;
	if (instanceId != null) {
		modelUrl = FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-instances/' + instanceId + '/model-json';
		
	} else {
		modelUrl = FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-definitions/' + caseDefinitionId + '/model-json';
	}
	
	var request = jQuery.ajax({
	    type: 'get',
	    url: modelUrl + '?nocaching=' + new Date().getTime()
	});
	
	request.success(function(data, textStatus, jqXHR) {
	
	    if ((!data.elements || data.elements.length == 0) && (!data.pools || data.pools.length == 0)) return;
	
	    INITIAL_CANVAS_WIDTH = data.diagramWidth;
	    
	    if (modelType == 'design') {
	    	INITIAL_CANVAS_WIDTH += 20;
	    } else {
	        INITIAL_CANVAS_WIDTH += 30;
	    }
	    
	    INITIAL_CANVAS_HEIGHT = data.diagramHeight + 50;
	    canvasWidth = INITIAL_CANVAS_WIDTH;
	    canvasHeight = INITIAL_CANVAS_HEIGHT;
	    viewBoxWidth = INITIAL_CANVAS_WIDTH;
	    viewBoxHeight = INITIAL_CANVAS_HEIGHT;
	    
	    if (modelType == 'design') {
	    	var headerBarHeight = 170;
	    	var offsetY = 0;
	    	if (jQuery(window).height() > (canvasHeight + headerBarHeight)) {
	        	offsetY = (jQuery(window).height() - headerBarHeight - canvasHeight) / 2;
	    	}
	
	    	if (offsetY > 50) {
	        	offsetY = 50;
	    	}
	
	    	jQuery('#cmmnModel').css('marginTop', offsetY);
	    }
	
	    jQuery('#cmmnModel').width(INITIAL_CANVAS_WIDTH);
	    jQuery('#cmmnModel').height(INITIAL_CANVAS_HEIGHT);
	    paper = Raphael(document.getElementById('cmmnModel'), canvasWidth, canvasHeight);
	    paper.setViewBox(0, 0, viewBoxWidth, viewBoxHeight, false);
	    paper.renderfix();
	
	    var modelElements = data.elements;
	    for (var i = 0; i < modelElements.length; i++) {
	        var element = modelElements[i];
	        //try {
	        var drawFunction = eval("_draw" + element.type);
	        drawFunction(element, false, paper);
	        //} catch(err) {console.log(err);}
	    }
	
	    if (data.flows) {
	        for (var i = 0; i < data.flows.length; i++) {
	            var flow = data.flows[i];
	            _drawAssociation(flow, paper);
	        }
	    }
	    
	    if (migrationDefinitionId && migrationDefinitionId.length > 0) {
	          migrationRequest = $.ajax({
	                type: 'get',
	                url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-definitions/' + migrationDefinitionId + '/model-json?nocaching=' + new Date().getTime()
	          });
	          
	          migrationRequest.success(function(data, textStatus, jqXHR) {
	            
	            if (!data.elements || data.elements.length == 0) return;
	            
	            INITIAL_CANVAS_WIDTH = data.diagramWidth + 30;
	            INITIAL_CANVAS_HEIGHT = data.diagramHeight + 50;
	            canvasWidth = INITIAL_CANVAS_WIDTH;
	            canvasHeight = INITIAL_CANVAS_HEIGHT;
	            viewBoxWidth = INITIAL_CANVAS_WIDTH;
	            viewBoxHeight = INITIAL_CANVAS_HEIGHT;
	            
	            var x = 0;
	            if ($(window).width() > canvasWidth) {
	                x = ($(window).width() - canvasWidth) / 2 - (data.diagramBeginX / 2);
	            }
	            
	            var canvasValue = 'targetModel';
	            
	            $('#' + canvasValue).width(INITIAL_CANVAS_WIDTH);
	            $('#' + canvasValue).height(INITIAL_CANVAS_HEIGHT);
	            migrationPaper = Raphael(document.getElementById(canvasValue), canvasWidth, canvasHeight);
	            migrationPaper.setViewBox(0, 0, viewBoxWidth, viewBoxHeight, false);
	            migrationPaper.renderfix();
	            
	            var modelElements = data.elements;
			    for (var i = 0; i < modelElements.length; i++) {
			        var element = modelElements[i];
			        //try {
			        var drawFunction = eval("_draw" + element.type);
			        drawFunction(element, true, migrationPaper);
			        //} catch(err) {console.log(err);}
			    }
			
			    if (data.flows) {
			        for (var i = 0; i < data.flows.length; i++) {
			            var flow = data.flows[i];
			            _drawAssociation(flow, migrationPaper);
			        }
			    }
	            
	            migrationData = data;
	          });
	    }
	});
	
	request.error(function(jqXHR, textStatus, errorThrown) {
	    alert("error");
	});
}

function showMigrationMappings() {
	var mappingValuesText = '';
    var counter = 1;
    for (var i = 0; i < migrationMappedElementsText.length; i++) {
        if (mappingValuesText.length > 0) {
            mappingValuesText += '  -  ';
        }
        mappingValuesText += 'Mapping ' + counter + ' ' + migrationMappedElementsText[i];
        
        counter++;
    }
    $('#currentMappingValues').text(mappingValuesText);
}

$(document).ready(function () {
     $('#changeStateToActivateButton').on('click', function(e) {
        e.preventDefault();
        
        var activateItemIds = new Array();
        for (var i = 0; i < changeStateElements.length; i++) {
            activateItemIds.push(changeStateElements[i].planItemDefinitionId);
        }
        
        $.confirm({
            title: 'Activate plan item definitions?',
            content: 'Are you sure you want to activate the selected plan item definitions?',
            buttons: {
                confirm: function () {
                    $.ajax({
                        type: 'post',
                        url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-instances/' + instanceId + '/change-state',
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            activatePlanItemDefinitionIds: activateItemIds
                        }),
                        success: function() {
                            paper.clear();
                            $('#changeStateToActivateButton').hide();
                            $('#changeStateToAvailableButton').hide();
                            $('#changeStateToTerminateButton').hide();
                            changeStateGlowElements = new Array();
                			changeStateElementIds = new Array();
                			changeStateElements = new Array();
                            _showCaseInstanceDiagram();
                        }
                    });
                },
                cancel: function () {
                    
                }
            }
        });
     });
     
     $('#changeStateToAvailableButton').on('click', function(e) {
        e.preventDefault();
        
        var availableItemIds = new Array();
        for (var i = 0; i < changeStateElements.length; i++) {
            availableItemIds.push(changeStateElements[i].planItemDefinitionId);
        }
        
        $.confirm({
            title: 'Move plan item definitions to available state?',
            content: 'Are you sure you want to move the selected plan item definitions to available state?',
            buttons: {
                confirm: function () {
                    $.ajax({
                        type: 'post',
                        url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-instances/' + instanceId + '/change-state',
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            moveToAvailablePlanItemDefinitionIds: availableItemIds
                        }),
                        success: function() {
                            paper.clear();
                            $('#changeStateToActivateButton').hide();
                            $('#changeStateToAvailableButton').hide();
                            $('#changeStateToTerminateButton').hide();
                            changeStateGlowElements = new Array();
                			changeStateElementIds = new Array();
                			changeStateElements = new Array();
                            _showCaseInstanceDiagram();
                        }
                    });
                },
                cancel: function () {
                    
                }
            }
        });
     });
     
     $('#changeStateToTerminateButton').on('click', function(e) {
        e.preventDefault();
        
        var terminateItemIds = new Array();
        for (var i = 0; i < changeStateElements.length; i++) {
            terminateItemIds.push(changeStateElements[i].planItemDefinitionId);
        }
        
        $.confirm({
            title: 'Terminate plan item definitions?',
            content: 'Are you sure you want to terminate the selected plan item definitions?',
            buttons: {
                confirm: function () {
                    $.ajax({
                        type: 'post',
                        url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-instances/' + instanceId + '/change-state',
                        contentType: 'application/json; charset=utf-8',
                        data: JSON.stringify({
                            terminatePlanItemDefinitionIds: terminateItemIds
                        }),
                        success: function() {
                            paper.clear();
                            $('#changeStateToActivateButton').hide();
                            $('#changeStateToAvailableButton').hide();
                            $('#changeStateToTerminateButton').hide();
                            changeStateGlowElements = new Array();
                			changeStateElementIds = new Array();
                			changeStateElements = new Array();
                            _showCaseInstanceDiagram();
                        }
                    });
                },
                cancel: function () {
                    
                }
            }
        });
     });
     
     $('#addActivatePlanItemDefinition').on('click', function(e) {
        e.preventDefault();
        
        var activateItemIds = new Array();
        var elementText = '';
        for (var i = 0; i < changeStateElements.length; i++) {
            if (elementText.length > 0) {
                elementText += ', ';
            }
            elementText += changeStateElements[i].name;
            activateItemIds.push(changeStateElements[i].planItemDefinitionId);
            
            var glowElement = changeStateGlowElements[i];
    		glowElement.remove();
        }
        
        migrationMappedElements.push({
            activateItemIds: activateItemIds
        });
        
        migrationMappedElementsText.push('Activate ' + elementText);
        
        showMigrationMappings();
     });
     
     $('#addAvailablePlanItemDefinition').on('click', function(e) {
        e.preventDefault();
        
        var availableItemIds = new Array();
        var elementText = '';
        for (var i = 0; i < changeStateElements.length; i++) {
            if (elementText.length > 0) {
                elementText += ', ';
            }
            elementText += changeStateElements[i].name;
            availableItemIds.push(changeStateElements[i].planItemDefinitionId);
            
            var glowElement = changeStateGlowElements[i];
    		glowElement.remove();
        }
        
        migrationMappedElements.push({
            availableItemIds: availableItemIds
        });
        
        migrationMappedElementsText.push('Move to available ' + elementText);
        
        showMigrationMappings();
     });
     
     $('#addTerminatePlanItemDefinition').on('click', function(e) {
        e.preventDefault();
        
        var terminateItemIds = new Array();
        var elementText = '';
        for (var i = 0; i < changeStateElements.length; i++) {
            if (elementText.length > 0) {
                elementText += ', ';
            }
            elementText += changeStateElements[i].name;
            terminateItemIds.push(changeStateElements[i].planItemDefinitionId);
            
            var glowElement = changeStateGlowElements[i];
    		glowElement.remove();
        }
        
        migrationMappedElements.push({
            terminateItemIds: terminateItemIds
        });
        
        migrationMappedElementsText.push('Terminate ' + elementText);
        
        showMigrationMappings();
     });
     
     $('#executeMigrationDocument').on('click', function(e) {
        e.preventDefault();
        
        var migrationDocumentPayload = {
            toCaseDefinitionId: migrationDefinitionId
        };
        
        var activatePlanItemDefinitionMappings = new Array();
        var moveToAvailablePlanItemDefinitionMappings = new Array();
        var terminatePlanItemDefinitionMappings = new Array();
        for (var i = 0; i < migrationMappedElements.length; i++) {
            var mappedElement = migrationMappedElements[i];
            if (mappedElement.activateItemIds) {
                for (var j = 0; j < mappedElement.activateItemIds.length; j++) {
                	activatePlanItemDefinitionMappings.push({
                		planItemDefinitionId: mappedElement.activateItemIds[j]
                	});
                }
                
            } else if (mappedElement.availableItemIds) {
                for (var j = 0; j < mappedElement.availableItemIds.length; j++) {
                	moveToAvailablePlanItemDefinitionMappings.push({
                		planItemDefinitionId: mappedElement.availableItemIds[j]
                	});
                }
            
            } else if (mappedElement.terminateItemIds) {
                for (var j = 0; j < mappedElement.terminateItemIds.length; j++) {
                	terminatePlanItemDefinitionMappings.push({
                		planItemDefinitionId: mappedElement.terminateItemIds[j]
                	});
                }
            }
        }
        
        migrationDocumentPayload.activatePlanItemDefinitions = activatePlanItemDefinitionMappings;
        migrationDocumentPayload.moveToAvailablePlanItemDefinitions = moveToAvailablePlanItemDefinitionMappings;
        migrationDocumentPayload.terminatePlanItemDefinitions = terminatePlanItemDefinitionMappings;
        
        if (instanceId != null) {
        
            $.confirm({
                title: 'Migrate case instance',
                content: 'Are you sure you want to migrate the case instance with the defined mappings',
                buttons: {
                    confirm: function () {
                        $.ajax({
                            type: 'post',
                            url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-instances/' + instanceId + '/migrate',
                            contentType: 'application/json; charset=utf-8',
                            data: JSON.stringify(migrationDocumentPayload),
                            success: function() {
                                paper.clear();
                                migrationPaper.clear();
                                $('#targetModel').removeAttr('data-migration-definition-id');
                                definitionId = migrationDefinitionId;
                                migrationDefinitionId = undefined;
                                migrationMappedElementsText = new Array();
                                migrationMappedElements = new Array();
                                $('#targetModel').hide();
                                $('#migrationDivider').hide();
                                $('#executeMigrationDocument').hide();
                                $('#currentMappingValues').hide();
                                changeStateGlowElements = new Array();
                				changeStateElementIds = new Array();
                				changeStateElements = new Array();
                                _showCaseInstanceDiagram();
                            }
                        });
                    },
                    cancel: function () {
                        
                    }
                }
            });
            
        } else {
        
            $.confirm({
                title: 'Migrate all instances of case definition',
                content: 'Are you sure you want to migrate all instances of the case definition with the defined mappings',
                buttons: {
                    confirm: function () {
                        $.ajax({
                            type: 'post',
                            url: FlowableAdmin.Config.adminContextRoot + 'rest/admin/case-definitions/' + definitionId + '/batch-migrate',
                            contentType: 'application/json; charset=utf-8',
                            data: JSON.stringify(migrationDocumentPayload),
                            success: function() {
                                paper.clear();
                                migrationPaper.clear();
                                $('#targetModel').removeAttr('data-migration-definition-id');
                                definitionId = migrationDefinitionId;
                                migrationDefinitionId = undefined;
                                migrationMappedElementsText = new Array();
                                migrationMappedElements = new Array();
                                $('#targetModel').hide();
                                $('#migrationDivider').hide();
                                $('#executeMigrationDocument').hide();
                                $('#currentMappingValues').hide();
                            }
                        });
                    },
                    cancel: function () {
                        
                    }
                }
            });
        }
     });
     
     _showCaseInstanceDiagram();
});
