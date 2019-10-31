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
package org.flowable.bpmn.converter;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.EventInParameterParser;
import org.flowable.bpmn.converter.child.EventOutParameterParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.OutParameterParser;
import org.flowable.bpmn.converter.export.FieldExtensionExport;
import org.flowable.bpmn.converter.export.MapExceptionExport;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.AbstractFlowableHttpHandler;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.CustomProperty;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 */
public class ServiceTaskXMLConverter extends BaseBpmnXMLConverter {

    protected Map<String, BaseChildElementParser> caseServiceChildParserMap = new HashMap<>();
    protected Map<String, BaseChildElementParser> sendEventServiceChildParserMap = new HashMap<>();

    public ServiceTaskXMLConverter() {

        // Case service
        InParameterParser inParameterParser = new InParameterParser();
        caseServiceChildParserMap.put(inParameterParser.getElementName(), inParameterParser);
        OutParameterParser outParameterParser = new OutParameterParser();
        caseServiceChildParserMap.put(outParameterParser.getElementName(), outParameterParser);

        // Send event service
        EventInParameterParser eventInParameterParser = new EventInParameterParser();
        sendEventServiceChildParserMap.put(eventInParameterParser.getElementName(), eventInParameterParser);
        EventOutParameterParser eventOutParameterParser = new EventOutParameterParser();
        sendEventServiceChildParserMap.put(eventOutParameterParser.getElementName(), eventOutParameterParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return ServiceTask.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_TASK_SERVICE;
    }

    @Override
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        String serviceTaskType = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TYPE, xtr);
        
        ServiceTask serviceTask = null;
        if (ServiceTask.HTTP_TASK.equals(serviceTaskType)) {
            serviceTask = new HttpServiceTask();
            
        } else if (ServiceTask.CASE_TASK.equals(serviceTaskType)) {
            serviceTask = new CaseServiceTask();
            
        } else if (ServiceTask.SEND_EVENT_TASK.equals(serviceTaskType)) {
            serviceTask = new SendEventServiceTask();
            
        } else {
            serviceTask = new ServiceTask();
        }
        
