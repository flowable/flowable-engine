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

import static org.flowable.cmmn.editor.json.converter.CmmnJsonConverterUtil.getValueAsString;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverterUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItemDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class ListenerConverterUtil {

    public static void convertLifecycleListenersToJson(ObjectMapper objectMapper, ObjectNode propertiesNode, PlanItemDefinition planItemDefinition) {
        ObjectNode planItemLifecycleListeners = convertListenersToJson(objectMapper, "planItemLifecycleListeners", planItemDefinition.getLifecycleListeners());
        if (planItemLifecycleListeners != null) {
            propertiesNode.set(CmmnStencilConstants.PROPERTY_LIFECYCLE_LISTENERS, planItemLifecycleListeners);
        }
    }

    public static ObjectNode convertListenersToJson(ObjectMapper objectMapper, String jsonPropertyName, List<FlowableListener> listeners) {
        if (listeners != null) {
            ObjectNode listenersNode = objectMapper.createObjectNode();
            ArrayNode itemsNode = objectMapper.createArrayNode();
            for (FlowableListener listener : listeners) {
                ObjectNode propertyItemNode = objectMapper.createObjectNode();

                if (StringUtils.isNotEmpty(listener.getEvent())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_EVENT, listener.getEvent());
                }

                if (StringUtils.isNotEmpty(listener.getSourceState())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_SOURCE_STATE, listener.getSourceState());
                }

                if (StringUtils.isNotEmpty(listener.getTargetState())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_TARGET_STATE, listener.getTargetState());
                }

                if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_CLASS_NAME, listener.getImplementation());
                } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(listener.getImplementationType())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_EXPRESSION, listener.getImplementation());
                } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(listener.getImplementationType())) {
                    propertyItemNode.put(CmmnStencilConstants.PROPERTY_LISTENER_DELEGATE_EXPRESSION, listener.getImplementation());
                }

                if (CollectionUtils.isNotEmpty(listener.getFieldExtensions())) {
                    ArrayNode fieldsArray = objectMapper.createArrayNode();
                    for (FieldExtension fieldExtension : listener.getFieldExtensions()) {
                        ObjectNode fieldNode = objectMapper.createObjectNode();
                        fieldNode.put(CmmnStencilConstants.PROPERTY_FIELD_NAME, fieldExtension.getFieldName());
                        if (StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                            fieldNode.put(CmmnStencilConstants.PROPERTY_FIELD_STRING_VALUE, fieldExtension.getStringValue());
                        }
                        if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
                            fieldNode.put(CmmnStencilConstants.PROPERTY_FIELD_EXPRESSION, fieldExtension.getExpression());
                        }
                        fieldsArray.add(fieldNode);
                    }
                    propertyItemNode.set(CmmnStencilConstants.PROPERTY_LISTENER_FIELDS, fieldsArray);
                }

                itemsNode.add(propertyItemNode);
            }

            listenersNode.set(jsonPropertyName, itemsNode);
            return listenersNode;
        }

        return null;
    }

    public static void convertJsonToLifeCycleListeners(JsonNode elementNode, PlanItemDefinition planItemDefinition) {
        List<FlowableListener> listeners = convertJsonToListeners(elementNode, CmmnStencilConstants.PROPERTY_LIFECYCLE_LISTENERS, "planItemLifecycleListeners");
        if (listeners != null) {
            planItemDefinition.getLifecycleListeners().addAll(listeners);
        }
    }

    public static List<FlowableListener> convertJsonToListeners(JsonNode elementNode, String propertyName, String innerPropertyName) {
        JsonNode listenersNode = CmmnJsonConverterUtil.getProperty(propertyName, elementNode);

        // The json coming from the modeler seems to have it duplicated
        if (listenersNode != null) {
            listenersNode = listenersNode.get(innerPropertyName);
        }

        if (listenersNode != null && !listenersNode.isNull()) {
            List<FlowableListener> listeners = new ArrayList<>();
            for (JsonNode listenerNode : listenersNode) {

                FlowableListener listener = new FlowableListener();

                JsonNode eventNode = listenerNode.get(CmmnStencilConstants.PROPERTY_LISTENER_EVENT);
                if (eventNode != null && !eventNode.isNull()) {
                    listener.setEvent(eventNode.asText());
                }

                JsonNode sourceStateNode = listenerNode.get(CmmnStencilConstants.PROPERTY_LISTENER_SOURCE_STATE);
                if (sourceStateNode != null && !sourceStateNode.isNull()) {
                    listener.setSourceState(sourceStateNode.asText());
                }

                JsonNode targetStateNode = listenerNode.get(CmmnStencilConstants.PROPERTY_LISTENER_TARGET_STATE);
                if (targetStateNode != null && !targetStateNode.isNull()) {
                    listener.setTargetState(targetStateNode.asText());
                }

                if (StringUtils.isNotEmpty(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_CLASS_NAME, listenerNode))) {
                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
                    listener.setImplementation(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_CLASS_NAME, listenerNode));
                } else if (StringUtils.isNotEmpty(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_EXPRESSION, listenerNode))) {
                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
                    listener.setImplementation(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_EXPRESSION, listenerNode));
                } else if (StringUtils.isNotEmpty(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_DELEGATE_EXPRESSION, listenerNode))) {
                    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                    listener.setImplementation(getValueAsString(CmmnStencilConstants.PROPERTY_LISTENER_DELEGATE_EXPRESSION, listenerNode));
                }

                JsonNode fieldsNode = listenerNode.get(CmmnStencilConstants.PROPERTY_LISTENER_FIELDS);
                if (fieldsNode != null) {
                    for (JsonNode fieldNode : fieldsNode) {
                        JsonNode nameNode = fieldNode.get(CmmnStencilConstants.PROPERTY_FIELD_NAME);
                        if (nameNode != null && !nameNode.isNull() && StringUtils.isNotEmpty(nameNode.asText())) {
                            FieldExtension fieldExtension = new FieldExtension();
                            fieldExtension.setFieldName(nameNode.asText());
                            fieldExtension.setStringValue(getValueAsString(CmmnStencilConstants.PROPERTY_FIELD_STRING_VALUE, fieldNode));
                            if (StringUtils.isEmpty(fieldExtension.getStringValue())) {
                                fieldExtension.setStringValue(getValueAsString(CmmnStencilConstants.PROPERTY_FIELD_STRING, fieldNode));
                            }
                            if (StringUtils.isEmpty(fieldExtension.getStringValue())) {
                                fieldExtension.setExpression(getValueAsString(CmmnStencilConstants.PROPERTY_FIELD_EXPRESSION, fieldNode));
                            }
                            listener.getFieldExtensions().add(fieldExtension);
                        }
                    }
                }

                listeners.add(listener);
            }

            return listeners;
        }

        return null;
    }

}
