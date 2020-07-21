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
package org.flowable.cmmn.editor.json.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.constants.EditorJsonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class CmmnJsonConverterUtil implements EditorJsonConstants, CmmnStencilConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnJsonConverterUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode createChildShape(String id, String type, double lowerRightX, double lowerRightY, double upperLeftX, double upperLeftY) {
        ObjectNode shapeNode = objectMapper.createObjectNode();
        shapeNode.set(EDITOR_BOUNDS, createBoundsNode(lowerRightX, lowerRightY, upperLeftX, upperLeftY));
        shapeNode.put(EDITOR_SHAPE_ID, id);
        ArrayNode shapesArrayNode = objectMapper.createArrayNode();
        shapeNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        ObjectNode stencilNode = objectMapper.createObjectNode();
        stencilNode.put(EDITOR_STENCIL_ID, type);
        shapeNode.set(EDITOR_STENCIL, stencilNode);
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

    public static ObjectNode createResourceNode(String id) {
        ObjectNode resourceNode = objectMapper.createObjectNode();
        resourceNode.put(EDITOR_SHAPE_ID, id);
        return resourceNode;
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

    public static String getShapeId(JsonNode objectNode) {
        return objectNode.get(EDITOR_SHAPE_ID).asText();
    }

    public static String lookForSourceRef(String flowId, JsonNode childShapesNode) {
        String sourceRef = null;

        if (childShapesNode != null) {

            for (JsonNode childNode : childShapesNode) {
                JsonNode outgoingNode = childNode.get("outgoing");
                if (outgoingNode != null && outgoingNode.size() > 0) {
                    for (JsonNode outgoingChildNode : outgoingNode) {
                        JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                        if (resourceNode != null && flowId.equals(resourceNode.asText())) {
                            sourceRef = CmmnJsonConverterUtil.getElementId(childNode);
                            break;
                        }
                    }

                    if (sourceRef != null) {
                        break;
                    }
                }
                sourceRef = lookForSourceRef(flowId, childNode.get(EDITOR_CHILD_SHAPES));

                if (sourceRef != null) {
                    break;
                }
            }
        }
        return sourceRef;
    }

    public static JsonNode validateIfNodeIsTextual(JsonNode node) {
        if (node != null && !node.isNull() && node.isTextual() && StringUtils.isNotEmpty(node.asText())) {
            try {
                node = validateIfNodeIsTextual(objectMapper.readTree(node.asText()));
            } catch (Exception e) {
                LOGGER.error("Error converting textual node", e);
            }
        }
        return node;
    }

    public static String getValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = objectNode.get(name);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static String getPropertyValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !propertyNode.isNull() && !"null".equalsIgnoreCase(propertyNode.asText())) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    public static Integer getPropertyValueAsInteger(String name, JsonNode jsonNode) {
        Integer propertyValue = null;
        JsonNode propertyNode = getProperty(name, jsonNode);
        if (propertyNode != null && !propertyNode.isNull()
                && !"null".equalsIgnoreCase(propertyNode.asText())
                && StringUtils.isNotEmpty(propertyNode.asText())) {
            propertyValue = propertyNode.asInt();
        }
        return propertyValue;
    }
    
    public static boolean getPropertyValueAsBoolean(String name, JsonNode objectNode) {
        return getPropertyValueAsBoolean(name, objectNode, false);
    }

    public static boolean getPropertyValueAsBoolean(String name, JsonNode objectNode, boolean defaultValue) {
        boolean result = defaultValue;
        String stringValue = getPropertyValueAsString(name, objectNode);

        if (PROPERTY_VALUE_YES.equalsIgnoreCase(stringValue) || "true".equalsIgnoreCase(stringValue)) {
            result = true;
        } else if (PROPERTY_VALUE_NO.equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
            result = false;
        }

        return result;
    }
    
    public static List<String> getPropertyValueAsList(String name, JsonNode objectNode) {
        List<String> resultList = new ArrayList<>();
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !"null".equalsIgnoreCase(propertyNode.asText())) {
            String propertyValue = propertyNode.asText();
            String[] valueList = propertyValue.split(",");
            for (String value : valueList) {
                resultList.add(value.trim());
            }
        }
        return resultList;
    }
    
    public static String getPropertyFormKey(JsonNode elementNode, CmmnJsonConverterContext converterContext) {
        String formKey = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_FORMKEY, elementNode);
        if (StringUtils.isNotEmpty(formKey)) {
            return formKey;

        } else {
            JsonNode formReferenceNode = CmmnJsonConverterUtil.getProperty(PROPERTY_FORM_REFERENCE, elementNode);
            if (formReferenceNode != null && formReferenceNode.get("id") != null) {
                String formModelId = formReferenceNode.get("id").asText();
                formKey = converterContext.getFormModelKeyForFormModelId(formModelId);

                if (StringUtils.isNotEmpty(formKey)) {
                    return formKey;
                } else {
                    formKey = formReferenceNode.path("key").asText();
                    if (StringUtils.isNotEmpty(formKey)) {
                        return formKey;
                    }
                }
            }
        }
        return null;
    }
    
    public static JsonNode getProperty(String name, JsonNode objectNode) {
        JsonNode propertyNode = null;
        if (objectNode.get(EDITOR_SHAPE_PROPERTIES) != null) {
            JsonNode propertiesNode = objectNode.get(EDITOR_SHAPE_PROPERTIES);
            propertyNode = propertiesNode.get(name);
        }
        return propertyNode;
    }

}
