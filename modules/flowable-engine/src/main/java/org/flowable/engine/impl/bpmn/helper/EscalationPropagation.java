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

package org.flowable.engine.impl.bpmn.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * This class is responsible for finding and executing escalation handlers for BPMN Errors.
 * 
 * Possible escalation handlers include Escalation Intermediate Events and Escalation Event Sub-Processes.
 * 
 * @author Tijs Rademakers
 */
public class EscalationPropagation {

    public static void propagateEscalation(Escalation escalation, DelegateExecution execution) {
        propagateEscalation(escalation.getEscalationCode(), escalation.getName(), execution);
    }

    public static void propagateEscalation(String escalationCode, String escalationName, DelegateExecution execution) {
        Map<String, List<Event>> eventMap = new HashMap<>();
        Set<String> rootProcessDefinitionIds = new HashSet<>();
        if (!execution.getProcessInstanceId().equals(execution.getRootProcessInstanceId())) {
            ExecutionEntity parentExecution = (ExecutionEntity) execution;
            while (parentExecution.getParentId() != null || parentExecution.getSuperExecutionId() != null) {
                if (parentExecution.getParentId() != null) {
                    parentExecution = parentExecution.getParent();
                } else {
                    parentExecution = parentExecution.getSuperExecution();
                    rootProcessDefinitionIds.add(parentExecution.getProcessDefinitionId());
                }
            }
        }
        
        if (rootProcessDefinitionIds.size() > 0) {
            for (String processDefinitionId : rootProcessDefinitionIds) {
                eventMap.putAll(findCatchingEventsForProcess(processDefinitionId, escalationCode));
            }
        }
        
        eventMap.putAll(findCatchingEventsForProcess(execution.getProcessDefinitionId(), escalationCode));
        if (eventMap.size() > 0) {
            executeCatch(eventMap, execution, escalationCode, escalationName);
        }
    }

    protected static void executeCatch(Map<String, List<Event>> eventMap, DelegateExecution delegateExecution, String escalationCode, String escalationName) {
        Set<String> toDeleteProcessInstanceIds = new HashSet<>();
        
        Event matchingEvent = null;
        ExecutionEntity currentExecution = (ExecutionEntity) delegateExecution;
        ExecutionEntity parentExecution = null;

        if (eventMap.containsKey(currentExecution.getActivityId() + "#" + currentExecution.getProcessDefinitionId())) {
            // Check for multi instance
            if (currentExecution.getParentId() != null && currentExecution.getParent().isMultiInstanceRoot()) {
                parentExecution = currentExecution.getParent();
            } else {
                parentExecution = currentExecution;
            }
            
            matchingEvent = getCatchEventFromList(eventMap.get(currentExecution.getActivityId() + 
                            "#" + currentExecution.getProcessDefinitionId()), parentExecution);

        } else {
            parentExecution = currentExecution.getParent();
            
            // Traverse parents until one is found that is a scope and matches the activity the boundary event is defined on
            while (matchingEvent == null && parentExecution != null) {
                FlowElementsContainer currentContainer = null;
                if (parentExecution.getCurrentFlowElement() instanceof FlowElementsContainer) {
                    currentContainer = (FlowElementsContainer) parentExecution.getCurrentFlowElement();
                } else if (parentExecution.getId().equals(parentExecution.getProcessInstanceId())) {
                    currentContainer = ProcessDefinitionUtil.getProcess(parentExecution.getProcessDefinitionId());
                }

                if (currentContainer != null) {
                    for (String refId : eventMap.keySet()) {
                        List<Event> events = eventMap.get(refId);
                        if (CollectionUtil.isNotEmpty(events) && events.get(0) instanceof StartEvent) {
                            String refActivityId = refId.substring(0, refId.indexOf('#'));
                            String refProcessDefinitionId = refId.substring(refId.indexOf('#') + 1);
                            if (parentExecution.getProcessDefinitionId().equals(refProcessDefinitionId) && 
                                            currentContainer.getFlowElement(refActivityId) != null) {
                                
                                matchingEvent = getCatchEventFromList(events, parentExecution);
                                EscalationEventDefinition escalationEventDef = getEscalationEventDefinition(matchingEvent);
                                if (StringUtils.isNotEmpty(escalationEventDef.getEscalationCode())) {
                                    break;
                                }
                            }
                        }
                    }
                }

                if (matchingEvent == null) {
                    if (eventMap.containsKey(parentExecution.getActivityId() + "#" + parentExecution.getProcessDefinitionId())) {
                        // Check for multi instance
                        if (parentExecution.getParentId() != null && parentExecution.getParent().isMultiInstanceRoot()) {
                            parentExecution = parentExecution.getParent();
                        }
                        
                        matchingEvent = getCatchEventFromList(eventMap.get(parentExecution.getActivityId() + 
                                        "#" + parentExecution.getProcessDefinitionId()), parentExecution);

                    } else if (StringUtils.isNotEmpty(parentExecution.getParentId())) {
                        parentExecution = parentExecution.getParent();
                        
                    } else {
                        if (parentExecution.getProcessInstanceId().equals(parentExecution.getRootProcessInstanceId()) == false) {
                            toDeleteProcessInstanceIds.add(parentExecution.getProcessInstanceId());
                            parentExecution = parentExecution.getSuperExecution();
                        } else {
                            parentExecution = null;
                        }
                    }
                }
            }
        }

        if (matchingEvent != null && parentExecution != null) {
            
            for (String processInstanceId : toDeleteProcessInstanceIds) {
                ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
                ExecutionEntity processInstanceEntity = executionEntityManager.findById(processInstanceId);

                // Event
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
                FlowableEventDispatcher eventDispatcher = null;
                if (processEngineConfiguration != null) {
                    eventDispatcher = processEngineConfiguration.getEventDispatcher();
                }
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(
                            FlowableEngineEventType.PROCESS_COMPLETED_WITH_ESCALATION_END_EVENT, processInstanceEntity),
                            processEngineConfiguration.getEngineCfgKey());
                }
            }
            
