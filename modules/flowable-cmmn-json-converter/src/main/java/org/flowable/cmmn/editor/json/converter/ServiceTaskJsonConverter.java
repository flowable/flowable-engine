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
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.model.CmmnModelInfo;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class ServiceTaskJsonConverter extends BaseCmmnJsonConverter implements DecisionTableKeyAwareConverter {
    
    protected Map<String, CmmnModelInfo> decisionTableKeyMap;

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SERVICE, ServiceTaskJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(CaseTask.class, ServiceTaskJsonConverter.class);
    }
    
    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_TASK_SERVICE;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
                    BaseElement baseElement, CmmnModel cmmnModel) {
        
        ServiceTask serviceTask = (ServiceTask) baseElement;

        if ("dmn".equalsIgnoreCase(serviceTask.getType())) {
            for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
                if (PROPERTY_DECISIONTABLE_REFERENCE_KEY.equals(fieldExtension.getFieldName()) &&
                                decisionTableKeyMap != null && decisionTableKeyMap.containsKey(fieldExtension.getStringValue())) {

                    ObjectNode decisionReferenceNode = objectMapper.createObjectNode();
                    propertiesNode.set(PROPERTY_DECISIONTABLE_REFERENCE, decisionReferenceNode);

                    CmmnModelInfo modelInfo = decisionTableKeyMap.get(fieldExtension.getStringValue());
                    decisionReferenceNode.put("id", modelInfo.getId());
                    decisionReferenceNode.put("name", modelInfo.getName());
                    decisionReferenceNode.put("key", modelInfo.getKey());
                }
            }

        } else if ("http".equalsIgnoreCase(serviceTask.getType())) {
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_METHOD, "requestMethod", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_URL, "requestUrl", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_HEADERS, "requestHeaders", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_BODY, "requestBody", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_TIMEOUT, "requestTimeout", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS, "disallowRedirects", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES, "failStatusCodes", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES, "handleStatusCodes", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION, "ignoreException", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES, "saveRequestVariables", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS, "saveResponseParameters", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX, "resultVariablePrefix", serviceTask, propertiesNode);

        } else {

            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
                propertiesNode.put(PROPERTY_SERVICETASK_CLASS, serviceTask.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(serviceTask.getImplementationType())) {
                propertiesNode.put(PROPERTY_SERVICETASK_EXPRESSION, serviceTask.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
                propertiesNode.put(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, serviceTask.getImplementation());
            }

            if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
                propertiesNode.put(PROPERTY_SERVICETASK_RESULT_VARIABLE, serviceTask.getResultVariableName());
            }

            addFieldExtensions(serviceTask.getFieldExtensions(), propertiesNode);
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {
        
        ServiceTask task = new ServiceTask();
        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode));

        } else if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode));

        } else if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            task.setImplementation(getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode));
        }

        if (StringUtils.isNotEmpty(getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode))) {
            task.setResultVariableName(getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode));
        }

        JsonNode fieldsNode = getProperty(PROPERTY_SERVICETASK_FIELDS, elementNode);
        if (fieldsNode != null) {
            JsonNode itemsArrayNode = fieldsNode.get("fields");
            if (itemsArrayNode != null) {
                for (JsonNode itemNode : itemsArrayNode) {
                    JsonNode nameNode = itemNode.get(PROPERTY_SERVICETASK_FIELD_NAME);
                    if (nameNode != null && StringUtils.isNotEmpty(nameNode.asText())) {

                        FieldExtension field = new FieldExtension();
                        field.setFieldName(nameNode.asText());
                        if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING_VALUE, itemNode))) {
                            field.setStringValue(getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING_VALUE, itemNode));
                        } else if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING, itemNode))) {
                            field.setStringValue(getValueAsString(PROPERTY_SERVICETASK_FIELD_STRING, itemNode));
                        } else if (StringUtils.isNotEmpty(getValueAsString(PROPERTY_SERVICETASK_FIELD_EXPRESSION, itemNode))) {
                            field.setExpression(getValueAsString(PROPERTY_SERVICETASK_FIELD_EXPRESSION, itemNode));
                        }
                        task.getFieldExtensions().add(field);
                    }
                }
            }
        }

        return task;
    }

    protected void setPropertyFieldValue(String name, ServiceTask task, ObjectNode propertiesNode) {
        for (FieldExtension extension : task.getFieldExtensions()) {
            if (name.substring(8).equalsIgnoreCase(extension.getFieldName())) {
                if (StringUtils.isNotEmpty(extension.getStringValue())) {
                    setPropertyValue(name, extension.getStringValue(), propertiesNode);
                } else if (StringUtils.isNotEmpty(extension.getExpression())) {
                    setPropertyValue(name, extension.getExpression(), propertiesNode);
                }
            }
        }
    }

    protected void setPropertyFieldValue(String propertyName, String fieldName, ServiceTask task, ObjectNode propertiesNode) {
        for (FieldExtension extension : task.getFieldExtensions()) {
            if (fieldName.equalsIgnoreCase(extension.getFieldName())) {
                if (StringUtils.isNotEmpty(extension.getStringValue())) {
                    setPropertyValue(propertyName, extension.getStringValue(), propertiesNode);
                } else if (StringUtils.isNotEmpty(extension.getExpression())) {
                    setPropertyValue(propertyName, extension.getExpression(), propertiesNode);
                }
            }
        }
    }

    @Override
    public void setDecisionTableKeyMap(Map<String, CmmnModelInfo> decisionTableKeyMap) {
        this.decisionTableKeyMap = decisionTableKeyMap;
    }
}
