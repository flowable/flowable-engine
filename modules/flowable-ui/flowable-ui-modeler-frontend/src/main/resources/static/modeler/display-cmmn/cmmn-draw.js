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

function _cmmnGetColor(element, defaultColor)
{
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

function _drawPlanModel(planModel)
{
	var rect = paper.rect(planModel.x, planModel.y, planModel.width, planModel.height);

	rect.attr({"stroke-width": 1,
		"stroke": "#000000",
		"fill": "white"
 	});

	if (planModel.name)
	{
		var planModelName = paper.text(planModel.x + 14, planModel.y + (planModel.height / 2), planModel.name).attr({
	        "text-anchor" : "middle",
	        "font-family" : "Arial",
	        "font-size" : "12",
	        "fill" : "#000000"
	  	});

		planModelName.transform("r270");
	}
}

function _drawSubProcess(element)
{
	var rect = paper.rect(element.x, element.y, element.width, element.height, 4);

	var strokeColor = _cmmnGetColor(element, MAIN_STROKE_COLOR);

	rect.attr({"stroke-width": 1,
		"stroke": strokeColor,
		"fill": "white"
 	});
}

function _drawServiceTaskTypeIcon(element)
{
	_drawTask(element);
	if (element.taskType === "mail")
	{
		_drawSendTaskIcon(paper, element.x + 4, element.y + 4);
	}
	else if (element.taskType === "camel")
	{
		_drawCamelTaskIcon(paper, element.x + 4, element.y + 4);
	}
	else if (element.taskType === "mule")
	{
		_drawMuleTaskIcon(paper, element.x + 4, element.y + 4);
	}
    else if (element.taskType === "http")
    {
        _drawHttpTaskIcon(paper, element.x + 4, element.y + 4);
    }
	else if (element.stencilIconId)
	{
		paper.image("../service/stencilitem/" + element.stencilIconId + "/icon", element.x + 4, element.y + 4, 16, 16);
	}
	else
	{
		_drawServiceTaskIcon(paper, element.x + 4, element.y + 4);
	}
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawHttpServiceTask(element)
{
    _drawTask(element);
    _drawHttpTaskIcon(paper, element.x + 4, element.y + 4);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawHumanTask(element)
{
	_drawTask(element);
	_drawHumanTaskIcon(paper, element.x + 4, element.y + 4);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawCaseTask(element)
{
    _drawTask(element);
    _drawCaseTaskIcon(paper, element.x + 1, element.y + 1);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawProcessTask(element)
{
    _drawTask(element);
    _drawProcessTaskIcon(paper, element.x + 1, element.y + 1);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawScriptTaskIcon(paper, startX, startY)
{
	var path1 = paper.path("m 5,2 0,0.094 c 0.23706,0.064 0.53189,0.1645 0.8125,0.375 0.5582,0.4186 1.05109,1.228 1.15625,2.5312 l 8.03125,0 1,0 1,0 c 0,-3 -2,-3 -2,-3 l -10,0 z M 4,3 4,13 2,13 c 0,3 2,3 2,3 l 9,0 c 0,0 2,0 2,-3 L 15,6 6,6 6,5.5 C 6,4.1111 5.5595,3.529 5.1875,3.25 4.8155,2.971 4.5,3 4.5,3 L 4,3 z");
	path1.attr({
		"opacity": 1,
		"stroke": "none",
		"fill": "#72a7d0"
 	});

	var scriptTaskIcon = paper.set();
	scriptTaskIcon.push(path1);

	scriptTaskIcon.transform("T" + startX + "," + startY);
}

function _drawScriptServiceTask(element)
{
	_drawTask(element);
	_drawScriptTaskIcon(paper, element.x + 4, element.y + 4);
	_addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawSendEventServiceTask(element)
{
    _drawTask(element);
    _drawSendTaskIcon(paper, element.x + 4, element.y + 4);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawExternalWorkerServiceTask(element)
{
    _drawTask(element);
    _drawServiceTaskIcon(paper, element.x + 4, element.y + 4);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawDecisionTask(element)
{
    _drawTask(element);
    _drawDecisionTaskIcon(paper, element.x + 1, element.y + 1);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawServiceTask(element)
{
    _drawTask(element);
    _drawServiceTaskTypeIcon(element);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawTask(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _cmmnGetColor(element, ACTIVITY_STROKE_COLOR);
    rectAttrs['stroke'] = strokeColor;

    var strokeWidth;
    if (strokeColor === ACTIVITY_STROKE_COLOR) {
        strokeWidth = TASK_STROKE;
    } else {
        strokeWidth = TASK_HIGHLIGHT_STROKE;
    }

	var width = element.width - (strokeWidth / 2);
	var height = element.height - (strokeWidth / 2);

	var rect = paper.rect(element.x, element.y, width, height, 4);
    rectAttrs['stroke-width'] = strokeWidth;

    // Fill
	rectAttrs['fill'] = ACTIVITY_FILL_COLOR;

    rect.attr(rectAttrs);
	rect.id = element.id;

	if (element.name) {
		this._drawMultilineText(element.name, element.x, element.y, element.width, element.height, "middle", "middle", 11);
	}
}

function _drawTimerEventListener(element)
{
    _drawEventListener(element);
    _drawTimerEventListenerIcon(paper, element);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawUserEventListener(element)
{
    _drawEventListener(element);
    _drawUserEventListenerIcon(paper, element);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawVariableEventListener(element)
{
    _drawEventListener(element);
    _drawVariableEventListenerIcon(paper, element);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawGenericEventListener(element)
{
    _drawEventListener(element);
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawEventListener(element)
{
    var x = element.x + (element.width / 2);
	var y = element.y + (element.height / 2);

	var circle = paper.circle(x, y, 15);

    circle.attr({"stroke-width": 1,
        "stroke": "black",
        "fill": "white"
    });

	circle.id = element.id;
}

function _drawMilestone(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _cmmnGetColor(element, ACTIVITY_STROKE_COLOR);
    rectAttrs['stroke'] = strokeColor;

    var strokeWidth;
    if (strokeColor === ACTIVITY_STROKE_COLOR) {
        strokeWidth = TASK_STROKE;
    } else {
        strokeWidth = TASK_HIGHLIGHT_STROKE;
    }

    var width = element.width - (strokeWidth / 2);
    var height = element.height - (strokeWidth / 2);

    var rect = paper.rect(element.x, element.y, width, height, 24);
    rectAttrs['stroke-width'] = strokeWidth;

    // Fill
    rectAttrs['fill'] = WHITE_FILL_COLOR;

    rect.attr(rectAttrs);
    rect.id = element.id;

    if (element.name) {
        this._drawMultilineText(element.name, element.x, element.y, element.width, element.height, "middle", "middle", 11);
    }
}

function _drawStage(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _cmmnGetColor(element, ACTIVITY_STROKE_COLOR);
    rectAttrs['stroke'] = strokeColor;

    var strokeWidth;
    if (strokeColor === ACTIVITY_STROKE_COLOR) {
        strokeWidth = TASK_STROKE;
    } else {
        strokeWidth = TASK_HIGHLIGHT_STROKE;
    }

    var width = element.width - (strokeWidth / 2);
    var height = element.height - (strokeWidth / 2);

    var rect = paper.rect(element.x, element.y, width, height, 16);
    rectAttrs['stroke-width'] = strokeWidth;

    // Fill
    rectAttrs['fill'] = WHITE_FILL_COLOR;

    rect.attr(rectAttrs);
    rect.id = element.id;

    if (element.name) {
        this._drawMultilineText(element.name, element.x + 10, element.y + 5, element.width, element.height, "start", "top", 11);
    }
}

function _drawPlanModel(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _cmmnGetColor(element, ACTIVITY_STROKE_COLOR);
    rectAttrs['stroke'] = strokeColor;

    var strokeWidth;
    if (strokeColor === ACTIVITY_STROKE_COLOR) {
        strokeWidth = TASK_STROKE;
    } else {
        strokeWidth = TASK_HIGHLIGHT_STROKE;
    }

    var width = element.width - (strokeWidth / 2);
    var height = element.height - (strokeWidth / 2);

    var rect = paper.rect(element.x, element.y, width, height, 4);
    rectAttrs['stroke-width'] = strokeWidth;

    // Fill
    rectAttrs['fill'] = WHITE_FILL_COLOR;

    rect.attr(rectAttrs);
    rect.id = element.id;

    var path1 = paper.path("M20 55 L37 34 L275 34 L291 55");
    path1.attr({
        "opacity": 1,
        "stroke": strokeColor,
        "fill": "#ffffff"
    });

    var planModelHeader = paper.set();
    planModelHeader.push(path1);

    planModelHeader.translate(element.x, element.y - 55);
    if (element.name) {
        this._drawMultilineText(element.name, element.x + 10, element.y - 16, 275, element.height, "middle", "top", 11);
    }
}

function _drawEntryCriterion(element)
{
    var strokeColor = _cmmnGetColor(element, MAIN_STROKE_COLOR);

    var rhombus = paper.path("M" + element.x + " " + (element.y + (element.height / 2)) +
        "L" + (element.x + (element.width / 2)) + " " + (element.y + element.height) +
        "L" + (element.x + element.width) + " " + (element.y + (element.height / 2)) +
        "L" + (element.x + (element.width / 2)) + " " + element.y + "z"
    );

    // Fill
    var gatewayFillColor = WHITE_FILL_COLOR;

    // Opacity
    var gatewayOpacity = 1.0;

    rhombus.attr("stroke-width", 1);
    rhombus.attr("stroke", strokeColor);
    rhombus.attr("fill", gatewayFillColor);
    rhombus.attr("fill-opacity", gatewayOpacity);

    rhombus.id = element.id;
}

function _drawExitCriterion(element)
{
    var strokeColor = _cmmnGetColor(element, MAIN_STROKE_COLOR);

    var rhombus = paper.path("M" + element.x + " " + (element.y + (element.height / 2)) +
        "L" + (element.x + (element.width / 2)) + " " + (element.y + element.height) +
        "L" + (element.x + element.width) + " " + (element.y + (element.height / 2)) +
        "L" + (element.x + (element.width / 2)) + " " + element.y + "z"
    );

    // Fill
    var gatewayFillColor = '#000000';

    // Opacity
    var gatewayOpacity = 1.0;

    rhombus.attr("stroke-width", 1);
    rhombus.attr("stroke", strokeColor);
    rhombus.attr("fill", gatewayFillColor);
    rhombus.attr("fill-opacity", gatewayOpacity);

    rhombus.id = element.id;
}

function _drawMultilineText(text, x, y, boxWidth, boxHeight, horizontalAnchor, verticalAnchor, fontSize)
{
	if (!text || text == "")
	{
		return;
	}

	var textBoxX, textBoxY;
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

 	var t = paper.text(textBoxX + TEXT_PADDING, textBoxY + TEXT_PADDING).attr({
        "text-anchor" : horizontalAnchor,
        "font-family" : "Arial",
        "font-size" : fontSize,
        "fill" : "#373e48"
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

function _drawAssociation(flow){

	var polyline = new Polyline(flow.id, flow.waypoints, ASSOCIATION_STROKE, paper);
	polyline.element = paper.path(polyline.path);
	polyline.element.attr({"stroke-width": ASSOCIATION_STROKE});
	polyline.element.attr({"stroke-dasharray": ". "});
	polyline.element.attr({"stroke":"#585858"});

	polyline.element.id = flow.id;

	var polylineInvisible = new Polyline(flow.id, flow.waypoints, ASSOCIATION_STROKE, paper);

	polylineInvisible.element = paper.path(polyline.path);
	polylineInvisible.element.attr({
			"opacity": 0,
			"stroke-width": 8,
            "stroke" : "#000000"
	});

	_showTip(jQuery(polylineInvisible.element.node), flow);

	polylineInvisible.element.mouseover(function() {
		paper.getById(polyline.element.id).attr({"stroke":"blue"});
	});

	polylineInvisible.element.mouseout(function() {
		paper.getById(polyline.element.id).attr({"stroke":"#585858"});
	});
}

function _drawArrowHead(line, connectionType)
{
	var doubleArrowWidth = 2 * ARROW_WIDTH;

	var arrowHead = paper.path("M0 0L-" + (ARROW_WIDTH / 2 + .5) + " -" + doubleArrowWidth + "L" + (ARROW_WIDTH/2 + .5) + " -" + doubleArrowWidth + "z");

	// anti smoothing
	if (this.strokeWidth%2 == 1)
		line.x2 += .5, line.y2 += .5;

	arrowHead.transform("t" + line.x2 + "," + line.y2 + "");
	arrowHead.transform("...r" + Raphael.deg(line.angle - Math.PI / 2) + " " + 0 + " " + 0);

	arrowHead.attr("fill", "#585858");

	arrowHead.attr("stroke-width", SEQUENCEFLOW_STROKE);
	arrowHead.attr("stroke", "#585858");

	return arrowHead;
}
