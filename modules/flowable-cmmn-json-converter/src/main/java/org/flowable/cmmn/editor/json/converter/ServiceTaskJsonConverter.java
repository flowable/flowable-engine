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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.model.CmmnModelInfo;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tijs Rademakers
 */
public class ServiceTaskJsonConverter extends BaseCmmnJsonConverter implements DecisionTableKeyAwareConverter {

    protected static final Map<String, String> TYPE_TO_STENCILSET = new HashMap<>();
    static {
        TYPE_TO_STENCILSET.put(HttpServiceTask.HTTP_TASK, STENCIL_TASK_HTTP);
    }

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
        convertersToJsonMap.put(ServiceTask.class, ServiceTaskJsonConverter.class);
        convertersToJsonMap.put(HttpServiceTask.class, ServiceTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        if (baseElement instanceof PlanItem) {
            PlanItem planItem = (PlanItem) baseElement;
            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition != null && planItemDefinition instanceof ServiceTask) {
                ServiceTask serviceTask = (ServiceTask) planItemDefinition;
                String stencilId = TYPE_TO_STENCILSET.get(serviceTask.getType());
                if (stencilId != null) {
                    return stencilId;
                }
            }
        }
        return CmmnStencilConstants.STENCIL_TASK_SERVICE;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
                    BaseElement baseElement, CmmnModel cmmnModel) {

        ServiceTask serviceTask = (ServiceTask) ((PlanItem) baseElement).getPlanItemDefinition();

        if (HttpServiceTask.HTTP_TASK.equalsIgnoreCase(serviceTask.getType())) {
            if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
                propertiesNode.put(PROPERTY_SERVICETASK_CLASS, serviceTask.getImplementation());
            }
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
            setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT, "saveResponseParametersTransient", serviceTask, propertiesNode);
            setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON, "saveResponseVariableAsJson", serviceTask, propertiesNode);

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
        task.setType("java");
        if (StringUtils.isNotEmpty(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            task.setImplementation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_CLASS, elementNode));

        } else if (StringUtils.isNotEmpty(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
            task.setImplementation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_EXPRESSION, elementNode));

        } else if (StringUtils.isNotEmpty(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode))) {
            task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            task.setImplementation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_DELEGATE_EXPRESSION, elementNode));
        }

        if (StringUtils.isNotEmpty(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode))) {
            task.setResultVariableName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_SERVICETASK_RESULT_VARIABLE, elementNode));
        }

        JsonNode fieldsNode = CmmnJsonConverterUtil.getProperty(PROPERTY_SERVICETASK_FIELDS, elementNode);
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
