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
package org.flowable.dmn.editor.converter;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.dmn.editor.constants.DmnStencilConstants;
import org.flowable.dmn.editor.constants.EditorJsonConstants;
import org.flowable.dmn.model.GraphicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterUtil implements EditorJsonConstants, DmnStencilConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DmnJsonConverterUtil.class);
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    protected static double lineWidth = 0.05d;

    public static String getValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode jsonNode = objectNode.get(name);
        if (jsonNode != null && !jsonNode.isNull()) {
            propertyValue = jsonNode.asText();
        }
        return propertyValue;
    }

    public static boolean migrateModel(JsonNode decisionTableNode, ObjectMapper objectMapper) {

        boolean wasMigrated = false;

        // check if model is version 1
        if ((decisionTableNode.get("modelVersion") == null || decisionTableNode.get("modelVersion").isNull()) && decisionTableNode.has("name")) {
            wasMigrated = true;

            String modelName = decisionTableNode.get("name").asText();
            LOGGER.info("Decision table model with name {} found with version < v2; migrating to v3", modelName);

            ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
            decisionTableObjectNode.put("modelVersion", "3");

            // split input rule nodes into operator and expression nodes
            //
            // determine input node ids
            JsonNode inputExpressionNodes = decisionTableNode.get("inputExpressions");
            Map<String, String> inputExpressionIds = new HashMap<>();

            if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                for (JsonNode inputExpressionNode : inputExpressionNodes) {
                    if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                        String inputId = inputExpressionNode.get("id").asText();

                        String inputType = null;
                        if (inputExpressionNode.get("type") != null && !inputExpressionNode.get("type").isNull()) {
                            inputType = inputExpressionNode.get("type").asText();
                        }

                        inputExpressionIds.put(inputId, inputType);
                    }
                }
            }
            // split input rule nodes
            JsonNode ruleNodes = decisionTableNode.get("rules");
            ArrayNode newRuleNodes = objectMapper.createArrayNode();

            if (ruleNodes != null && !ruleNodes.isNull()) {
                for (JsonNode ruleNode : ruleNodes) {
                    ObjectNode newRuleNode = objectMapper.createObjectNode();

                    for (String inputExpressionId : inputExpressionIds.keySet()) {
                        if (ruleNode.has(inputExpressionId)) {
                            String operatorId = inputExpressionId + "_operator";
                            String expressionId = inputExpressionId + "_expression";
                            String operatorValue = null;
                            String expressionValue = null;

                            if (ruleNode.get(inputExpressionId) != null && !ruleNode.get(inputExpressionId).isNull()) {
                                String oldExpression = ruleNode.get(inputExpressionId).asText();

                                if (StringUtils.isNotEmpty(oldExpression)) {
                                    if (oldExpression.indexOf(' ') != -1) {
                                        operatorValue = oldExpression.substring(0, oldExpression.indexOf(' '));
                                        expressionValue = oldExpression.substring(oldExpression.indexOf(' ') + 1);
                                    } else { // no prefixed operator
                                        expressionValue = oldExpression;
                                    }

                                    // remove outer escape quotes
                                    if (expressionValue.startsWith("\"") && expressionValue.endsWith("\"")) {
                                        expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                                    }

                                    // if build in date function
                                    if (expressionValue.startsWith("fn_date(")) {
                                        expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));

                                    } else if (expressionValue.startsWith("date:toDate(")) {
                                        expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                                    }

                                    // determine type is null
                                    if (StringUtils.isEmpty(inputExpressionIds.get(inputExpressionId))) {
                                        String expressionType = determineExpressionType(expressionValue);
                                        inputExpressionIds.put(inputExpressionId, expressionType);
                                    }
                                }
                            }

                            // add new operator kv
                            if (StringUtils.isNotEmpty(operatorValue)) {
                                newRuleNode.put(operatorId, operatorValue);
                            } else { // default value
                                newRuleNode.put(operatorId, "==");
                            }

                            // add new expression kv
                            if (StringUtils.isNotEmpty(expressionValue)) {
                                newRuleNode.put(expressionId, expressionValue);
                            } else { // default value
                                newRuleNode.put(expressionId, "-");
                            }
                        }
                    }

                    Iterator<String> ruleProperty = ruleNode.fieldNames();
                    while (ruleProperty.hasNext()) {
                        String expressionId = ruleProperty.next();
                        if (!inputExpressionIds.containsKey(expressionId)) {
                            if (ruleNode.hasNonNull(expressionId)) {
                                String expressionValue = ruleNode.get(expressionId).asText();

                                // remove outer escape quotes
                                if (StringUtils.isNotEmpty(expressionValue) && expressionValue.startsWith("\"") && expressionValue
                                    .endsWith("\"")) {
                                    expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                                }

                                // if build in date function
                                if (expressionValue.startsWith("fn_date(")) {
                                    expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));

                                } else if (expressionValue.startsWith("date:toDate(")) {
                                    expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                                }

                                newRuleNode.put(expressionId, expressionValue);
                            }
                        }
                    }

                    newRuleNodes.add(newRuleNode);
                }

                // set input expression nodes types
                if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                    for (JsonNode inputExpressionNode : inputExpressionNodes) {
                        if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                            String inputId = inputExpressionNode.get("id").asText();
                            ((ObjectNode) inputExpressionNode).put("type", inputExpressionIds.get(inputId));
                        }
                    }
                }

                // replace rules node
                decisionTableObjectNode.replace("rules", newRuleNodes);
            }

            LOGGER.info("Decision table model {} migrated to v3", modelName);
        }

        return wasMigrated;
    }

    public static boolean migrateModelV3(JsonNode decisionTableNode, ObjectMapper objectMapper) {
        // migrate to v2
        boolean wasMigrated = migrateModel(decisionTableNode, objectMapper);

        // migrate to v3
        if (decisionTableNode.has("modelVersion") && "2".equals(decisionTableNode.get("modelVersion").asText()) && decisionTableNode.has("name")) {
            wasMigrated = true;

            String modelName = decisionTableNode.get("name").asText();
            LOGGER.info("Decision table model {} found with version v2; migrating to v3", modelName);

            ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
            decisionTableObjectNode.put("modelVersion", "3");

            JsonNode inputExpressionNodes = decisionTableNode.get("inputExpressions");
            Map<String, String> inputExpressionIds = new HashMap<>();

            if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                for (JsonNode inputExpressionNode : inputExpressionNodes) {
                    if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                        String inputId = inputExpressionNode.get("id").asText();

                        String inputType = null;
                        if (inputExpressionNode.get("type") != null && !inputExpressionNode.get("type").isNull()) {
                            inputType = inputExpressionNode.get("type").asText();
                        }

                        inputExpressionIds.put(inputId, inputType);
                    }
                }
            }

            // split input rule nodes
            JsonNode ruleNodes = decisionTableNode.get("rules");

            if (ruleNodes != null && !ruleNodes.isNull()) {
                for (JsonNode ruleNode : ruleNodes) {

                    for (String inputExpressionId : inputExpressionIds.keySet()) {

                        // get operator
                        String inputExpressionOperatorId = inputExpressionId + "_operator";

                        if (ruleNode.has(inputExpressionOperatorId)) {
                            if (ruleNode.get(inputExpressionOperatorId) != null && !ruleNode.get(inputExpressionOperatorId).isNull()) {
                                String oldInputExpressionOperatorValue = ruleNode.get(inputExpressionOperatorId).asText();
                                String inputType = inputExpressionIds.get(inputExpressionId);
                                try {
                                    String newInputExpressionOperatorValue = transformCollectionOperation(oldInputExpressionOperatorValue, inputType);

                                    // replace operator value
                                    ((ObjectNode) ruleNode).put(inputExpressionOperatorId, newInputExpressionOperatorValue);
                                } catch (IllegalArgumentException iae) {
                                    LOGGER.warn("Skipping model migration; could not transform collection operator for model name: {}", modelName, iae);
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Decision table model {} migrated to v3", modelName);
        }

        return wasMigrated;
    }

    public static String determineExpressionType(String expressionValue) {
        String expressionType = null;
        if (!"-".equals(expressionValue)) {
            expressionType = "string";
            if (NumberUtils.isCreatable(expressionValue)) {
                expressionType = "number";
            } else {
                try {
                    new SimpleDateFormat("yyyy-MM-dd").parse(expressionValue);
                    expressionType = "date";
                } catch (ParseException pe) {
                    if ("true".equalsIgnoreCase(expressionValue) || "false".equalsIgnoreCase(expressionValue)) {
                        expressionType = "boolean";
                    }
                }
            }
        }
        return expressionType;
    }

    public static String formatCollectionExpression(String containsOperator, String inputVariable, String expressionValue) {
        String containsPrefixAndMethod = getDMNContainsExpressionMethod(containsOperator);
        StringBuilder stringBuilder = new StringBuilder();

        if (containsPrefixAndMethod != null) {
            stringBuilder.append("${");
            stringBuilder.append(containsPrefixAndMethod);
            stringBuilder.append("(");
            stringBuilder.append(formatCollectionExpressionValue(inputVariable));
            stringBuilder.append(", ");

            String formattedExpressionValue = formatCollectionExpressionValue(expressionValue);
            stringBuilder.append(formattedExpressionValue);

            stringBuilder.append(")}");
        } else {
            stringBuilder.append(containsOperator);
            stringBuilder.append(" ");
            stringBuilder.append(formatCollectionExpressionValue(expressionValue));
        }

        return stringBuilder.toString();
    }

    public static boolean isCollectionOperator(String operator) {
        return "IN".equals(operator) || "NOT IN".equals(operator) || "ANY".equals(operator) || "NOT ANY".equals(operator) ||
            "IS IN".equals(operator) || "IS NOT IN".equals(operator) ||
            "NONE OF".equals(operator) || "NOT ALL OF".equals(operator) || "ALL OF".equals(operator);
    }

    public static boolean isDRD(JsonNode definitionNode) {
        return definitionNode.has("childShapes");
    }

    public static String getStencilId(JsonNode objectNode) {
        String stencilId = null;
        JsonNode stencilNode = objectNode.get(EDITOR_STENCIL);
        if (stencilNode != null && stencilNode.get(EDITOR_STENCIL_ID) != null) {
            stencilId = stencilNode.get(EDITOR_STENCIL_ID).asText();
        }
        return stencilId;
    }

    public static String getElementId(JsonNode objectNode) {
        String elementId = null;
        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_OVERRIDE_ID, objectNode))) {
            elementId = getPropertyValueAsString(PROPERTY_OVERRIDE_ID, objectNode).trim();
        } else {
            elementId = objectNode.get(EDITOR_SHAPE_ID).asText();
        }

        return elementId;
    }

    public static String getUniqueElementId() {
        return getUniqueElementId(null);
    }

    public static String getUniqueElementId(String prefix) {
        UUID uuid = UUID.randomUUID();
        if (StringUtils.isEmpty(prefix)) {
            return uuid.toString();
        } else {
            return String.format("%s_%s", prefix, uuid);
        }
    }

    public static String getPropertyValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static JsonNode getProperty(String name, JsonNode objectNode) {
        JsonNode propertyNode = null;
        if (objectNode.get(EDITOR_SHAPE_PROPERTIES) != null) {
            JsonNode propertiesNode = objectNode.get(EDITOR_SHAPE_PROPERTIES);
            propertyNode = propertiesNode.get(name);
        }
        return propertyNode;
    }

    public static ObjectNode createChildShape(String id, String type, double lowerRightX, double lowerRightY, double upperLeftX, double upperLeftY) {
        ObjectNode shapeNode = objectMapper.createObjectNode();
        shapeNode.set(EDITOR_BOUNDS, createBoundsNode(lowerRightX, lowerRightY, upperLeftX, upperLeftY));
        shapeNode.put(EDITOR_SHAPE_ID, id);
        ArrayNode shapesArrayNode = objectMapper.createArrayNode();
        shapeNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        ObjectNode stencilNode = objectMapper.createObjectNode();
        stencilNode.put(EDITOR_STENCIL_ID, type);
        shapeNode.set(EDITOR_STENCIL, stencilNode);
        shapeNode.putArray(EDITOR_OUTGOING);
        shapeNode.putArray(EDITOR_DOCKERS);
        return shapeNode;
    }

    public static ObjectNode createBoundsNode(double lowerRightX, double lowerRightY, double upperLeftX, double upperLeftY) {
        ObjectNode boundsNode = objectMapper.createObjectNode();
        boundsNode.set(EDITOR_BOUNDS_LOWER_RIGHT, createPositionNode(lowerRightX, lowerRightY));
        boundsNode.set(EDITOR_BOUNDS_UPPER_LEFT, createPositionNode(upperLeftX, upperLeftY));
        return boundsNode;
    }

    public static ObjectNode createPositionNode(double x, double y) {
        ObjectNode positionNode = objectMapper.createObjectNode();
        positionNode.put(EDITOR_BOUNDS_X, x);
        positionNode.put(EDITOR_BOUNDS_Y, y);
        return positionNode;
    }

    public static Area createRectangle(GraphicInfo graphicInfo) {
        Area outerRectangle = new Area(new Rectangle2D.Double(
            graphicInfo.getX(), graphicInfo.getY(),
            graphicInfo.getWidth(), graphicInfo.getHeight()
        ));
        Area innerRectangle = new Area(new Rectangle2D.Double(
            graphicInfo.getX() + lineWidth, graphicInfo.getY() + lineWidth,
            graphicInfo.getWidth() - 2*lineWidth, graphicInfo.getHeight() - 2*lineWidth
        ));
        outerRectangle.subtract(innerRectangle);
        return outerRectangle;
    }

    public static GraphicInfo createGraphicInfo(double x, double y) {
        GraphicInfo graphicInfo = new GraphicInfo();
        graphicInfo.setX(x);
        graphicInfo.setY(y);
        return graphicInfo;
    }

    public static Collection<Point2D> getIntersections(java.awt.geom.Line2D line, Area shape) {
        Area intersectionArea = new Area(getLineShape(line));
        intersectionArea.intersect(shape);
        if (!intersectionArea.isEmpty()) {
            Rectangle2D bounds2D = intersectionArea.getBounds2D();
            HashSet<Point2D> intersections = new HashSet<>(1);
            intersections.add(new java.awt.geom.Point2D.Double(bounds2D.getX(), bounds2D.getY()));
            return intersections;
        }
        return Collections.EMPTY_SET;
    }

    public static Shape getLineShape(java.awt.geom.Line2D line2D) {
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        line.moveTo(line2D.getX1(), line2D.getY1());
        line.lineTo(line2D.getX2(), line2D.getY2());
        line.lineTo(line2D.getX2() + lineWidth, line2D.getY2() + lineWidth);
        line.closePath();
        return line;
    }

    public static List<JsonLookupResult> getDmnModelChildShapesPropertyValues(JsonNode editorJsonNode, String propertyName, List<String> allowedStencilTypes) {
        List<JsonLookupResult> result = new ArrayList<>();
        internalGetDmnChildShapePropertyValues(editorJsonNode, propertyName, allowedStencilTypes, result);
        return result;
    }

    public static List<JsonNode> filterOutJsonNodes(List<JsonLookupResult> lookupResults) {
        List<JsonNode> jsonNodes = new ArrayList<>(lookupResults.size());
        for (JsonLookupResult lookupResult : lookupResults) {
            jsonNodes.add(lookupResult.getJsonNode());
        }
        return jsonNodes;
    }

    public static List<JsonLookupResult> getDmnModelDecisionTableReferences(ObjectNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_DECISION);
        return getDmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_DECISION_TABLE_REFERENCE, allowedStencilTypes);
    }

    public static void updateDecisionTableModelReferences(ObjectNode decisionServiceObjectNode, DmnJsonConverterContext converterContext) {
        List<JsonNode> decisionTableNodes = DmnJsonConverterUtil.filterOutJsonNodes(DmnJsonConverterUtil.getDmnModelDecisionTableReferences(decisionServiceObjectNode));
        decisionTableNodes.forEach(decisionNode -> {
            String decisionTableKey = decisionNode.get("key").asText();
            Map<String, String> modelInfo = converterContext.getDecisionTableModelInfoForDecisionTableModelKey(decisionTableKey);
            if (modelInfo != null) {
                ((ObjectNode) decisionNode).put("id", modelInfo.get("id"));
            }
        });

    }

    protected static void internalGetDmnChildShapePropertyValues(JsonNode editorJsonNode, String propertyName,
        List<String> allowedStencilTypes, List<JsonLookupResult> result) {

        JsonNode childShapesNode = editorJsonNode.get("childShapes");
        if (childShapesNode != null && childShapesNode.isArray()) {
            ArrayNode childShapesArrayNode = (ArrayNode) childShapesNode;
            Iterator<JsonNode> childShapeNodeIterator = childShapesArrayNode.iterator();
            while (childShapeNodeIterator.hasNext()) {
                JsonNode childShapeNode = childShapeNodeIterator.next();

                String childShapeNodeStencilId = DmnJsonConverterUtil.getStencilId(childShapeNode);
                boolean readPropertiesNode = allowedStencilTypes.contains(childShapeNodeStencilId);

                if (readPropertiesNode) {
                    // Properties
                    JsonNode properties = childShapeNode.get("properties");
                    if (properties != null && properties.has(propertyName)) {
                        JsonNode nameNode = properties.get("name");
                        JsonNode propertyNode = properties.get(propertyName);
                        result.add(new JsonLookupResult(DmnJsonConverterUtil.getElementId(childShapeNode),
                            nameNode != null ? nameNode.asText() : null, propertyNode));
                    }
                }

                // Potential nested child shapes
                if (childShapeNode.has("childShapes")) {
                    internalGetDmnChildShapePropertyValues(childShapeNode, propertyName, allowedStencilTypes, result);
                }

            }
        }
    }

    protected static String getDMNContainsExpressionMethod(String containsOperator) {
        if (StringUtils.isEmpty(containsOperator)) {
            throw new IllegalArgumentException("containsOperator must be provided");
        }

        String containsPrefixAndMethod;

        switch (containsOperator) {
            case "IS IN":
            case "ALL OF":
            case "IN":
                containsPrefixAndMethod = "collection:allOf";
                break;
            case "IS NOT IN":
            case "NONE OF":
            case "NOT IN":
                containsPrefixAndMethod = "collection:noneOf";
                break;
            case "ANY OF":
            case "ANY":
                containsPrefixAndMethod = "collection:anyOf";
                break;
            case "NOT ALL OF":
            case "NOT ANY":
                containsPrefixAndMethod = "collection:notAllOf";
                break;
            default:
                containsPrefixAndMethod = null;
        }

        return containsPrefixAndMethod;
    }

    protected static String formatCollectionExpressionValue(String expressionValue) {
        if (StringUtils.isEmpty(expressionValue)) {
            return "\"\"";
        }

        StringBuilder formattedExpressionValue = new StringBuilder();

        // if multiple values
        if (expressionValue.contains(",")) {
            formattedExpressionValue.append("'");

            List<String> formattedValues = split(expressionValue);
            formattedExpressionValue.append(StringUtils.join(formattedValues, ','));
        } else {
            String formattedValue = expressionValue;
            formattedExpressionValue.append(formattedValue);
        }

        // if multiple values
        if (expressionValue.contains(",")) {
            formattedExpressionValue.append("'");
        }

        return formattedExpressionValue.toString();
    }

    protected static List<String> split(String str) {
        String regex;
        if (str.contains("\"")) {
            // only split on comma between matching quotes
            regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        } else {
            regex = ",";
        }
        return Stream.of(str.split(regex))
            .map(elem -> elem.trim())
            .collect(Collectors.toList());
    }

    protected static String transformCollectionOperation(String operatorValue, String inputType) {
        if (StringUtils.isEmpty(operatorValue) || StringUtils.isEmpty(inputType)) {
            throw new IllegalArgumentException("operator value and input type must be present");
        }

        if ("collection".equalsIgnoreCase(inputType)) {
            switch (operatorValue) {
                case "IN":
                    return "ALL OF";
                case "NOT IN":
                    return "NONE OF";
                case "ANY":
                    return "ANY OF";
                case "NOT ANY":
                    return "NOT ALL OF";
                default:
                    return operatorValue;
            }
        } else {
            switch (operatorValue) {
                case "IN":
                    return "IS IN";
                case "NOT IN":
                    return "IS NOT IN";
                case "ANY":
                    return "IS IN";
                case "NOT ANY":
                    return "IS NOT IN";
                default:
                    return operatorValue;
            }
        }
    }

    // Helper classes

    public static class JsonLookupResult {

        private String id;
        private String name;
        private JsonNode jsonNode;

        public JsonLookupResult(String id, String name, JsonNode jsonNode) {
            this(name, jsonNode);
            this.id = id;
        }

        public JsonLookupResult(String name, JsonNode jsonNode) {
            this.name = name;
            this.jsonNode = jsonNode;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        public void setJsonNode(JsonNode jsonNode) {
            this.jsonNode = jsonNode;
        }
    }
}
