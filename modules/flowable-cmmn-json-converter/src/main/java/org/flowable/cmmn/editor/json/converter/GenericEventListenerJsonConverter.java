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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.util.CmmnModelJsonConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class GenericEventListenerJsonConverter extends AbstractEventListenerJsonConverter {


    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
                                 Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_GENERIC_EVENT_LISTENER, GenericEventListenerJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(GenericEventListener.class, GenericEventListenerJsonConverter.class);
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {

        convertCommonElementToJson(elementNode, propertiesNode, baseElement);

        GenericEventListener genericEventListener = (GenericEventListener) ((PlanItem) baseElement).getPlanItemDefinition();
        if (StringUtils.isNotEmpty(genericEventListener.getEventType())) {
            propertiesNode.put(PROPERTY_EVENT_REGISTRY_EVENT_KEY, genericEventListener.getEventType());
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_NAME, getExtensionValue("eventName", genericEventListener), propertiesNode);
            CmmnModelJsonConverterUtil.addEventOutParameters(genericEventListener.getExtensionElements().get("eventOutParameter"), 
                            propertiesNode, objectMapper);
            CmmnModelJsonConverterUtil.addEventCorrelationParameters(genericEventListener.getExtensionElements().get("eventCorrelationParameter"), 
                            propertiesNode, objectMapper);
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, getExtensionValue("channelKey", genericEventListener), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, getExtensionValue("channelName", genericEventListener), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, getExtensionValue("channelType", genericEventListener), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, getExtensionValue("channelDestination", genericEventListener), propertiesNode);
            
            String keyDetectionType = getExtensionValue("keyDetectionType", genericEventListener);
            String keyDetectionValue = getExtensionValue("keyDetectionValue", genericEventListener);
            if (StringUtils.isNotEmpty(keyDetectionType) && StringUtils.isNotEmpty(keyDetectionValue)) {
                if ("fixedValue".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, keyDetectionValue, propertiesNode);
                    
                } else if ("jsonField".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, keyDetectionValue, propertiesNode);
                    
                } else if ("jsonPointer".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, keyDetectionValue, propertiesNode);
                }
            }
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
            BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnJsonConverter.CmmnModelIdHelper cmmnModelIdHelper) {
        
        GenericEventListener genericEventListener = new GenericEventListener();
        convertCommonJsonToElement(elementNode, genericEventListener);

        String eventType = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_KEY, elementNode);
        if (StringUtils.isNotEmpty(eventType)) {
            genericEventListener.setEventType(eventType);
            
            addFlowableExtensionElementWithValue("eventName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_NAME, elementNode), genericEventListener);
            CmmnModelJsonConverterUtil.convertJsonToOutParameters(elementNode, genericEventListener);
            CmmnModelJsonConverterUtil.convertJsonToCorrelationParameters(elementNode, "eventCorrelationParameter", genericEventListener);
            
            addFlowableExtensionElementWithValue("channelKey", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, elementNode), genericEventListener);
            addFlowableExtensionElementWithValue("channelName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, elementNode), genericEventListener);
            addFlowableExtensionElementWithValue("channelType", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, elementNode), genericEventListener);
            addFlowableExtensionElementWithValue("channelDestination", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, elementNode), genericEventListener);
            
            String fixedValue = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, elementNode);
            String jsonField = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, elementNode);
            String jsonPointer = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, elementNode);
            if (StringUtils.isNotEmpty(fixedValue)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "fixedValue", genericEventListener);
                addFlowableExtensionElementWithValue("keyDetectionValue", fixedValue, genericEventListener);
                
            } else if (StringUtils.isNotEmpty(jsonField)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "jsonField", genericEventListener);
                addFlowableExtensionElementWithValue("keyDetectionValue", jsonField, genericEventListener);
                
            } else if (StringUtils.isNotEmpty(jsonPointer)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "jsonPointer", genericEventListener);
                addFlowableExtensionElementWithValue("keyDetectionValue", jsonPointer, genericEventListener);
            }
        }

        return genericEventListener;
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_GENERIC_EVENT_LISTENER;
    }
}
