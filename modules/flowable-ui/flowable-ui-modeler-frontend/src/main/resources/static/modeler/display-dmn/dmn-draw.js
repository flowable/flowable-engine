function _dmnGetColor(element, defaultColor)
{
    var strokeColor;
    if (element.current) {
        strokeColor = CURRENT_COLOR;
    } else if (element.completed) {
        strokeColor = COMPLETED_COLOR;
    } else if (element.available) {
        strokeColor = AVAILABLE_COLOR;
    } else {
        strokeColor = defaultColor;
    }
    return strokeColor;
}

function _drawDecisionService(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _dmnGetColor(element, ACTIVITY_STROKE_COLOR);
    rectAttrs['stroke'] = strokeColor;

    var strokeWidth = DECISION_SERVICE_STROKE;
    var width = element.width - (strokeWidth / 2);
    var height = element.height - (strokeWidth / 2);

    var rect = paper.rect(element.x, element.y, width, height, 16);
    rectAttrs['stroke-width'] = strokeWidth;

    // Fill
    rectAttrs['fill'] = WHITE_FILL_COLOR;

    rect.attr(rectAttrs);
    rect.id = element.id;

    var dividerElement = element.divider;
    var divider = new Polyline("divider_" + element.id, dividerElement.waypoints, ACTIVITY_STROKE_COLOR, paper);
    divider.element = paper.path(divider.path);
    divider.element.attr({"stroke-width": ASSOCIATION_STROKE});
    divider.element.attr({"stroke":"#bbbbbb"});
    divider.element.id = "divider_" + element.id;

    if (element.name) {
        this._drawMultilineText(element.name, element.x + 10, element.y + 5, element.width, element.height, "start", "top", 11);
    }

    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawDecision(element)
{
    var rectAttrs = {};

    // Stroke
    var strokeColor = _dmnGetColor(element, ACTIVITY_STROKE_COLOR);
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
    _addHoverLogic(element, "rect", ACTIVITY_STROKE_COLOR);
}

function _drawInformationRequirement(flow){

    var polyline = new Polyline(flow.id, flow.waypoints, ASSOCIATION_STROKE, paper);

    var strokeColor = _dmnGetColor(flow, MAIN_STROKE_COLOR);

    polyline.element = paper.path(polyline.path);
    polyline.element.attr({"stroke-width": ASSOCIATION_STROKE});
    polyline.element.attr({"stroke":strokeColor});

    polyline.element.id = flow.id;

    var lastLineIndex = polyline.getLinesCount() - 1;
    var line = polyline.getLine(lastLineIndex);

    var polylineInvisible = new Polyline(flow.id, flow.waypoints, ASSOCIATION_STROKE, paper);

    polylineInvisible.element = paper.path(polyline.path);
    polylineInvisible.element.attr({
        "opacity": 0,
        "stroke-width": 8,
        "stroke" : "#000000"
    });

    _showTip(jQuery(polylineInvisible.element.node), flow);

    polylineInvisible.element.mouseover(function() {
        paper.getById(polyline.element.id).attr({"stroke":HOVER_COLOR});
    });

    polylineInvisible.element.mouseout(function() {
        paper.getById(polyline.element.id).attr({"stroke":strokeColor});
    });

    _drawArrowHead(line, strokeColor, paper);

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

    arrowHead.attr("stroke-width", ASSOCIATION_STROKE);
    arrowHead.attr("stroke", "#585858");

    return arrowHead;
}
