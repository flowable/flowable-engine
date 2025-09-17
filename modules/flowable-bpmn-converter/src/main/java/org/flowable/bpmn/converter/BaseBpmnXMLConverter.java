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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.export.FailedJobRetryCountExport;
import org.flowable.bpmn.converter.export.FlowableListenerExport;
import org.flowable.bpmn.converter.export.MultiInstanceExport;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.Assignment;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.DataAssociation;
import org.flowable.bpmn.model.DataObject;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TerminateEventDefinition;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class BaseBpmnXMLConverter implements BpmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseBpmnXMLConverter.class);

    protected static final List<ExtensionAttribute> defaultElementAttributes = Arrays.asList(new ExtensionAttribute(ATTRIBUTE_ID), new ExtensionAttribute(ATTRIBUTE_NAME));

    protected static final List<ExtensionAttribute> defaultActivityAttributes = Arrays.asList(
            new ExtensionAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS),
            new ExtensionAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_BEFORE),
            new ExtensionAttribute(ATTRIBUTE_ACTIVITY_EXCLUSIVE),
            new ExtensionAttribute(ATTRIBUTE_DEFAULT),
            new ExtensionAttribute(ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION));

    public void convertToBpmnModel(XMLStreamReader xtr, BpmnModel model, Process activeProcess, List<SubProcess> activeSubProcessList) throws Exception {

        String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
        String elementName = xtr.getAttributeValue(null, ATTRIBUTE_NAME);
        boolean async = parseAsync(xtr);
        boolean asyncLeave = parsAsyncLeave(xtr);
        boolean triggerable = parseTriggerable(xtr);
        boolean notExclusive = parseNotExclusive(xtr);
        boolean asyncLeaveNotExclusive = parseAsyncLeaveNotExclusive(xtr);
        String defaultFlow = xtr.getAttributeValue(null, ATTRIBUTE_DEFAULT);
        boolean isForCompensation = parseForCompensation(xtr);

        BaseElement parsedElement = convertXMLToElement(xtr, model);

        if (parsedElement instanceof Artifact currentArtifact) {
            currentArtifact.setId(elementId);

            if (!activeSubProcessList.isEmpty()) {
                activeSubProcessList.get(activeSubProcessList.size() - 1).addArtifact(currentArtifact);

            } else {
                activeProcess.addArtifact(currentArtifact);
            }
        }

        if (parsedElement instanceof FlowElement currentFlowElement) {

            currentFlowElement.setId(elementId);
            if (currentFlowElement.getName() == null) {
                currentFlowElement.setName(elementName);
            }

            if (currentFlowElement instanceof FlowNode flowNode) {
                flowNode.setAsynchronous(async);
                flowNode.setAsynchronousLeave(asyncLeave);
                flowNode.setNotExclusive(notExclusive);
                flowNode.setAsynchronousLeaveNotExclusive(asyncLeaveNotExclusive);
                
                if (currentFlowElement instanceof Activity activity) {

                    activity.setForCompensation(isForCompensation);
                    if (StringUtils.isNotEmpty(defaultFlow)) {
                        activity.setDefaultFlow(defaultFlow);
                    }
                }

                if (currentFlowElement instanceof Gateway gateway) {
                    if (StringUtils.isNotEmpty(defaultFlow)) {
                        gateway.setDefaultFlow(defaultFlow);
                    }
                }

                if (currentFlowElement instanceof ServiceTask serviceTask) {
                    serviceTask.setTriggerable(triggerable);
                }
            }

            if (currentFlowElement instanceof DataObject) {
                if (!activeSubProcessList.isEmpty()) {
                    SubProcess subProcess = activeSubProcessList.get(activeSubProcessList.size() - 1);
                    subProcess.getDataObjects().add((ValuedDataObject) parsedElement);
                } else {
                    activeProcess.getDataObjects().add((ValuedDataObject) parsedElement);
                }
            }

            if (!activeSubProcessList.isEmpty()) {

                SubProcess subProcess = activeSubProcessList.get(activeSubProcessList.size() - 1);
                subProcess.addFlowElement(currentFlowElement);

            } else {
                activeProcess.addFlowElement(currentFlowElement);
            }
        }
    }

    public void convertToXML(XMLStreamWriter xtw, BaseElement baseElement, BpmnModel model, BpmnXMLConverterOptions options) throws Exception {
        xtw.writeStartElement(getXMLElementName());
        boolean didWriteExtensionStartElement = false;
        writeDefaultAttribute(ATTRIBUTE_ID, baseElement.getId(), xtw);
        if (baseElement instanceof FlowElement) {
            String name = ((FlowElement) baseElement).getName();
            if (!(options.isSaveElementNameWithNewLineInExtensionElement() && BpmnXMLUtil.containsNewLine(name))) {
                writeDefaultAttribute(ATTRIBUTE_NAME, name, xtw);
            }
        }

        if (baseElement instanceof FlowNode flowNode) {

            if (flowNode.isAsynchronous()) {
                writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, ATTRIBUTE_VALUE_TRUE, xtw);
                if (flowNode.isNotExclusive()) {
                    writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_EXCLUSIVE, ATTRIBUTE_VALUE_FALSE, xtw);
                }
            }
            if (flowNode.isAsynchronousLeave()) {
                writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_LEAVE, ATTRIBUTE_VALUE_TRUE, xtw);
                if (flowNode.isAsynchronousLeaveNotExclusive()) {
                    writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_LEAVE_EXCLUSIVE, ATTRIBUTE_VALUE_FALSE, xtw); // shared with async
                }
            }

            if (baseElement instanceof Activity activity) {
                if (activity.isForCompensation()) {
                    writeDefaultAttribute(ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION, ATTRIBUTE_VALUE_TRUE, xtw);
                }
                if (StringUtils.isNotEmpty(activity.getDefaultFlow())) {
                    FlowElement defaultFlowElement = model.getFlowElement(activity.getDefaultFlow());
                    if (defaultFlowElement instanceof SequenceFlow) {
                        writeDefaultAttribute(ATTRIBUTE_DEFAULT, activity.getDefaultFlow(), xtw);
                    }
                }
            }

            if (baseElement instanceof Gateway gateway) {
                if (StringUtils.isNotEmpty(gateway.getDefaultFlow())) {
                    FlowElement defaultFlowElement = model.getFlowElement(gateway.getDefaultFlow());
                    if (defaultFlowElement instanceof SequenceFlow) {
                        writeDefaultAttribute(ATTRIBUTE_DEFAULT, gateway.getDefaultFlow(), xtw);
                    }
                }
            }
        }

        writeAdditionalAttributes(baseElement, model, xtw);

        if (baseElement instanceof FlowElement flowElement) {
            if (StringUtils.isNotEmpty(flowElement.getDocumentation())) {

                xtw.writeStartElement(ELEMENT_DOCUMENTATION);
                xtw.writeCharacters(flowElement.getDocumentation());
                xtw.writeEndElement();
            }

            if (options.isSaveElementNameWithNewLineInExtensionElement()) {
                didWriteExtensionStartElement = BpmnXMLUtil.writeElementNameExtensionElement(flowElement, didWriteExtensionStartElement, xtw);
            }
        }

        didWriteExtensionStartElement = writeExtensionChildElements(baseElement, didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = writeListeners(baseElement, didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(baseElement, didWriteExtensionStartElement, model.getNamespaces(), xtw);
        if (baseElement instanceof Activity activity) {
            didWriteExtensionStartElement = FailedJobRetryCountExport.writeFailedJobRetryCount(activity, didWriteExtensionStartElement, xtw);
        }

        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }

        if (baseElement instanceof Activity activity) {
            MultiInstanceExport.writeMultiInstance(activity, model, xtw);

            for (DataAssociation dataInputAssociation : activity.getDataInputAssociations()) {
                writeDataAssociation(ELEMENT_INPUT_ASSOCIATION, dataInputAssociation, xtw);
            }
            for (DataAssociation dataOutputAssociation : activity.getDataOutputAssociations()) {
                writeDataAssociation(ELEMENT_OUTPUT_ASSOCIATION, dataOutputAssociation, xtw);
            }
        }

        writeAdditionalChildElements(baseElement, model, xtw);

        xtw.writeEndElement();
    }

    protected abstract Class<? extends BaseElement> getBpmnElementType();

    protected abstract BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception;

    protected abstract String getXMLElementName();

    protected abstract void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception;

    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        return didWriteExtensionStartElement;
    }

    protected abstract void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception;

    // To BpmnModel converter convenience methods

    protected void parseChildElements(String elementName, BaseElement parentElement, BpmnModel model, XMLStreamReader xtr) throws Exception {
        parseChildElements(elementName, parentElement, null, model, xtr);
    }

    protected void parseChildElements(String elementName, BaseElement parentElement, Map<String, BaseChildElementParser> additionalParsers, BpmnModel model, XMLStreamReader xtr) throws Exception {
        Map<String, BaseChildElementParser> childParsers = new HashMap<>();
        if (additionalParsers != null) {
            childParsers.putAll(additionalParsers);
        }
        BpmnXMLUtil.parseChildElements(elementName, parentElement, xtr, childParsers, model);
    }

    @SuppressWarnings("unchecked")
    protected ExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
        ExtensionElement extensionElement = new ExtensionElement();
        BpmnXMLUtil.addXMLLocation(extensionElement, xtr);
        extensionElement.setName(xtr.getLocalName());
        if (StringUtils.isNotEmpty(xtr.getNamespaceURI())) {
            extensionElement.setNamespace(xtr.getNamespaceURI());
        }
        if (StringUtils.isNotEmpty(xtr.getPrefix())) {
            extensionElement.setNamespacePrefix(xtr.getPrefix());
        }

        BpmnXMLUtil.addCustomAttributes(xtr, extensionElement, defaultElementAttributes);

        boolean readyWithExtensionElement = false;
        while (!readyWithExtensionElement && xtr.hasNext()) {
            xtr.next();
            if (xtr.isCharacters() || XMLStreamReader.CDATA == xtr.getEventType()) {
                if (StringUtils.isNotEmpty(xtr.getText().trim())) {
                    if (extensionElement.getElementText() != null) {
                        extensionElement.setElementText(extensionElement.getElementText() + xtr.getText().trim());
                        
                    } else {
                        extensionElement.setElementText(xtr.getText().trim());
                    }
                }
            } else if (xtr.isStartElement()) {
                ExtensionElement childExtensionElement = parseExtensionElement(xtr);
                extensionElement.addChildElement(childExtensionElement);
                
            } else if (xtr.isEndElement() && extensionElement.getName().equalsIgnoreCase(xtr.getLocalName())) {
                readyWithExtensionElement = true;
            }
        }
        return extensionElement;
    }

    protected boolean parseAsync(XMLStreamReader xtr) {
        boolean async = false;
        String asyncString = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, xtr);

        if (asyncString == null) {
            asyncString = xtr.getAttributeValue(CAMUNDA_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_BEFORE);
        }

        if (ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(asyncString)) {
            async = true;
        }
        return async;
    }

    protected boolean parseNotExclusive(XMLStreamReader xtr) {
        boolean notExclusive = false;
        String exclusiveString = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_EXCLUSIVE, xtr);
        if (ATTRIBUTE_VALUE_FALSE.equalsIgnoreCase(exclusiveString)) {
            notExclusive = true;
        }
        return notExclusive;
    }
    
    protected boolean parseAsyncLeaveNotExclusive(XMLStreamReader xtr) {
        boolean notExclusive = false;
        String exclusiveString = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_LEAVE_EXCLUSIVE, xtr);
        if (ATTRIBUTE_VALUE_FALSE.equalsIgnoreCase(exclusiveString)) {
            notExclusive = true;
        }
        return notExclusive;
    }

    protected boolean parsAsyncLeave(XMLStreamReader xtr) {
        String asyncLeaveString = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS_LEAVE, xtr);
        if (StringUtils.isEmpty(asyncLeaveString)) {
            asyncLeaveString = BpmnXMLUtil.getAttributeValue("asyncAfter", xtr);
        }
        return ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(asyncLeaveString);
    }

    protected boolean parseTriggerable(XMLStreamReader xtr) {
        String triggerable = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_TRIGGERABLE, xtr);
        return ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(triggerable);
    }

    protected boolean parseForCompensation(XMLStreamReader xtr) {
        boolean isForCompensation = false;
        String compensationString = xtr.getAttributeValue(null, ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION);
        if (ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(compensationString)) {
            isForCompensation = true;
        }
        return isForCompensation;
    }

    protected List<String> parseDelimitedList(String expression) {
        return BpmnXMLUtil.parseDelimitedList(expression);
    }

    // To XML converter convenience methods

    protected String convertToDelimitedString(List<String> stringList) {
        return BpmnXMLUtil.convertToDelimitedString(stringList);
    }

    protected boolean writeFormProperties(FlowElement flowElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {

        List<FormProperty> propertyList = null;
        if (flowElement instanceof UserTask) {
            propertyList = ((UserTask) flowElement).getFormProperties();
        } else if (flowElement instanceof StartEvent) {
            propertyList = ((StartEvent) flowElement).getFormProperties();
        }

        if (propertyList != null) {

            for (FormProperty property : propertyList) {

                if (StringUtils.isNotEmpty(property.getId())) {

                    if (!didWriteExtensionStartElement) {
                        xtw.writeStartElement(ELEMENT_EXTENSIONS);
                        didWriteExtensionStartElement = true;
                    }

                    xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_FORMPROPERTY, FLOWABLE_EXTENSIONS_NAMESPACE);
                    writeDefaultAttribute(ATTRIBUTE_FORM_ID, property.getId(), xtw);

                    writeDefaultAttribute(ATTRIBUTE_FORM_NAME, property.getName(), xtw);
                    writeDefaultAttribute(ATTRIBUTE_FORM_TYPE, property.getType(), xtw);
                    writeDefaultAttribute(ATTRIBUTE_FORM_EXPRESSION, property.getExpression(), xtw);
                    writeDefaultAttribute(ATTRIBUTE_FORM_VARIABLE, property.getVariable(), xtw);
                    writeDefaultAttribute(ATTRIBUTE_FORM_DEFAULT, property.getDefaultExpression(), xtw);
                    writeDefaultAttribute(ATTRIBUTE_FORM_DATEPATTERN, property.getDatePattern(), xtw);
                    if (!property.isReadable()) {
                        writeDefaultAttribute(ATTRIBUTE_FORM_READABLE, ATTRIBUTE_VALUE_FALSE, xtw);
                    }
                    if (!property.isWriteable()) {
                        writeDefaultAttribute(ATTRIBUTE_FORM_WRITABLE, ATTRIBUTE_VALUE_FALSE, xtw);
                    }
                    if (property.isRequired()) {
                        writeDefaultAttribute(ATTRIBUTE_FORM_REQUIRED, ATTRIBUTE_VALUE_TRUE, xtw);
                    }

                    for (FormValue formValue : property.getFormValues()) {
                        if (StringUtils.isNotEmpty(formValue.getId())) {
                            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_VALUE, FLOWABLE_EXTENSIONS_NAMESPACE);
                            xtw.writeAttribute(ATTRIBUTE_ID, formValue.getId());
                            xtw.writeAttribute(ATTRIBUTE_NAME, formValue.getName());
                            xtw.writeEndElement();
                        }
                    }

                    xtw.writeEndElement();
                }
            }
        }

        return didWriteExtensionStartElement;
    }

    protected boolean writeListeners(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        return FlowableListenerExport.writeListeners(element, didWriteExtensionStartElement, xtw);
    }

    protected void writeEventDefinitions(Event parentEvent, List<EventDefinition> eventDefinitions, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        for (EventDefinition eventDefinition : eventDefinitions) {
            if (eventDefinition instanceof TimerEventDefinition) {
                writeTimerDefinition(parentEvent, (TimerEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof SignalEventDefinition) {
                writeSignalDefinition(parentEvent, (SignalEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof MessageEventDefinition) {
                writeMessageDefinition(parentEvent, (MessageEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof ConditionalEventDefinition) {
                writeConditionalDefinition(parentEvent, (ConditionalEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof ErrorEventDefinition) {
                writeErrorDefinition(parentEvent, (ErrorEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof EscalationEventDefinition) {
                writeEscalationDefinition(parentEvent, (EscalationEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof TerminateEventDefinition) {
                writeTerminateDefinition(parentEvent, (TerminateEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof CancelEventDefinition) {
                writeCancelDefinition(parentEvent, (CancelEventDefinition) eventDefinition, model, xtw);
                
            } else if (eventDefinition instanceof CompensateEventDefinition) {
                writeCompensateDefinition(parentEvent, (CompensateEventDefinition) eventDefinition, model, xtw);
            }
        }
    }

    protected void writeTimerDefinition(Event parentEvent, TimerEventDefinition timerDefinition, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_EVENT_TIMERDEFINITION);
        if (StringUtils.isNotEmpty(timerDefinition.getCalendarName())) {
            writeQualifiedAttribute(ATTRIBUTE_CALENDAR_NAME, timerDefinition.getCalendarName(), xtw);
        }
        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(timerDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        if (StringUtils.isNotEmpty(timerDefinition.getTimeDate())) {
            xtw.writeStartElement(ATTRIBUTE_TIMER_DATE);
            xtw.writeCharacters(timerDefinition.getTimeDate());
            xtw.writeEndElement();

        } else if (StringUtils.isNotEmpty(timerDefinition.getTimeCycle())) {
            xtw.writeStartElement(ATTRIBUTE_TIMER_CYCLE);

            if (StringUtils.isNotEmpty(timerDefinition.getEndDate())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_END_DATE, timerDefinition.getEndDate());
            }

            xtw.writeCharacters(timerDefinition.getTimeCycle());
            xtw.writeEndElement();

        } else if (StringUtils.isNotEmpty(timerDefinition.getTimeDuration())) {
            xtw.writeStartElement(ATTRIBUTE_TIMER_DURATION);
            xtw.writeCharacters(timerDefinition.getTimeDuration());
            xtw.writeEndElement();
        }

        xtw.writeEndElement();
    }

    protected void writeSignalDefinition(Event parentEvent, SignalEventDefinition signalDefinition, 
            BpmnModel model, XMLStreamWriter xtw) throws Exception {
        
        xtw.writeStartElement(ELEMENT_EVENT_SIGNALDEFINITION);

        if (StringUtils.isNotEmpty(signalDefinition.getSignalRef())) {
            writeDefaultAttribute(ATTRIBUTE_SIGNAL_REF, signalDefinition.getSignalRef(), xtw);
        }
        if (StringUtils.isNotEmpty(signalDefinition.getSignalExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SIGNAL_EXPRESSION, signalDefinition.getSignalExpression());
        }

        if (parentEvent instanceof ThrowEvent && signalDefinition.isAsync()) {
            BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, "true", xtw);
        }
        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(signalDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeCancelDefinition(Event parentEvent, CancelEventDefinition cancelEventDefinition, 
            BpmnModel model, XMLStreamWriter xtw) throws Exception {
        
        xtw.writeStartElement(ELEMENT_EVENT_CANCELDEFINITION);
        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(cancelEventDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeCompensateDefinition(Event parentEvent, CompensateEventDefinition compensateEventDefinition, 
            BpmnModel model, XMLStreamWriter xtw) throws Exception {
        
        xtw.writeStartElement(ELEMENT_EVENT_COMPENSATEDEFINITION);
        writeDefaultAttribute(ATTRIBUTE_COMPENSATE_ACTIVITYREF, compensateEventDefinition.getActivityRef(), xtw);
        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(compensateEventDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeMessageDefinition(Event parentEvent, MessageEventDefinition messageDefinition, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_EVENT_MESSAGEDEFINITION);

        String messageRef = messageDefinition.getMessageRef();
        if (StringUtils.isNotEmpty(messageRef)) {
            // remove the namespace from the message id if set
            if (messageRef.startsWith(model.getTargetNamespace())) {
                messageRef = messageRef.replace(model.getTargetNamespace(), "");
                messageRef = messageRef.replaceFirst(":", "");
            } else {
                for (String prefix : model.getNamespaces().keySet()) {
                    String namespace = model.getNamespace(prefix);
                    if (messageRef.startsWith(namespace)) {
                        messageRef = messageRef.replace(model.getTargetNamespace(), "");
                        messageRef = prefix + messageRef;
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(messageRef)) {
            writeDefaultAttribute(ATTRIBUTE_MESSAGE_REF, messageRef, xtw);
        }
        if (StringUtils.isNotEmpty(messageDefinition.getMessageExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_MESSAGE_EXPRESSION, messageDefinition.getMessageExpression());
        }

        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(messageDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeConditionalDefinition(Event parentEvent, ConditionalEventDefinition conditionalDefinition, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_EVENT_CONDITIONALDEFINITION);
        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(conditionalDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }

        if (StringUtils.isNotEmpty(conditionalDefinition.getConditionExpression())) {
            xtw.writeStartElement(ELEMENT_CONDITION);
            if (conditionalDefinition.getConditionLanguage() != null) {
                xtw.writeAttribute(XSI_PREFIX, XSI_NAMESPACE, "type", "tFormalExpression");
                BpmnXMLUtil.writeDefaultAttribute(BpmnXMLConstants.ATTRIBUTE_SCRIPT_LANGUAGE, conditionalDefinition.getConditionLanguage(), xtw);
            }
            xtw.writeCharacters(conditionalDefinition.getConditionExpression());
            xtw.writeEndElement();
        }

        xtw.writeEndElement();
    }

    protected void writeErrorDefinition(Event parentEvent, ErrorEventDefinition errorDefinition, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_EVENT_ERRORDEFINITION);
        writeDefaultAttribute(ATTRIBUTE_ERROR_REF, errorDefinition.getErrorCode(), xtw);
        writeQualifiedAttribute(ATTRIBUTE_ERROR_VARIABLE_NAME, errorDefinition.getErrorVariableName(), xtw);
        
        if (errorDefinition.getErrorVariableLocalScope() != null) {
            writeQualifiedAttribute(ATTRIBUTE_ERROR_VARIABLE_LOCAL_SCOPE, errorDefinition.getErrorVariableLocalScope().toString(), xtw);
        }
        
        if (errorDefinition.getErrorVariableTransient() != null) {
            writeQualifiedAttribute(ATTRIBUTE_ERROR_VARIABLE_TRANSIENT, errorDefinition.getErrorVariableTransient().toString(), xtw);
        }

        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(errorDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeEscalationDefinition(Event parentEvent, EscalationEventDefinition escalationDefinition, BpmnModel model,
                    XMLStreamWriter xtw) throws Exception {

        xtw.writeStartElement(ELEMENT_EVENT_ESCALATIONDEFINITION);
        writeDefaultAttribute(ATTRIBUTE_ESCALATION_REF, escalationDefinition.getEscalationCode(), xtw);

        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(escalationDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }

    protected void writeTerminateDefinition(Event parentEvent, TerminateEventDefinition terminateDefinition, BpmnModel model,
        XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_EVENT_TERMINATEDEFINITION);

        if (terminateDefinition.isTerminateAll()) {
            writeQualifiedAttribute(ATTRIBUTE_TERMINATE_ALL, "true", xtw);
        }

        if (terminateDefinition.isTerminateMultiInstance()) {
            writeQualifiedAttribute(ATTRIBUTE_TERMINATE_MULTI_INSTANCE, "true", xtw);
        }

        boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(terminateDefinition, false, model.getNamespaces(), xtw);
        if (didWriteExtensionStartElement) {
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }
    
    protected boolean writeVariableListenerDefinition(Event parentEvent, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        if (parentEvent.getEventDefinitions().size() == 1) {
            EventDefinition eventDefinition = parentEvent.getEventDefinitions().iterator().next();
            if (eventDefinition instanceof VariableListenerEventDefinition variableListenerEventDefinition) {
                if (!didWriteExtensionStartElement) {
                    xtw.writeStartElement(ELEMENT_EXTENSIONS);
                    didWriteExtensionStartElement = true;
                }

                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_EVENT_VARIABLELISTENERDEFINITION, FLOWABLE_EXTENSIONS_NAMESPACE);

                if (StringUtils.isNotEmpty(variableListenerEventDefinition.getVariableName())) {
                    writeDefaultAttribute(ATTRIBUTE_VARIABLE_NAME, variableListenerEventDefinition.getVariableName(), xtw);
                }
                
                if (StringUtils.isNotEmpty(variableListenerEventDefinition.getVariableChangeType())) {
                    writeDefaultAttribute(ATTRIBUTE_VARIABLE_CHANGE_TYPE, variableListenerEventDefinition.getVariableChangeType(), xtw);
                }

                xtw.writeEndElement();
            }
        }
        
        return didWriteExtensionStartElement;
    }

    protected void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        BpmnXMLUtil.writeDefaultAttribute(attributeName, value, xtw);
    }

    protected void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        BpmnXMLUtil.writeQualifiedAttribute(attributeName, value, xtw);
    }

    protected void writeDataAssociation(String elementName, DataAssociation dataAssociation, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(elementName);
        writeDefaultAttribute(ATTRIBUTE_ID, dataAssociation.getId(), xtw);
        if (!StringUtils.isEmpty(dataAssociation.getSourceRef())) {
            xtw.writeStartElement(ELEMENT_SOURCE_REF);
            xtw.writeCharacters(dataAssociation.getSourceRef());
            xtw.writeEndElement();
        }
        xtw.writeStartElement(ELEMENT_TARGET_REF);
        xtw.writeCharacters(dataAssociation.getTargetRef());
        xtw.writeEndElement();
        if (!StringUtils.isEmpty(dataAssociation.getTransformation())) {
            xtw.writeStartElement(ELEMENT_TRANSFORMATION);
            xtw.writeCharacters(dataAssociation.getTransformation());
            xtw.writeEndElement();
        }
        for (Assignment assignment : dataAssociation.getAssignments()) {
            xtw.writeStartElement(ELEMENT_ASSIGNMENT);
            xtw.writeStartElement(ELEMENT_FROM);
            xtw.writeCharacters(assignment.getFrom());
            xtw.writeEndElement();
            xtw.writeStartElement(ELEMENT_TO);
            xtw.writeCharacters(assignment.getTo());
            xtw.writeEndElement();
            xtw.writeEndElement();
        }
        xtw.writeEndElement();
    }
}
