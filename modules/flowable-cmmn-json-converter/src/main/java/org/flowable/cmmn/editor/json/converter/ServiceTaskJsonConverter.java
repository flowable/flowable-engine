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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.CmmnModelJsonConverterUtil;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class ServiceTaskJsonConverter extends BaseCmmnJsonConverter {

    protected static final Map<String, String> TYPE_TO_STENCILSET = new HashMap<>();
    static {
        TYPE_TO_STENCILSET.put(HttpServiceTask.HTTP_TASK, STENCIL_TASK_HTTP);
        TYPE_TO_STENCILSET.put(ScriptServiceTask.SCRIPT_TASK, STENCIL_TASK_SCRIPT);
        TYPE_TO_STENCILSET.put(SendEventServiceTask.SEND_EVENT, STENCIL_TASK_SEND_EVENT);
        TYPE_TO_STENCILSET.put(ServiceTask.MAIL_TASK, STENCIL_TASK_MAIL);
    }

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
        convertersToJsonMap.put(ScriptServiceTask.class, ServiceTaskJsonConverter.class);
        convertersToJsonMap.put(SendEventServiceTask.class, ServiceTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        if (baseElement instanceof PlanItem) {
            PlanItem planItem = (PlanItem) baseElement;
            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition instanceof ServiceTask) {
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
                    BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {

        ServiceTask serviceTask = (ServiceTask) ((PlanItem) baseElement).getPlanItemDefinition();
        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, serviceTask);

        if (HttpServiceTask.HTTP_TASK.equalsIgnoreCase(serviceTask.getType())) {
            fillHttpJsonPropertyValues(serviceTask, propertiesNode);

        } else if (ScriptServiceTask.SCRIPT_TASK.equalsIgnoreCase(serviceTask.getType())) {
            fillScriptJsonPropertyValues(serviceTask, propertiesNode);
            
        } else if (SendEventServiceTask.SEND_EVENT.equalsIgnoreCase(serviceTask.getType())) {
            fillSendEventJsonPropertyValues(serviceTask, propertiesNode);
            
        } else if (ServiceTask.MAIL_TASK.equalsIgnoreCase(serviceTask.getType())) {
            fillMailJsonPropertyValues(serviceTask, propertiesNode);
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

            if (serviceTask.isStoreResultVariableAsTransient()) {
                propertiesNode.put(PROPERTY_SERVICETASK_STORE_RESULT_AS_TRANSIENT, serviceTask.isStoreResultVariableAsTransient());
            }

            addFieldExtensions(serviceTask.getFieldExtensions(), propertiesNode);
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

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

        if (CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_SERVICETASK_STORE_RESULT_AS_TRANSIENT, elementNode)) {
            task.setStoreResultVariableAsTransient(true);
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

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, task);

        return task;
    }
    
    protected void fillHttpJsonPropertyValues(ServiceTask serviceTask, ObjectNode propertiesNode) {
        if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
            propertiesNode.put(PROPERTY_SERVICETASK_CLASS, serviceTask.getImplementation());
        }
        Boolean parallelInSameTransaction = ((HttpServiceTask) serviceTask).getParallelInSameTransaction();
        if (parallelInSameTransaction != null) {
            propertiesNode.put(PROPERTY_HTTPTASK_PARALLEL_IN_SAME_TRANSACTION, parallelInSameTransaction.toString());
        }
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_METHOD, "requestMethod", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_URL, "requestUrl", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_HEADERS, "requestHeaders", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_BODY, "requestBody", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_BODY_ENCODING, "requestBodyEncoding", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_TIMEOUT, "requestTimeout", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS, "disallowRedirects", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES, "failStatusCodes", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES, "handleStatusCodes", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_RESPONSE_VARIABLE_NAME, "responseVariableName", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION, "ignoreException", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES, "saveRequestVariables", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS, "saveResponseParameters", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX, "resultVariablePrefix", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT, "saveResponseParametersTransient", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON, "saveResponseVariableAsJson", serviceTask, propertiesNode);
    }
    
    protected void fillScriptJsonPropertyValues(ServiceTask serviceTask, ObjectNode propertiesNode) {
        propertiesNode.put(PROPERTY_SCRIPT_TASK_SCRIPT_FORMAT, ((ScriptServiceTask) serviceTask).getScriptFormat());
        setPropertyFieldValue(PROPERTY_SCRIPT_TASK_SCRIPT_TEXT, "scripttext", serviceTask, propertiesNode);
        if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
            propertiesNode.put(PROPERTY_SERVICETASK_RESULT_VARIABLE, serviceTask.getResultVariableName());
        }
    }
    
    protected void fillSendEventJsonPropertyValues(ServiceTask serviceTask, ObjectNode propertiesNode) {
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) serviceTask;
        
        String eventType = sendEventServiceTask.getEventType();
        if (StringUtils.isNotEmpty(eventType)) {
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_KEY, sendEventServiceTask.getEventType(), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_NAME, getExtensionValue("eventName", sendEventServiceTask), propertiesNode);
            CmmnModelJsonConverterUtil.addEventInParameters(sendEventServiceTask.getExtensionElements().get("eventInParameter"), 
                            propertiesNode, objectMapper);
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, getExtensionValue("channelKey", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, getExtensionValue("channelName", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, getExtensionValue("channelType", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, getExtensionValue("channelDestination", sendEventServiceTask), propertiesNode);
        }
    }

    protected void fillMailJsonPropertyValues(ServiceTask serviceTask, ObjectNode propertiesNode) {
        setPropertyFieldValue(PROPERTY_MAILTASK_HEADERS, "headers", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_TO, "to", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_FROM, "from", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_SUBJECT, "subject", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_CC, "cc", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_BCC, "bcc", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_TEXT, "text", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_HTML, "html", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_HTML_VAR, "htmlVar", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_TEXT_VAR, "textVar", serviceTask, propertiesNode);
        setPropertyFieldValue(PROPERTY_MAILTASK_CHARSET, "charset", serviceTask, propertiesNode);
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

}
