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
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * This class is responsible for finding and executing error handlers for BPMN Errors.
 * 
 * Possible error handlers include Error Intermediate Events and Error Event Sub-Processes.
 * 
 * @author Tijs Rademakers
 * @author Saeid Mirzaei
 */
public class ErrorPropagation {

    public static void propagateError(BpmnError error, DelegateExecution execution) {
        propagateError(error.getErrorCode(), execution);
    }

    public static void propagateError(String errorCode, DelegateExecution execution) {
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
                eventMap.putAll(findCatchingEventsForProcess(processDefinitionId, errorCode));
            }
        }
        
        eventMap.putAll(findCatchingEventsForProcess(execution.getProcessDefinitionId(), errorCode));
        if (eventMap.size() > 0) {
            executeCatch(eventMap, execution, errorCode);
        }

        if (eventMap.size() == 0) {
            throw new BpmnError(errorCode, "No catching boundary event found for error with errorCode '" + errorCode + "', neither in same process nor in parent process");
        }
    }

    protected static void executeCatch(Map<String, List<Event>> eventMap, DelegateExecution delegateExecution, String errorId) {
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
                                String errorCode = getErrorCodeFromErrorEventDefinition(matchingEvent);
                                if (StringUtils.isNotEmpty(errorCode)) {
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

                // Delete
                executionEntityManager.deleteProcessInstanceExecutionEntity(processInstanceEntity.getId(),
                                currentExecution.getCurrentFlowElement() != null ? currentExecution.getCurrentFlowElement().getId() : null,
                                                "ERROR_EVENT " + errorId, false, false, false);

                // Event
                if (CommandContextUtil.getProcessEngineConfiguration() != null && CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                    CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT, processInstanceEntity));
                }
            }
            
            executeEventHandler(matchingEvent, parentExecution, currentExecution, errorId);
            
        } else {
            throw new FlowableException("No matching parent execution for error code " + errorId + " found");
        }
    }

    protected static void executeEventHandler(Event event, ExecutionEntity parentExecution, ExecutionEntity currentExecution, String errorId) {
        if (CommandContextUtil.getProcessEngineConfiguration() != null && CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
            if (bpmnModel != null) {

                String errorCode = bpmnModel.getErrors().get(errorId);
                if (errorCode == null) {
                    errorCode = errorId;
                }

                CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createErrorEvent(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED, event.getId(), errorId, errorCode, parentExecution.getId(),
                                parentExecution.getProcessInstanceId(), parentExecution.getProcessDefinitionId()));
            }
        }

        if (event instanceof StartEvent) {
            ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

            if (parentExecution.isProcessInstanceType()) {
                executionEntityManager.deleteChildExecutions(parentExecution, null, true);
            } else if (!currentExecution.getParentId().equals(parentExecution.getId())) {
                CommandContextUtil.getAgenda().planDestroyScopeOperation(currentExecution);
            } else {
                executionEntityManager.deleteExecutionAndRelatedData(currentExecution, null);
            }

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

            CommandContextUtil.getAgenda().planTriggerExecutionOperation(boundaryExecution);
        }
    }

    protected static Map<String, List<Event>> findCatchingEventsForProcess(String processDefinitionId, String errorCode) {
        Map<String, List<Event>> eventMap = new HashMap<>();
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

        String compareErrorCode = retrieveErrorCode(bpmnModel, errorCode);

        List<EventSubProcess> subProcesses = process.findFlowElementsOfType(EventSubProcess.class, true);
        for (EventSubProcess eventSubProcess : subProcesses) {
            for (FlowElement flowElement : eventSubProcess.getFlowElements()) {
                if (flowElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) flowElement;
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions()) && startEvent.getEventDefinitions().get(0) instanceof ErrorEventDefinition) {
                        ErrorEventDefinition errorEventDef = (ErrorEventDefinition) startEvent.getEventDefinitions().get(0);
                        String eventErrorCode = retrieveErrorCode(bpmnModel, errorEventDef.getErrorCode());

                        if (eventErrorCode == null || compareErrorCode == null || eventErrorCode.equals(compareErrorCode)) {
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
            if (boundaryEvent.getAttachedToRefId() != null && CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) && boundaryEvent.getEventDefinitions().get(0) instanceof ErrorEventDefinition) {

                ErrorEventDefinition errorEventDef = (ErrorEventDefinition) boundaryEvent.getEventDefinitions().get(0);
                String eventErrorCode = retrieveErrorCode(bpmnModel, errorEventDef.getErrorCode());

                if (eventErrorCode == null || compareErrorCode == null || eventErrorCode.equals(compareErrorCode)) {
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

    public static boolean mapException(Exception e, ExecutionEntity execution, List<MapExceptionEntry> exceptionMap) {
        String errorCode = findMatchingExceptionMapping(e, exceptionMap);
        if (errorCode != null) {
            propagateError(errorCode, execution);
            return true;
        } else {
            ExecutionEntity callActivityExecution = null;
            ExecutionEntity parentExecution = execution.getParent();
            while (parentExecution != null && callActivityExecution == null) {
                if (parentExecution.getId().equals(parentExecution.getProcessInstanceId())) {
                    if (parentExecution.getSuperExecution() != null) {
                        callActivityExecution = parentExecution.getSuperExecution();
                    } else {
                        parentExecution = null;
                    }
                } else {
                    parentExecution = parentExecution.getParent();
                }
            }

            if (callActivityExecution != null) {
                CallActivity callActivity = (CallActivity) callActivityExecution.getCurrentFlowElement();
                if (CollectionUtil.isNotEmpty(callActivity.getMapExceptions())) {
                    errorCode = findMatchingExceptionMapping(e, callActivity.getMapExceptions());
                    if (errorCode != null) {
                        propagateError(errorCode, callActivityExecution);
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static String findMatchingExceptionMapping(Exception e, List<MapExceptionEntry> exceptionMap) {
        String defaultExceptionMapping = null;

        for (MapExceptionEntry me : exceptionMap) {
            String exceptionClass = me.getClassName();
            String errorCode = me.getErrorCode();

            // save the first mapping with no exception class as default map
            if (StringUtils.isNotEmpty(errorCode) && StringUtils.isEmpty(exceptionClass) && defaultExceptionMapping == null) {
                defaultExceptionMapping = errorCode;
                continue;
            }

            // ignore if error code or class are not defined
            if (StringUtils.isEmpty(errorCode) || StringUtils.isEmpty(exceptionClass)) {
                continue;
            }

            if (e.getClass().getName().equals(exceptionClass)) {
                return errorCode;
            }
            
            if (me.isAndChildren()) {
                Class<?> exceptionClassClass = ReflectUtil.loadClass(exceptionClass);
                if (exceptionClassClass.isAssignableFrom(e.getClass())) {
                    return errorCode;
                }
            }
        }

        return defaultExceptionMapping;
    }
    
    protected static Event getCatchEventFromList(List<Event> events, ExecutionEntity parentExecution) {
        Event selectedEvent = null;
        String selectedEventErrorCode = null;
        
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
        for (Event event : events) {
            String errorCode = getErrorCodeFromErrorEventDefinition(event);
            if (bpmnModel != null) {
                errorCode = retrieveErrorCode(bpmnModel, errorCode);
            }
            
            if (selectedEvent == null || (StringUtils.isEmpty(selectedEventErrorCode) && StringUtils.isNotEmpty(errorCode))) {
                selectedEvent = event;
                selectedEventErrorCode = errorCode;
            }
        }
        
        return selectedEvent;
    }
        
    protected static String getErrorCodeFromErrorEventDefinition(Event event) {
        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            if (eventDefinition instanceof ErrorEventDefinition) {
                return ((ErrorEventDefinition) eventDefinition).getErrorCode();
            }
        }
        
        return null;
    }

    protected static String retrieveErrorCode(BpmnModel bpmnModel, String errorCode) {
        String finalErrorCode = null;
        if (errorCode != null && bpmnModel.containsErrorRef(errorCode)) {
            finalErrorCode = bpmnModel.getErrors().get(errorCode);
        } else {
            finalErrorCode = errorCode;
        }
        return finalErrorCode;
    }
}
