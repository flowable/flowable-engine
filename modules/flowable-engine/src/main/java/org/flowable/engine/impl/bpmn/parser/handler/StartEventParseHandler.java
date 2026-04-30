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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.bpmn.model.EventRegistryEventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class StartEventParseHandler extends AbstractActivityBpmnParseHandler<StartEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return StartEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, StartEvent element) {
        if (element.getSubProcess() instanceof EventSubProcess) {
            if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
                EventDefinition eventDefinition = element.getEventDefinitions().get(0);
                if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT)) {
                    LOGGER.warn("EventDefinition {} is not supported on event sub-process start event {}",
                            eventDefinition.getClass().getSimpleName(), element.getId());
                    return;
                }
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
                } else if (eventDefinition instanceof EventRegistryEventDefinition eventRegistry) {
                    String key = eventRegistry.getEventDefinitionKey();
                    if (StringUtils.isEmpty(key)) {
                        throw new FlowableIllegalArgumentException("EventRegistryEventDefinition on '" + element.getId()
                                + "' has an empty eventDefinitionKey; the engine cannot register an event-registry subscription without a key.");
                    }
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessEventRegistryStartEventActivityBehavior(element, key));
                
                } else {
                    // Custom EventDefinition: delegate to the per-EventDefinition parse-handler chain so a
                    // post-registered handler can install a behavior. Mirrors the dispatch pattern used by
                    // IntermediateCatchEventParseHandler and BoundaryEventParseHandler.
                    bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);
                }
            }

        } else if (CollectionUtil.isEmpty(element.getEventDefinitions())) {
            element.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneStartEventActivityBehavior(element));
        
        } else {
            EventDefinition eventDefinition = element.getEventDefinitions().get(0);
            if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.START_EVENT)) {
                LOGGER.warn("EventDefinition {} is not supported on process-level start event {}",
                        eventDefinition.getClass().getSimpleName(), element.getId());
            } else if (eventDefinition instanceof MessageEventDefinition) {
                fillMessageRef(bpmnParse, eventDefinition);
            }
        }

        if (element.getSubProcess() == null && (CollectionUtil.isEmpty(element.getEventDefinitions()) ||
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
}
