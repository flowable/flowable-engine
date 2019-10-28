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
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 */
public class SendEventServiceTaskXMLConverter extends ServiceTaskXMLConverter {
    
    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();
    
    public SendEventServiceTaskXMLConverter() {
        EventInParameterParser inParameterParser = new EventInParameterParser();
        childParserMap.put(inParameterParser.getElementName(), inParameterParser);
        EventOutParameterParser outParameterParser = new EventOutParameterParser();
        childParserMap.put(outParameterParser.getElementName(), outParameterParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return SendEventServiceTask.class;
    }

    @Override
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
        
        parseChildElements(getXMLElementName(), sendEventServiceTask, childParserMap, bpmnModel, xtr);
    }
    
    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
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

    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) element;
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_EVENT_IN_PARAMETER, sendEventServiceTask.getEventInParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_EVENT_OUT_PARAMETER, sendEventServiceTask.getEventOutParameters(), didWriteExtensionStartElement, xtw);
        
        return didWriteExtensionStartElement;
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    }
}
