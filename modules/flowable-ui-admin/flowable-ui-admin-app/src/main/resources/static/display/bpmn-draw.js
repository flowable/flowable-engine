/* Copyright 2005-2015 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

function _bpmnGetColor(element, defaultColor) {
  var strokeColor;
  if(element.current) {
    strokeColor = CURRENT_COLOR;
  } else if(element.completed) {
    strokeColor = COMPLETED_COLOR;
  } else {
    strokeColor = defaultColor;
  }
  return strokeColor;
}

function _drawPool(pool, isMigrationModelElement, currentPaper)
{
	var rect = currentPaper.rect(pool.x, pool.y, pool.width, pool.height);

	rect.attr({"stroke-width": 1,
		"stroke": MAIN_STROKE_COLOR,
		"fill": "white"
 	});

	if (pool.name) {
		var poolName = currentPaper.text(pool.x + 14, pool.y + (pool.height / 2), pool.name).attr({
	        "text-anchor" : "middle",
	        "font-family" : "Arial",
	        "font-size" : "12",
	        "fill" : MAIN_STROKE_COLOR
	  	});

		poolName.transform("r270");
	}

	if (pool.lanes) {
		for (var i = 0; i < pool.lanes.length; i++)
		{
			var lane = pool.lanes[i];
			_drawLane(lane, isMigrationModelElement, currentPaper);
		}
	}
}

function _drawLane(lane, isMigrationModelElement, currentPaper)
{
	var rect = currentPaper.rect(lane.x, lane.y, lane.width, lane.height);

	rect.attr({"stroke-width": 1,
		"stroke": MAIN_STROKE_COLOR,
		"fill": "white"
 	});

	if (lane.name) {
		var laneName = currentPaper.text(lane.x + 10, lane.y + (lane.height / 2), lane.name).attr({
	        "text-anchor" : "middle",
	        "font-family" : "Arial",
	        "font-size" : "12",
	        "fill" : MAIN_STROKE_COLOR
	  	});

		laneName.transform("r270");
	}
}

function _drawSubProcess(element, isMigrationModelElement, currentPaper)
{
	var rect = currentPaper.rect(element.x, element.y, element.width, element.height, 4);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	rect.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "white"
 	});
 	
 	if (element.collapsed) {
        if (element.name) {
            this._drawMultilineText(element.name, element.x, element.y, element.width, element.height, "middle", "middle", 11,
            _bpmnGetColor(element, TEXT_COLOR), currentPaper);
        }

        rect.click(function() {
            _expandCollapsedElement(element);
        });
    }
}

function _drawEventSubProcess(element, isMigrationModelElement, currentPaper)
{
	var rect = currentPaper.rect(element.x, element.y, element.width, element.height, 4);
	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	rect.attr({"stroke-width": 2,
		"stroke": strokeColor,
		"stroke-dasharray": ".",
		"fill": "white"
 	});
}

function _drawStartEvent(element, isMigrationModelElement, currentPaper)
{
	var startEvent = _drawEvent(element, NORMAL_STROKE, 15, currentPaper);
	startEvent.click(function() {
		_zoom(true);
	});
	_addHoverLogic(element, "circle", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawEndEvent(element, isMigrationModelElement, currentPaper)
{
	var endEvent = _drawEvent(element, ENDEVENT_STROKE, 14, currentPaper);
	endEvent.click(function() {
		_zoom(false);
	});
	_addHoverLogic(element, "circle", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawEvent(element, strokeWidth, radius, currentPaper)
{
	var x = element.x + (element.width / 2);
	var y = element.y + (element.height / 2);

	var circle = currentPaper.circle(x, y, radius);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);
	circle.attr({"stroke-width": strokeWidth,
		"stroke": strokeColor,
		"fill": "#ffffff"
 	});

	circle.id = element.id;

	_drawEventIcon(currentPaper, element);

	return circle;
}

function _drawServiceTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	if (element.taskType === "mail") {
		_drawSendTaskIcon(currentPaper, element.x - 4, element.y - 4, element);
		
	} else if (element.taskType === "camel") {
        _drawCamelTaskIcon(currentPaper, element.x + 4, element.y + 4);
        
    } else if (element.taskType === "mule") {
        _drawMuleTaskIcon(currentPaper, element.x + 4, element.y + 4);
        
    } else if (element.taskType === "http") {
        _drawHttpTaskIcon(currentPaper, element.x + 4, element.y + 4);
        
    } else if (element.taskType === "dmn") {
        _drawDecisionTaskIcon(currentPaper, element.x + 4, element.y + 4);
        
    } else if (element.taskType === "shell") {
        _drawShellTaskIcon(currentPaper, element.x + 4, element.y + 4);
        
    } else if (element.stencilIconId) {
		currentPaper.image("../service/stencilitem/" + element.stencilIconId + "/icon", element.x + 4, element.y + 4, 16, 16);
		
	} else {
		_drawServiceTaskIcon(currentPaper, element.x + 4, element.y + 4, element);
	}
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawHttpServiceTask(element, isMigrationModelElement, currentPaper)
{
    _drawTask(element, currentPaper);
    _drawHttpTaskIcon(currentPaper, element.x + 4, element.y + 4);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawCallActivity(element, isMigrationModelElement, currentPaper)
{
  var width = element.width - (CALL_ACTIVITY_STROKE / 2);
  var height = element.height - (CALL_ACTIVITY_STROKE / 2);

  var rect = currentPaper.rect(element.x, element.y, width, height, 4);


  var strokeColor = _bpmnGetColor(element, ACTIVITY_STROKE_COLOR);

  rect.attr({"stroke-width": CALL_ACTIVITY_STROKE,
    "stroke": strokeColor,
    "fill": ACTIVITY_FILL_COLOR
  });

  rect.id = element.id;

  if (element.name) {
    this._drawMultilineText(element.name, element.x, element.y, element.width, element.height, "middle", "middle", 11, null, currentPaper);
  }
  _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawScriptTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	_drawScriptTaskIcon(currentPaper, element.x + 4, element.y + 4, element);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawUserTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	_drawUserTaskIcon(currentPaper, element.x + 4, element.y + 4, element);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawBusinessRuleTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	_drawBusinessRuleTaskIcon(currentPaper, element.x + 4, element.y + 4, element);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawManualTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	_drawManualTaskIcon(currentPaper, element.x + 4, element.y + 4, element);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawReceiveTask(element, isMigrationModelElement, currentPaper)
{
	_drawTask(element, currentPaper);
	_drawReceiveTaskIcon(currentPaper, element.x, element.y, element);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawTask(element, currentPaper)
{
	var width = element.width - (TASK_STROKE / 2);
	var height = element.height - (TASK_STROKE / 2);

	var rect = currentPaper.rect(element.x, element.y, width, height, 4);


	var strokeColor = _bpmnGetColor(element, ACTIVITY_STROKE_COLOR);
    var strokeWidth = element.current ? CURRENT_ACTIVITY_STROKE : TASK_STROKE;
	rect.attr({"stroke-width": strokeWidth,
		"stroke": strokeColor,
		"fill": ACTIVITY_FILL_COLOR
 	});

	rect.id = element.id;

	if (element.name) {
		this._drawMultilineText(element.name, element.x, element.y, element.width, element.height, "middle", "middle", 11,
		    _bpmnGetColor(element, TEXT_COLOR), currentPaper);
	}
}

function _drawExclusiveGateway(element, isMigrationModelElement, currentPaper)
{
	_drawGateway(element, currentPaper);
	var quarterWidth = element.width / 4;
	var quarterHeight = element.height / 4;

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	var iks = currentPaper.path(
		"M" + (element.x + quarterWidth + 3) + " " + (element.y + quarterHeight + 3) +
		"L" + (element.x + 3 * quarterWidth - 3) + " " + (element.y + 3 * quarterHeight - 3) +
		"M" + (element.x + quarterWidth + 3) + " " + (element.y + 3 * quarterHeight - 3) +
		"L" + (element.x + 3 * quarterWidth - 3) + " " + (element.y + quarterHeight + 3)
	);
	iks.attr({"stroke-width": 3, "stroke": strokeColor});

	_addHoverLogic(element, "rhombus", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawParallelGateway(element, isMigrationModelElement, currentPaper)
{
	_drawGateway(element, currentPaper);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	var path1 = currentPaper.path("M 6.75,16 L 25.75,16 M 16,6.75 L 16,25.75");
	path1.attr({
		"stroke-width": 3,
		"stroke": strokeColor,
		"fill": "none"
	});

	path1.transform("T" + (element.x + 4) + "," + (element.y + 4));

	_addHoverLogic(element, "rhombus", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawInclusiveGateway(element, isMigrationModelElement, currentPaper)
{
	_drawGateway(element, currentPaper);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	var circle1 = currentPaper.circle(element.x + (element.width / 2), element.y + (element.height / 2), 9.75);
	circle1.attr({
		"stroke-width": 2.5,
		"stroke": strokeColor,
		"fill": "none"
	});

	_addHoverLogic(element, "rhombus", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawEventGateway(element, isMigrationModelElement, currentPaper)
{
	_drawGateway(element, currentPaper);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	var circle1 = currentPaper.circle(element.x + (element.width / 2), element.y + (element.height / 2), 10.4);
	circle1.attr({
		"stroke-width": 0.5,
		"stroke": strokeColor,
		"fill": "none"
	});

	var circle2 = currentPaper.circle(element.x + (element.width / 2), element.y + (element.height / 2), 11.7);
	circle2.attr({
		"stroke-width": 0.5,
		"stroke": strokeColor,
		"fill": "none"
	});

	var path1 = currentPaper.path("M 20.327514,22.344972 L 11.259248,22.344216 L 8.4577203,13.719549 L 15.794545,8.389969 L 23.130481,13.720774 L 20.327514,22.344972 z");
	path1.attr({
		"stroke-width": 1.39999998,
		"stroke": strokeColor,
		"fill": "none",
		"stroke-linejoin": "bevel"
	});

	path1.transform("T" + (element.x + 4) + "," + (element.y + 4));

	_addHoverLogic(element, "rhombus", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);
}

function _drawGateway(element, currentPaper)
{
  var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	var rhombus = currentPaper.path("M" + element.x + " " + (element.y + (element.height / 2)) +
		"L" + (element.x + (element.width / 2)) + " " + (element.y + element.height) +
		"L" + (element.x + element.width) + " " + (element.y + (element.height / 2)) +
		"L" + (element.x + (element.width / 2)) + " " + element.y + "z"
	);

	rhombus.attr("stroke-width", 2);
	rhombus.attr("stroke", strokeColor);
	rhombus.attr({fill: "#ffffff"});

	rhombus.id = element.id;

	return rhombus;
}

function _drawBoundaryEvent(element, isMigrationModelElement, currentPaper)
{
	var x = element.x + (element.width / 2);
	var y = element.y + (element.height / 2);

	var circle = currentPaper.circle(x, y, 15);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	circle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "white"
 	});

	var innerCircle = currentPaper.circle(x, y, 12);

	innerCircle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "none"
 	});

	_drawEventIcon(currentPaper, element);
	_addHoverLogic(element, "circle", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);

	circle.id = element.id;
	innerCircle.id = element.id + "_inner";
}

function _drawIntermediateCatchEvent(element, isMigrationModelElement, currentPaper)
{
	var x = element.x + (element.width / 2);
	var y = element.y + (element.height / 2);

	var circle = currentPaper.circle(x, y, 15);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	circle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "white"
 	});

	var innerCircle = currentPaper.circle(x, y, 12);

	innerCircle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "none"
 	});

	_drawEventIcon(currentPaper, element);
	_addHoverLogic(element, "circle", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);

	circle.id = element.id;
	innerCircle.id = element.id + "_inner";
}

function _drawThrowEvent(element, isMigrationModelElement, currentPaper)
{
	var x = element.x + (element.width / 2);
	var y = element.y + (element.height / 2);

	var circle = currentPaper.circle(x, y, 15);

	var strokeColor = _bpmnGetColor(element, MAIN_STROKE_COLOR);

	circle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "white"
 	});

	var innerCircle = currentPaper.circle(x, y, 12);

	innerCircle.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "none"
 	});

	_drawEventIcon(currentPaper, element);
	_addHoverLogic(element, "circle", MAIN_STROKE_COLOR, isMigrationModelElement, currentPaper);

	circle.id = element.id;
	innerCircle.id = element.id + "_inner";
}

function _drawMultilineText(text, x, y, boxWidth, boxHeight, horizontalAnchor, verticalAnchor, fontSize, color, currentPaper)
{
	if (!text || text == "")
	{
		return;
	}

	var textBoxX=0, textBoxY;
    var width = boxWidth - (2 * TEXT_PADDING);

    if (horizontalAnchor === "middle")
    {
    	textBoxX = x + (boxWidth / 2);
    }
    else if (horizontalAnchor === "start")
    {
    	textBoxX = x;
    }

    textBoxY = y + (boxHeight / 2);

    if(!color) {
     color = TEXT_COLOR;
    }
 	var t = currentPaper.text(textBoxX + TEXT_PADDING, textBoxY + TEXT_PADDING).attr({
        "text-anchor" : horizontalAnchor,
        "font-family" : "Arial",
        "font-size" : fontSize,
        "fill" : color
  	});

    var abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    t.attr({
        "text" : abc
    });
    var letterWidth = t.getBBox().width / abc.length;

    t.attr({
        "text" : text
    });
    var removedLineBreaks = text.split("\n");
    var x = 0, s = [];
    for (var r = 0; r < removedLineBreaks.length; r++)
    {
  	    var words = removedLineBreaks[r].split(" ");
  	    for ( var i = 0; i < words.length; i++) {

  	        var l = words[i].length;
  	        if (x + (l * letterWidth) > width) {
  	            s.push("\n");
  	            x = 0;
  	        }
  	        x += l * letterWidth;
  	        s.push(words[i] + " ");
  	    }
	  	s.push("\n");
        x = 0;
    }
    t.attr({
    	"text" : s.join("")
    });

    if (verticalAnchor && verticalAnchor === "top")
    {
    	t.attr({"y": y + (t.getBBox().height / 2)});
    }
}

function _drawFlow(flow, currentPaper){

	var polyline = new Polyline(flow.id, flow.waypoints, SEQUENCEFLOW_STROKE, currentPaper);

	var strokeColor = _bpmnGetColor(flow, MAIN_STROKE_COLOR);

	polyline.element = currentPaper.path(polyline.path);
	polyline.element.attr({"stroke-width":SEQUENCEFLOW_STROKE});
	polyline.element.attr({"stroke":strokeColor});

	polyline.element.id = flow.id;

	var lastLineIndex = polyline.getLinesCount() - 1;
	var line = polyline.getLine(lastLineIndex);

	if (flow.type == "connection" && flow.conditions) {
		var middleX = (line.x1 + line.x2) / 2;
		var middleY = (line.y1 + line.y2) / 2;
		var image = currentPaper.image("../editor/images/condition-flow.png", middleX - 8, middleY - 8, 16, 16);
	}

	var polylineInvisible = new Polyline(flow.id, flow.waypoints, SEQUENCEFLOW_STROKE, currentPaper);

	polylineInvisible.element = currentPaper.path(polyline.path);
	polylineInvisible.element.attr({
			"opacity": 0,
			"stroke-width": 8,
            "stroke" : "#000000"
	});

    if (flow.name) {
        var firstLine = polyline.getLine(0);

        var angle;
        if (firstLine.x1 !== firstLine.x2) {
            angle = Math.atan((firstLine.y2 - firstLine.y1) / (firstLine.x2 - firstLine.x1));
        } else if (firstLine.y1 < firstLine.y2) {
            angle = Math.PI / 2;
        } else {
            angle = -Math.PI / 2;
        }
        var flowName = currentPaper.text(firstLine.x1, firstLine.y1, flow.name).attr({
            "text-anchor": "middle",
            "font-family" : "Arial",
            "font-size" : "12",
            "fill" : "#000000"
        });

        var offsetX = (flowName.getBBox().width / 2 + 5);
        var offsetY = -(flowName.getBBox().height / 2 + 5);

        if (firstLine.x1 > firstLine.x2) {
            offsetX = -offsetX;
        }
        var rotatedOffsetX = offsetX * Math.cos(angle) - offsetY * Math.sin(angle);
        var rotatedOffsetY = offsetX * Math.sin(angle) + offsetY * Math.cos(angle);

        flowName.attr({
            x: firstLine.x1 + rotatedOffsetX,
            y: firstLine.y1 + rotatedOffsetY
        });

        flowName.transform("r" + ((angle) * 180) / Math.PI);
    }

    _showTip($(polylineInvisible.element.node), flow);

	polylineInvisible.element.mouseover(function() {
		currentPaper.getById(polyline.element.id).attr({"stroke": HOVER_COLOR});
	});

	polylineInvisible.element.mouseout(function() {
		currentPaper.getById(polyline.element.id).attr({"stroke":strokeColor});
	});

	_drawArrowHead(line, strokeColor, currentPaper);
}

function _drawArrowHead(line, color, currentPaper)
{
	var doubleArrowWidth = 2 * ARROW_WIDTH;

	var arrowHead = currentPaper.path("M0 0L-" + (ARROW_WIDTH / 2 + .5) + " -" + doubleArrowWidth + "L" + (ARROW_WIDTH/2 + .5) + " -" + doubleArrowWidth + "z");

	// anti smoothing
	if (this.strokeWidth%2 == 1)
		line.x2 += .5, line.y2 += .5;

	arrowHead.transform("t" + line.x2 + "," + line.y2 + "");
	arrowHead.transform("...r" + Raphael.deg(line.angle - Math.PI / 2) + " " + 0 + " " + 0);

	arrowHead.attr("fill", color);

	arrowHead.attr("stroke-width", SEQUENCEFLOW_STROKE);
	arrowHead.attr("stroke", color);

	return arrowHead;
}
