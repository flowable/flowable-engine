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
package org.flowable.cmmn.editor.json.converter.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.constants.EditorJsonConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.ExtensionAttribute;
import org.flowable.cmmn.model.ExtensionElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CmmnModelJsonConverterUtil implements EditorJsonConstants, CmmnStencilConstants {
    
    public static void addEventOutParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode, ObjectMapper objectMapper) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, element.getAttributeValue(null, "source"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, element.getAttributeValue(null, "sourceType"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, element.getAttributeValue(null, "target"));

            arrayNode.add(itemNode);
        }

        valueNode.set("outParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, valueNode);
    }
    
    public static void addEventInParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode, ObjectMapper objectMapper) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, element.getAttributeValue(null, "source"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, element.getAttributeValue(null, "target"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, element.getAttributeValue(null, "targetType"));

            arrayNode.add(itemNode);
        }

        valueNode.set("inParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, valueNode);
    }
    
    public static void addEventCorrelationParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode, ObjectMapper objectMapper) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME, element.getAttributeValue(null, "name"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONTYPE, element.getAttributeValue(null, "nameType"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONVALUE, element.getAttributeValue(null, "value"));

            arrayNode.add(itemNode);
        }

        valueNode.set("correlationParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_CORRELATION_PARAMETERS, valueNode);
    }
    
    public static void convertJsonToOutParameters(JsonNode objectNode, BaseElement baseElement) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, objectNode);
        parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("outParameters") != null) {
            parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("outParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement("eventOutParameter", baseElement);
                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();
                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();
                    
                    addExtensionAttribute("source", eventName, extensionElement);
                    addExtensionAttribute("sourceType", eventType, extensionElement);
                    addExtensionAttribute("target", variableName, extensionElement);
                }
            }
        }
    }
    
    public static void convertJsonToInParameters(JsonNode objectNode, BaseElement baseElement) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, objectNode);
        parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("inParameters") != null) {
            parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("inParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement("eventInParameter", baseElement);
                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();
                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();
                    
                    addExtensionAttribute("source", variableName, extensionElement);
                    addExtensionAttribute("target", eventName, extensionElement);
                    addExtensionAttribute("targetType", eventType, extensionElement);
                }
            }
        }
    }
    
    public static void convertJsonToCorrelationParameters(JsonNode objectNode, String correlationPropertyName, BaseElement baseElement) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_CORRELATION_PARAMETERS, objectNode);
        parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("correlationParameters") != null) {
            parametersNode = CmmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("correlationParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement(correlationPropertyName, baseElement);
                    String name = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME).asText();
                    String type = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONTYPE).asText();
                    String value = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONVALUE).asText();
                    
                    addExtensionAttribute("name", name, extensionElement);
                    addExtensionAttribute("type", type, extensionElement);
                    addExtensionAttribute("value", value, extensionElement);
                }
            }
        }
    }

    public static String getPropertyValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = getProperty(name, objectNode);
        if (propertyNode != null && !"null".equalsIgnoreCase(propertyNode.asText())) {
            propertyValue = propertyNode.asText();
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

    public static JsonNode getProperty(String name, JsonNode objectNode) {
        JsonNode propertyNode = null;
        if (objectNode.get(EDITOR_SHAPE_PROPERTIES) != null) {
            JsonNode propertiesNode = objectNode.get(EDITOR_SHAPE_PROPERTIES);
            propertyNode = propertiesNode.get(name);
        }
        return propertyNode;
    }

    public static String getPropertyFormKey(JsonNode elementNode, Map<String, String> formMap) {
        String formKey = getPropertyValueAsString(PROPERTY_FORMKEY, elementNode);
        if (StringUtils.isNotEmpty(formKey)) {
            return (formKey);
        } else {
            JsonNode formReferenceNode = CmmnModelJsonConverterUtil.getProperty(PROPERTY_FORM_REFERENCE, elementNode);
            if (formReferenceNode != null && formReferenceNode.get("id") != null) {

                if (formMap != null && formMap.containsKey(formReferenceNode.get("id").asText())) {
                    return formMap.get(formReferenceNode.get("id").asText());
                }
            }
        }
        return null;
    }
    
    public static ExtensionElement addFlowableExtensionElement(String name, BaseElement baseElement) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setName(name);
        extensionElement.setNamespace("http://flowable.org/cmmn");
        extensionElement.setNamespacePrefix("flowable");
        baseElement.addExtensionElement(extensionElement);
        return extensionElement;
    }
    
    public static void addExtensionAttribute(String name, String value, ExtensionElement extensionElement) {
        ExtensionAttribute attribute = new ExtensionAttribute(name);
        attribute.setValue(value);
        extensionElement.addAttribute(attribute);
    }

    /**
     * Usable for BPMN 2.0 editor json: traverses all child shapes (also nested), goes into the properties and sees if there is a matching property in the 'properties' of the childshape and returns
     * those in a list.
     *
     * Returns a map with said json nodes, with the key the name of the childshape.
     */
    protected static List<JsonLookupResult> getCmmnModelChildShapesPropertyValues(JsonNode editorJsonNode, String propertyName, List<String> allowedStencilTypes) {
        List<JsonLookupResult> result = new ArrayList<>();
        internalGetCmmnChildShapePropertyValues(editorJsonNode, propertyName, allowedStencilTypes, result);
        return result;
    }

    protected static void internalGetCmmnChildShapePropertyValues(JsonNode editorJsonNode, String propertyName,
            List<String> allowedStencilTypes, List<JsonLookupResult> result) {

        JsonNode childShapesNode = editorJsonNode.get("childShapes");
        if (childShapesNode != null && childShapesNode.isArray()) {
            ArrayNode childShapesArrayNode = (ArrayNode) childShapesNode;
            Iterator<JsonNode> childShapeNodeIterator = childShapesArrayNode.iterator();
            while (childShapeNodeIterator.hasNext()) {
                JsonNode childShapeNode = childShapeNodeIterator.next();

                String childShapeNodeStencilId = CmmnJsonConverterUtil.getStencilId(childShapeNode);
                boolean readPropertiesNode = allowedStencilTypes.contains(childShapeNodeStencilId);

                if (readPropertiesNode) {
                    // Properties
                    JsonNode properties = childShapeNode.get("properties");
                    if (properties != null && properties.has(propertyName)) {
                        JsonNode nameNode = properties.get("name");
                        JsonNode propertyNode = properties.get(propertyName);
                        result.add(new JsonLookupResult(CmmnJsonConverterUtil.getElementId(childShapeNode),
                                nameNode != null ? nameNode.asText() : null, propertyNode));
                    }
                }

                // Potential nested child shapes
                if (childShapeNode.has("childShapes")) {
                    internalGetCmmnChildShapePropertyValues(childShapeNode, propertyName, allowedStencilTypes, result);
                }

            }
        }
    }

    public static List<JsonLookupResult> getCmmnModelFormReferences(JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_HUMAN);
        allowedStencilTypes.add(STENCIL_PLANMODEL);
        return getCmmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_FORM_REFERENCE, allowedStencilTypes);
    }

    public static List<JsonLookupResult> getCmmnModelDecisionTableReferences(JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_DECISION);
        return getCmmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_DECISIONTABLE_REFERENCE, allowedStencilTypes);
    }

    public static List<JsonLookupResult> getCmmnModelCaseReferences(JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_CASE);
        return getCmmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_CASE_REFERENCE, allowedStencilTypes);
    }

    public static List<JsonLookupResult> getCmmnModelProcessReferences(JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_PROCESS);
        return getCmmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_PROCESS_REFERENCE, allowedStencilTypes);
    }

    public static List<JsonLookupResult> getCmmnModelDecisionReferences(JsonNode editorJsonNode) {
        List<String> allowedStencilTypes = new ArrayList<>();
        allowedStencilTypes.add(STENCIL_TASK_DECISION);
        return getCmmnModelChildShapesPropertyValues(editorJsonNode, PROPERTY_DECISIONTABLE_REFERENCE, allowedStencilTypes);
    }

    // GENERIC

    public static List<JsonNode> filterOutJsonNodes(List<JsonLookupResult> lookupResults) {
        List<JsonNode> jsonNodes = new ArrayList<>(lookupResults.size());
        for (JsonLookupResult lookupResult : lookupResults) {
            jsonNodes.add(lookupResult.getJsonNode());
        }
        return jsonNodes;
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