        BpmnXMLUtil.addXMLLocation(serviceTask, xtr);
        if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_CLASS, xtr))) {
            serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            serviceTask.setImplementation(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_CLASS, xtr));

        } else if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_EXPRESSION, xtr))) {
            serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
            serviceTask.setImplementation(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_EXPRESSION, xtr));

        } else if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION, xtr))) {
            serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            serviceTask.setImplementation(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION, xtr));

        } else if ("##WebService".equals(xtr.getAttributeValue(null, ATTRIBUTE_TASK_IMPLEMENTATION))) {
            serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE);
            serviceTask.setOperationRef(parseOperationRef(xtr.getAttributeValue(null, ATTRIBUTE_TASK_OPERATION_REF), model));
        }

        serviceTask.setResultVariableName(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_RESULTVARIABLE, xtr));
        if (StringUtils.isEmpty(serviceTask.getResultVariableName())) {
            serviceTask.setResultVariableName(BpmnXMLUtil.getAttributeValue("resultVariable", xtr));
        }

        serviceTask.setUseLocalScopeForResultVariable(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, xtr)));

        serviceTask.setType(serviceTaskType);
        serviceTask.setExtensionId(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_EXTENSIONID, xtr));

        if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, xtr))) {
            serviceTask.setSkipExpression(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, xtr));
        }
        
        if (serviceTask instanceof CaseServiceTask) {
            convertCaseServiceTaskXMLProperties((CaseServiceTask) serviceTask, model, xtr);
        } else if (serviceTask instanceof SendEventServiceTask) {
            convertSendEventServiceTaskXMLProperties((SendEventServiceTask) serviceTask, model, xtr);
        } else {
            parseChildElements(getXMLElementName(), serviceTask, model, xtr);
        }

        return serviceTask;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {

        if (element instanceof CaseServiceTask) {
            writeCaseServiceTaskAdditionalAttributes(element, model, xtw);

        } else if (element instanceof SendEventServiceTask) {
            writeSendEventServiceAdditionalAttributes(element, model, xtw);

        } else {
            writeServiceTaskAdditionalAttributes((ServiceTask) element, xtw);

        }
    }

    protected void writeCaseServiceTaskAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        CaseServiceTask caseServiceTask = (CaseServiceTask) element;
        writeQualifiedAttribute(ATTRIBUTE_TYPE, ServiceTask.CASE_TASK, xtw);

        if (StringUtils.isNotEmpty(caseServiceTask.getSkipExpression())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, caseServiceTask.getSkipExpression(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseDefinitionKey())) {
            writeQualifiedAttribute(ATTRIBUTE_CASE_TASK_CASE_DEFINITION_KEY, caseServiceTask.getCaseDefinitionKey(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseInstanceName())) {
            writeQualifiedAttribute(ATTRIBUTE_CASE_TASK_CASE_INSTANCE_NAME, caseServiceTask.getCaseInstanceName(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getBusinessKey())) {
            writeQualifiedAttribute(ATTRIBUTE_BUSINESS_KEY, caseServiceTask.getBusinessKey(), xtw);
        }
        if (caseServiceTask.isInheritBusinessKey()) {
            writeQualifiedAttribute(ATTRIBUTE_INHERIT_BUSINESS_KEY, "true", xtw);
        }
        if (caseServiceTask.isSameDeployment()) {
            writeQualifiedAttribute(ATTRIBUTE_SAME_DEPLOYMENT, "true", xtw);
        }
        if (caseServiceTask.isFallbackToDefaultTenant()) {
            writeQualifiedAttribute(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, "true", xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseInstanceIdVariableName())) {
            writeQualifiedAttribute(ATTRIBUTE_ID_VARIABLE_NAME, caseServiceTask.getCaseInstanceIdVariableName(), xtw);
        }
    }

    protected void writeSendEventServiceAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) element;
        writeQualifiedAttribute(ATTRIBUTE_TYPE, ServiceTask.SEND_EVENT_TASK, xtw);

        if (StringUtils.isNotEmpty(sendEventServiceTask.getSkipExpression())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, sendEventServiceTask.getSkipExpression(), xtw);
        }

        if (StringUtils.isNotEmpty(sendEventServiceTask.getEventType())) {
            writeQualifiedAttribute(ATTRIBUTE_EVENT_TYPE, sendEventServiceTask.getEventType(), xtw);
        }

        if (sendEventServiceTask.isTriggerable()) {
            writeQualifiedAttribute(ATTRIBUTE_TRIGGERABLE, "true", xtw);
        }

        if (StringUtils.isNotEmpty(sendEventServiceTask.getTriggerEventType())) {
            writeQualifiedAttribute(ATTRIBUTE_TRIGGER_EVENT_TYPE, sendEventServiceTask.getTriggerEventType(), xtw);
        }
    }

    protected void writeServiceTaskAdditionalAttributes(ServiceTask element, XMLStreamWriter xtw) throws Exception {
        ServiceTask serviceTask = element;

        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_CLASS, serviceTask.getImplementation(), xtw);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(serviceTask.getImplementationType())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_EXPRESSION, serviceTask.getImplementation(), xtw);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION, serviceTask.getImplementation(), xtw);
        }

        if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_RESULTVARIABLE, serviceTask.getResultVariableName(), xtw);
        }
        if (StringUtils.isNotEmpty(serviceTask.getType())) {
            writeQualifiedAttribute(ATTRIBUTE_TYPE, serviceTask.getType(), xtw);
        }
        if (StringUtils.isNotEmpty(serviceTask.getExtensionId())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_EXTENSIONID, serviceTask.getExtensionId(), xtw);
        }
        if (StringUtils.isNotEmpty(serviceTask.getSkipExpression())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, serviceTask.getSkipExpression(), xtw);
        }
        if (serviceTask.isTriggerable()) {
            writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_TRIGGERABLE, "true", xtw);
        }

        if (serviceTask.isUseLocalScopeForResultVariable()) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE, "true", xtw);
        }
    }

    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        if (element instanceof CaseServiceTask) {
            return writeCaseServiceTaskExtensionChildElements(element, didWriteExtensionStartElement, xtw);

        } else if (element instanceof SendEventServiceTask) {
            return writeSendServiceExtensionChildElements(element, didWriteExtensionStartElement, xtw);

        } else {
            return writeServiceTaskExtensionChildElements((ServiceTask) element, didWriteExtensionStartElement, xtw);

        }
    }

    protected  boolean writeServiceTaskExtensionChildElements(ServiceTask element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        ServiceTask serviceTask = element;

        if (!serviceTask.getCustomProperties().isEmpty()) {
            writeCustomProperties(serviceTask, didWriteExtensionStartElement, xtw);

        } else {
            if (serviceTask instanceof HttpServiceTask) {
                didWriteExtensionStartElement = writeHttpTaskExtensionElements((HttpServiceTask) serviceTask, didWriteExtensionStartElement, xtw);
            }

            didWriteExtensionStartElement = FieldExtensionExport.writeFieldExtensions(serviceTask.getFieldExtensions(), didWriteExtensionStartElement, xtw);
            didWriteExtensionStartElement = MapExceptionExport.writeMapExceptionExtensions(serviceTask.getMapExceptions(), didWriteExtensionStartElement, xtw);
        }

        return didWriteExtensionStartElement;
    }

    protected boolean writeSendServiceExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) element;
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_EVENT_IN_PARAMETER, sendEventServiceTask.getEventInParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_EVENT_OUT_PARAMETER, sendEventServiceTask.getEventOutParameters(), didWriteExtensionStartElement, xtw);

        return didWriteExtensionStartElement;
    }

    protected boolean writeCaseServiceTaskExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        CaseServiceTask caseServiceTask = (CaseServiceTask) element;
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_IN_PARAMETERS, caseServiceTask.getInParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_OUT_PARAMETERS, caseServiceTask.getOutParameters(), didWriteExtensionStartElement, xtw);
        return didWriteExtensionStartElement;
    }


    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    }
    
    protected void convertCaseServiceTaskXMLProperties(CaseServiceTask caseServiceTask, BpmnModel bpmnModel, XMLStreamReader xtr) throws Exception {
        String caseDefinitionKey = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CASE_TASK_CASE_DEFINITION_KEY, xtr);
        if (StringUtils.isNotEmpty(caseDefinitionKey)) {
            caseServiceTask.setCaseDefinitionKey(caseDefinitionKey);
        }

        String caseInstanceName = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CASE_TASK_CASE_INSTANCE_NAME, xtr);
        if (StringUtils.isNotEmpty(caseInstanceName)) {
            caseServiceTask.setCaseInstanceName(caseInstanceName);
        }

        caseServiceTask.setBusinessKey(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_BUSINESS_KEY, xtr));
        caseServiceTask.setInheritBusinessKey(Boolean.parseBoolean(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_INHERIT_BUSINESS_KEY, xtr)));
        caseServiceTask.setSameDeployment(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_SAME_DEPLOYMENT, xtr)));
        caseServiceTask.setFallbackToDefaultTenant(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, xtr)));
        caseServiceTask.setCaseInstanceIdVariableName(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ID_VARIABLE_NAME, xtr));
        parseChildElements(getXMLElementName(), caseServiceTask, caseServiceChildParserMap, bpmnModel, xtr);
    }
    
    protected void convertSendEventServiceTaskXMLProperties(SendEventServiceTask sendEventServiceTask, BpmnModel bpmnModel, XMLStreamReader xtr) throws Exception {
        String eventType = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_EVENT_TYPE, xtr);
        if (StringUtils.isNotEmpty(eventType)) {
            sendEventServiceTask.setEventType(eventType);
        }

        String triggerable = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TRIGGERABLE, xtr);
        if ("true".equalsIgnoreCase(triggerable)) {
            sendEventServiceTask.setTriggerable(true);
        }

        String triggerEventType = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TRIGGER_EVENT_TYPE, xtr);
        if (StringUtils.isNotEmpty(triggerEventType)) {
            sendEventServiceTask.setTriggerEventType(triggerEventType);
        }

        parseChildElements(getXMLElementName(), sendEventServiceTask, sendEventServiceChildParserMap, bpmnModel, xtr);
    }
    
    protected boolean writeCustomProperties(ServiceTask serviceTask, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        for (CustomProperty customProperty : serviceTask.getCustomProperties()) {

            if (StringUtils.isEmpty(customProperty.getSimpleValue())) {
                continue;
            }

            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_FIELD, FLOWABLE_EXTENSIONS_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_FIELD_NAME, customProperty.getName());
            if ((customProperty.getSimpleValue().contains("${") || customProperty.getSimpleValue().contains("#{")) && customProperty.getSimpleValue().contains("}")) {

                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ATTRIBUTE_FIELD_EXPRESSION, FLOWABLE_EXTENSIONS_NAMESPACE);
            } else {
                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_FIELD_STRING, FLOWABLE_EXTENSIONS_NAMESPACE);
            }
            xtw.writeCharacters(customProperty.getSimpleValue());
            xtw.writeEndElement();
            xtw.writeEndElement();
        }
        
        return didWriteExtensionStartElement;
    }
    
    protected boolean writeHttpTaskExtensionElements(HttpServiceTask httpServiceTask, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        if (httpServiceTask.getHttpRequestHandler() != null) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }
            
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_HTTP_REQUEST_HANDLER, FLOWABLE_EXTENSIONS_NAMESPACE);
            writeHttpHandlerAttributes(httpServiceTask.getHttpRequestHandler(), xtw);
            xtw.writeEndElement();
        }
        
        if (httpServiceTask.getHttpResponseHandler() != null) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }
            
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_HTTP_RESPONSE_HANDLER, FLOWABLE_EXTENSIONS_NAMESPACE);
            writeHttpHandlerAttributes(httpServiceTask.getHttpResponseHandler(), xtw);
            xtw.writeEndElement();
        }
        
        return didWriteExtensionStartElement;
    }

    protected String parseOperationRef(String operationRef, BpmnModel model) {
        String result = null;
        if (StringUtils.isNotEmpty(operationRef)) {
            int indexOfP = operationRef.indexOf(':');
            if (indexOfP != -1) {
                String prefix = operationRef.substring(0, indexOfP);
                String resolvedNamespace = model.getNamespace(prefix);
                result = resolvedNamespace + ":" + operationRef.substring(indexOfP + 1);
            } else {
                result = model.getTargetNamespace() + ":" + operationRef;
            }
        }
        return result;
    }
    
    protected void writeHttpHandlerAttributes(AbstractFlowableHttpHandler httpHandler, XMLStreamWriter xtw) throws Exception {
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(httpHandler.getImplementationType())) {
            xtw.writeAttribute(ATTRIBUTE_TASK_SERVICE_CLASS, httpHandler.getImplementation());
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(httpHandler.getImplementationType())) {
            xtw.writeAttribute(ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION, httpHandler.getImplementation());
        }
    }
}
