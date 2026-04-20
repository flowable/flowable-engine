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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class StartEventParseHandler extends AbstractActivityBpmnParseHandler<StartEvent> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return StartEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, StartEvent element) {
        if (element.getSubProcess() != null && element.getSubProcess() instanceof EventSubProcess) {
            if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
                EventDefinition eventDefinition = element.getEventDefinitions().get(0);
                if (eventDefinition instanceof MessageEventDefinition) {
                    MessageEventDefinition messageDefinition = fillMessageRef(bpmnParse, eventDefinition);
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessMessageStartEventActivityBehavior(element, messageDefinition));

                } else if (eventDefinition instanceof SignalEventDefinition signalDefinition) {
                    Signal signal = bpmnParse.getBpmnModel().getSignal(signalDefinition.getSignalRef());

                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessSignalStartEventActivityBehavior(
                            element, signalDefinition, signal));

                } else if (eventDefinition instanceof TimerEventDefinition timerEventDefinition) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessTimerStartEventActivityBehavior(
                            element, timerEventDefinition));

                } else if (eventDefinition instanceof ErrorEventDefinition) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessErrorStartEventActivityBehavior(element));
                
                } else if (eventDefinition instanceof EscalationEventDefinition) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessEscalationStartEventActivityBehavior(element));
                
                } else if (eventDefinition instanceof VariableListenerEventDefinition variableListenerEventDefinition) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessVariableListenerlStartEventActivityBehavior(element, variableListenerEventDefinition));
                }
                
            } else if (hasEventTypeElement(element)) {
                List<ExtensionElement> eventTypeElements = element.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                String eventType = eventTypeElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(eventType)) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessEventRegistryStartEventActivityBehavior(element, eventType));
                }
            }

        } else if (CollectionUtil.isEmpty(element.getEventDefinitions())) {
            element.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneStartEventActivityBehavior(element));
        
        } else if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
            EventDefinition eventDefinition = element.getEventDefinitions().get(0);
            if (eventDefinition instanceof MessageEventDefinition) {
                fillMessageRef(bpmnParse, eventDefinition);
            }
        }

        if (element.getSubProcess() == null && (hasNoEventDefinitionOrTypeElement(element) ||
                bpmnParse.getCurrentProcess().getInitialFlowElement() == null)) {
            
            bpmnParse.getCurrentProcess().setInitialFlowElement(element);
        }
    }
    
    protected MessageEventDefinition fillMessageRef(BpmnParse bpmnParse, EventDefinition eventDefinition) {
        MessageEventDefinition messageDefinition = (MessageEventDefinition) eventDefinition;
        BpmnModel bpmnModel = bpmnParse.getBpmnModel();
        String messageRef = messageDefinition.getMessageRef();
        if (bpmnModel.containsMessageId(messageRef)) {
            Message message = bpmnModel.getMessage(messageRef);
            messageDefinition.setMessageRef(message.getName());
            messageDefinition.setExtensionElements(message.getExtensionElements());
        }
        
        return messageDefinition;
    }
    
    protected boolean hasNoEventDefinitionOrTypeElement(StartEvent element) {
        return CollectionUtil.isEmpty(element.getEventDefinitions()) && !hasEventTypeElement(element);
    }

    protected boolean hasEventTypeElement(StartEvent element) {
        boolean foundEventTypeElement = false;
        List<ExtensionElement> eventTypeElements = element.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
        if (eventTypeElements != null && !eventTypeElements.isEmpty()) {
            foundEventTypeElement = true;
        }
        
        return foundEventTypeElement;
    }
}