            executeEventHandler(matchingEvent, parentExecution, currentExecution, escalationCode, escalationName);   
        }
    }

    protected static void executeEventHandler(Event event, ExecutionEntity parentExecution, ExecutionEntity currentExecution, String escalationCode, String escalationName) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        FlowableEventDispatcher eventDispatcher = null;
        if (processEngineConfiguration != null) {
            eventDispatcher = processEngineConfiguration.getEventDispatcher();
        }
        
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            processEngineConfiguration.getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createEscalationEvent(FlowableEngineEventType.ACTIVITY_ESCALATION_RECEIVED, event.getId(), escalationCode, 
                            escalationName, parentExecution.getId(), parentExecution.getProcessInstanceId(), parentExecution.getProcessDefinitionId()),
                    processEngineConfiguration.getEngineCfgKey());
        }

        if (event instanceof StartEvent) {
            ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

            ExecutionEntity eventSubProcessExecution = executionEntityManager.createChildExecution(parentExecution);
            eventSubProcessExecution.setCurrentFlowElement(event.getSubProcess() != null ? event.getSubProcess() : event);
            CommandContextUtil.getAgenda().planContinueProcessOperation(eventSubProcessExecution);

        } else {
            ExecutionEntity boundaryExecution = null;
            List<? extends ExecutionEntity> childExecutions = parentExecution.getExecutions();
            for (ExecutionEntity childExecution : childExecutions) {
                if (childExecution != null
                        && childExecution.getActivityId() != null
                        && childExecution.getActivityId().equals(event.getId())) {
                    boundaryExecution = childExecution;
                }
            }
            
            if (boundaryExecution != null && boundaryExecution.isSuspended()) {
                throw new FlowableException(
                        "Cannot propagate escalation '" + escalationName + "' with code '" + escalationCode + "', because " + boundaryExecution
                                + " is suspended");
            }

            CommandContextUtil.getAgenda().planTriggerExecutionOperation(boundaryExecution);
        }
    }

    protected static Map<String, List<Event>> findCatchingEventsForProcess(String processDefinitionId, String escalationCode) {
        Map<String, List<Event>> eventMap = new HashMap<>();
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

        List<EventSubProcess> subProcesses = process.findFlowElementsOfType(EventSubProcess.class, true);
        for (EventSubProcess eventSubProcess : subProcesses) {
            for (FlowElement flowElement : eventSubProcess.getFlowElements()) {
                if (flowElement instanceof StartEvent startEvent) {
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions()) && startEvent.getEventDefinitions()
                            .get(0) instanceof EscalationEventDefinition escalationEventDef) {
                        String eventEscalationCode = null;
                        if (StringUtils.isNotEmpty(escalationEventDef.getEscalationCode()) && bpmnModel.containsEscalationRef(escalationEventDef.getEscalationCode())) {
                            eventEscalationCode = bpmnModel.getEscalation(escalationEventDef.getEscalationCode()).getEscalationCode();
                        } else {
                            eventEscalationCode = escalationEventDef.getEscalationCode();
                        }
                        
                        if (eventEscalationCode == null || escalationCode == null || eventEscalationCode.equals(escalationCode)) {
                            List<Event> startEvents = new ArrayList<>();
                            startEvents.add(startEvent);
                            eventMap.put(eventSubProcess.getId() + "#" + processDefinitionId, startEvents);
                        }
                    }
                }
            }
        }

        List<BoundaryEvent> boundaryEvents = process.findFlowElementsOfType(BoundaryEvent.class, true);
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            if (boundaryEvent.getAttachedToRefId() != null && CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) && boundaryEvent.getEventDefinitions()
                    .get(0) instanceof EscalationEventDefinition escalationEventDef) {

                String eventEscalationCode = null;
                if (StringUtils.isNotEmpty(escalationEventDef.getEscalationCode()) && bpmnModel.containsEscalationRef(escalationEventDef.getEscalationCode())) {
                    eventEscalationCode = bpmnModel.getEscalation(escalationEventDef.getEscalationCode()).getEscalationCode();
                } else {
                    eventEscalationCode = escalationEventDef.getEscalationCode();
                }

                if (eventEscalationCode == null || escalationCode == null || eventEscalationCode.equals(escalationCode)) {
                    List<Event> elementBoundaryEvents = null;
                    if (!eventMap.containsKey(boundaryEvent.getAttachedToRefId() + "#" + processDefinitionId)) {
                        elementBoundaryEvents = new ArrayList<>();
                        eventMap.put(boundaryEvent.getAttachedToRefId() + "#" + processDefinitionId, elementBoundaryEvents);
                    } else {
                        elementBoundaryEvents = eventMap.get(boundaryEvent.getAttachedToRefId() + "#" + processDefinitionId);
                    }
                    elementBoundaryEvents.add(boundaryEvent);
                }
            }
        }
        return eventMap;
    }
    
    protected static Event getCatchEventFromList(List<Event> events, ExecutionEntity parentExecution) {
        Event selectedEvent = null;
        String selectedEventEscalationCode = null;
        
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
        for (Event event : events) {
            EscalationEventDefinition escalationEventDef = getEscalationEventDefinition(event);
            String escalationCode = escalationEventDef.getEscalationCode();
            if (bpmnModel != null) {
                if (StringUtils.isNotEmpty(escalationEventDef.getEscalationCode()) && bpmnModel.containsEscalationRef(escalationEventDef.getEscalationCode())) {
                    escalationCode = bpmnModel.getEscalation(escalationEventDef.getEscalationCode()).getEscalationCode();
                } else {
                    escalationCode = escalationEventDef.getEscalationCode();
                }
            }
            
            if (selectedEvent == null || (StringUtils.isEmpty(selectedEventEscalationCode) && StringUtils.isNotEmpty(escalationCode))) {
                selectedEvent = event;
                selectedEventEscalationCode = escalationCode;
            }
        }
        
        return selectedEvent;
    }
        
    protected static EscalationEventDefinition getEscalationEventDefinition(Event event) {
        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            if (eventDefinition instanceof EscalationEventDefinition) {
                return (EscalationEventDefinition) eventDefinition;
            }
        }
        
        return null;
    }
}
