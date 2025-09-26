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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IOParameterUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for finding and executing error handlers for BPMN Errors.
 * <p>
 * Possible error handlers include Error Intermediate Events and Error Event Sub-Processes.
 *
 * @author Tijs Rademakers
 * @author Saeid Mirzaei
 */
public class ErrorPropagation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorPropagation.class);

    public static void propagateError(BpmnError error, DelegateExecution execution) {
        propagateError(new BpmnErrorVariableContainer(error, execution.getTenantId()), execution);
    }

    public static void propagateError(String errorCode, DelegateExecution execution) {
        propagateError(new BpmnErrorVariableContainer(errorCode, execution.getTenantId()), execution);
    }

    protected static void propagateError(String errorCode, Throwable exception, DelegateExecution execution) {
        propagateError(new BpmnErrorVariableContainer(errorCode, exception, execution.getTenantId()), execution);
    }

    protected static void propagateError(BpmnErrorVariableContainer errorVariableContainer, DelegateExecution execution) {
        Map<String, List<Event>> eventMap = new HashMap<>();
        Set<String> rootProcessDefinitionIds = new HashSet<>();
        String errorCode = errorVariableContainer.getErrorCode();
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
            executeCatch(eventMap, execution, errorVariableContainer);
        }

        if (eventMap.size() == 0) {
            BpmnError bpmnError = new BpmnError(errorCode,
                    "No catching boundary event found for error with errorCode '" + errorCode + "', neither in same process nor in parent process");
            bpmnError.setAdditionalDataContainer(errorVariableContainer);
            throw bpmnError;
        }
    }

    protected static void executeCatch(Map<String, List<Event>> eventMap, DelegateExecution delegateExecution,
            BpmnErrorVariableContainer errorVariableContainer) {
        Set<String> toDeleteProcessInstanceIds = new HashSet<>();
        String errorId = errorVariableContainer.getErrorCode();
        LOGGER.debug("Executing error catch for error={}, execution={}, eventMap={}", errorId, delegateExecution, eventMap);
        Event matchingEvent = null;
        ExecutionEntity currentExecution = (ExecutionEntity) delegateExecution;
        ExecutionEntity parentExecution = null;

        /* Only use boundary event, when error handling is not being executed as result of listener invocation.
         * The reason is the lifecycle of 'executions':
         * For example, when the 'end' execution listeners are invoked, the corresponding
         * boundary execution has already been deleted / ended.
         * For the 'start' listener, the corresponding boundary executions might not have been created yet.
         * Therefore, when a BPMNError is thrown out listeners, we only search parent executions for a matching
         * error handler.
         * When an BPMNError is thrown as a result of a list
         *  */
        if (delegateExecution.getCurrentFlowableListener() == null && eventMap.containsKey(currentExecution.getActivityId() + "#" + currentExecution.getProcessDefinitionId())) {
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
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
                FlowableEventDispatcher eventDispatcher = null;
                if (processEngineConfiguration != null) {
                    eventDispatcher = processEngineConfiguration.getEventDispatcher();
                }
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    processEngineConfiguration.getEventDispatcher()
                            .dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT, processInstanceEntity),
                                    processEngineConfiguration.getEngineCfgKey());
                }
            }

            executeEventHandler(matchingEvent, parentExecution, currentExecution, errorVariableContainer);

        } else {
            throw new FlowableException("No matching parent execution for error code " + errorId + " found for " + delegateExecution);
        }
    }

    protected static void executeEventHandler(Event event, ExecutionEntity parentExecution, ExecutionEntity currentExecution,
            BpmnErrorVariableContainer errorVariableContainer) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        FlowableEventDispatcher eventDispatcher = null;

        String errorId = errorVariableContainer.getErrorCode();
        String errorCode = errorId;
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
        if (bpmnModel != null) {
            String modelError = bpmnModel.getErrors().get(errorId);
            if (modelError != null) {
                errorCode = modelError;
                errorVariableContainer.setErrorCode(errorCode);
            }
        }

        if (processEngineConfiguration != null) {
            eventDispatcher = processEngineConfiguration.getEventDispatcher();
        }

        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            processEngineConfiguration.getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createErrorEvent(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED, event.getId(), errorId, errorCode, parentExecution.getId(),
                            parentExecution.getProcessInstanceId(), parentExecution.getProcessDefinitionId()),
                    processEngineConfiguration.getEngineCfgKey());
        }

        if (event instanceof StartEvent) {
            ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

            if (parentExecution.isProcessInstanceType()) {
                LOGGER.debug(
                        "Ending and deleting child executions for parent execution '{}'. Reason: Parent Execution is processIntanceType. Current Execution '{}'",
                        parentExecution, currentExecution);
                executionEntityManager.deleteChildExecutions(parentExecution, null, true);
            } else if (!currentExecution.getParentId().equals(parentExecution.getId())) {
                LOGGER.debug("Planing destroyScopeOperation for execution {}. Reason: {}. Parent execution: {}", currentExecution,
                        "Current execution parentId odes not match parentExecution id", parentExecution);
                CommandContextUtil.getAgenda().planDestroyScopeOperation(currentExecution);
            } else {
                LOGGER.debug("Deleting execution and related data for execution {}.", currentExecution);
                executionEntityManager.deleteExecutionAndRelatedData(currentExecution, null, false);
            }

            ExecutionEntity eventSubProcessExecution = executionEntityManager.createChildExecution(parentExecution);
            injectErrorContext(event, eventSubProcessExecution, errorVariableContainer, processEngineConfiguration.getExpressionManager());
            if (event.getSubProcess() != null) {
                eventSubProcessExecution.setCurrentFlowElement(event.getSubProcess());
                CommandContextUtil.getActivityInstanceEntityManager().recordActivityStart(eventSubProcessExecution);
                ExecutionEntity subProcessStartEventExecution = executionEntityManager.createChildExecution(eventSubProcessExecution);
                subProcessStartEventExecution.setCurrentFlowElement(event);
                LOGGER.debug("Error StartEvent SubProcess handler {}{} for errorCode {} continueProcessOperation with execution {}", event,
                        event.getSubProcess(), errorCode,
                        subProcessStartEventExecution);
                CommandContextUtil.getAgenda().planContinueProcessOperation(subProcessStartEventExecution);

            } else {
                LOGGER.debug("Error StartEvent handler {} for errorCode {} continueProcessOperation with execution {}", event, errorCode,
                        eventSubProcessExecution);
                eventSubProcessExecution.setCurrentFlowElement(event);
                CommandContextUtil.getAgenda().planContinueProcessOperation(eventSubProcessExecution);
            }

        } else {
            // boundary event
            ExecutionEntity boundaryExecution = null;
            List<? extends ExecutionEntity> childExecutions = parentExecution.getExecutions();
            for (ExecutionEntity childExecution : childExecutions) {
                if (childExecution != null
                        && childExecution.getActivityId() != null
                        && childExecution.getActivityId().equals(event.getId())) {
                    boundaryExecution = childExecution;
                }
            }
            injectErrorContext(event, boundaryExecution, errorVariableContainer, processEngineConfiguration.getExpressionManager());
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
                if (flowElement instanceof StartEvent startEvent) {
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions()) && startEvent.getEventDefinitions()
                            .get(0) instanceof ErrorEventDefinition errorEventDef) {
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
            if (boundaryEvent.getAttachedToRefId() != null && CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) && boundaryEvent
                    .getEventDefinitions().get(0) instanceof ErrorEventDefinition errorEventDef && !(boundaryEvent.getAttachedToRef() instanceof EventSubProcess)) {

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
            propagateError(errorCode, e, execution);
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
                        propagateError(errorCode, e, callActivityExecution);
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
            String rootCause = me.getRootCause();

            // save the first mapping with no exception class as default map
            if (StringUtils.isNotEmpty(errorCode) && StringUtils.isEmpty(exceptionClass) && defaultExceptionMapping == null) {
                // if rootCause is set, check if it matches the exception
                if (StringUtils.isNotEmpty(rootCause)) {
                    if (ExceptionUtils.getRootCause(e).getClass().getName().equals(rootCause)) {
                        defaultExceptionMapping = errorCode;
                        continue;
                    }
                } else {
                    defaultExceptionMapping = errorCode;
                    continue;
                }
            }

            // ignore if error code or class are not defined
            if (StringUtils.isEmpty(errorCode) || StringUtils.isEmpty(exceptionClass)) {
                continue;
            }

            if (e.getClass().getName().equals(exceptionClass)) {
                if (StringUtils.isNotEmpty(rootCause)) {
                    if (ExceptionUtils.getRootCause(e).getClass().getName().equals(rootCause)) {
                        return errorCode;
                    }
                    continue;
                }
                return errorCode;
            }

            if (me.isAndChildren()) {
                Class<?> exceptionClassClass = ReflectUtil.loadClass(exceptionClass);
                if (exceptionClassClass.isAssignableFrom(e.getClass())) {
                    if (StringUtils.isNotEmpty(rootCause)) {
                        if (ExceptionUtils.getRootCause(e).getClass().getName().equals(rootCause)) {
                            return errorCode;
                        }
                    } else {
                        return errorCode;
                    }
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

    public static <E extends Throwable> void handleException(Throwable exc, ExecutionEntity execution, List<MapExceptionEntry> exceptionMap) throws E {
        Throwable cause = exc;
        BpmnError error = null;
        while (cause != null) {
            if (cause instanceof BpmnError) {
                error = (BpmnError) cause;
                break;
            } else if (cause instanceof Exception) {
                if (ErrorPropagation.mapException((Exception) cause, (ExecutionEntity) execution, exceptionMap)) {
                    return;
                }
            }
            cause = cause.getCause();
        }

        if (error != null) {
            ErrorPropagation.propagateError(error, execution);
        } else {
            throw (E) exc;
        }
    }

    protected static void injectErrorContext(Event event, ExecutionEntity execution, BpmnErrorVariableContainer errorSourceContainer,
            ExpressionManager expressionManager) {

        for (EventDefinition eventDefinition : event.getEventDefinitions()) {
            if (!(eventDefinition instanceof ErrorEventDefinition definition)) {
                continue;
            }

            IOParameterUtil.processInParameters(event.getInParameters(), errorSourceContainer, execution, expressionManager);

            String variableName = definition.getErrorVariableName();

            if (variableName == null || variableName.isEmpty()) {
                continue;
            }

            String errorCode = errorSourceContainer.getErrorCode();

            if (definition.getErrorVariableTransient() != null && definition.getErrorVariableTransient()) {
                if (definition.getErrorVariableLocalScope() != null && definition.getErrorVariableLocalScope()) {
                    execution.setTransientVariableLocal(variableName, errorCode);
                } else {
                    execution.setTransientVariable(variableName, errorCode);
                }
            } else {
                if (definition.getErrorVariableLocalScope() != null && definition.getErrorVariableLocalScope()) {
                    execution.setVariableLocal(variableName, errorCode);
                } else {
                    execution.setVariable(variableName, errorCode);
                }
            }
        }
    }

    protected static class BpmnErrorVariableContainer implements VariableContainer {

        private static final String ERROR_CODE_VARIABLE_NAME = "errorCode";
        private static final String ERROR_VARIABLE_NAME = "error";
        private static final String ERROR_MESSAGE_VARIABLE_NAME = "errorMessage";

        protected String errorCode;
        protected Throwable error;
        protected VariableContainer additionalDataContainer;
        protected String tenantId;

        protected BpmnErrorVariableContainer(String errorCode, String tenantId) {
            this.errorCode = errorCode;
            this.tenantId = tenantId;
        }

        protected BpmnErrorVariableContainer(BpmnError error, String tenantId) {
            this.error = error;
            this.additionalDataContainer = error.getAdditionalDataContainer();
            this.errorCode = error.getErrorCode();
            this.tenantId = tenantId;
        }

        protected BpmnErrorVariableContainer(String errorCode, Throwable error, String tenantId) {
            this.error = error;
            if (error instanceof BpmnError) {
                this.additionalDataContainer = ((BpmnError) error).getAdditionalDataContainer();
            }
            this.errorCode = errorCode;
            this.tenantId = tenantId;
        }

        protected String getErrorCode() {
            return errorCode;
        }

        protected void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public boolean hasVariable(String variableName) {
            if (ERROR_CODE_VARIABLE_NAME.equals(variableName) || ERROR_VARIABLE_NAME.equals(variableName) || ERROR_MESSAGE_VARIABLE_NAME.equals(variableName)) {
                return true;
            } else if (additionalDataContainer != null) {
                return additionalDataContainer.hasVariable(variableName);
            }
            return false;
        }

        @Override
        public Object getVariable(String variableName) {
            if (ERROR_CODE_VARIABLE_NAME.equals(variableName)) {
                return errorCode;
            } else if (ERROR_VARIABLE_NAME.equals(variableName)) {
                return error;
            } else if (ERROR_MESSAGE_VARIABLE_NAME.equals(variableName)) {
                return error != null ? error.getMessage() : null;
            } else if (additionalDataContainer != null) {
                return additionalDataContainer.getVariable(variableName);
            }

            return null;
        }

        @Override
        public void setVariable(String variableName, Object variableValue) {
            throw new UnsupportedOperationException("Not allowed to set variables in a bpmn error variable container");
        }

        @Override
        public void setTransientVariable(String variableName, Object variableValue) {
            throw new UnsupportedOperationException("Not allowed to set variables in a bpmn error variable container");
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public Set<String> getVariableNames() {
            if (additionalDataContainer == null) {
                return Set.of(ERROR_CODE_VARIABLE_NAME, ERROR_VARIABLE_NAME, ERROR_MESSAGE_VARIABLE_NAME);
            }

            Set<String> variableKeys = new LinkedHashSet<>(additionalDataContainer.getVariableNames());
            variableKeys.add(ERROR_CODE_VARIABLE_NAME);
            variableKeys.add(ERROR_VARIABLE_NAME);
            variableKeys.add(ERROR_MESSAGE_VARIABLE_NAME);
            return variableKeys;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                    .add("additionalDataContainer=" + additionalDataContainer)
                    .add("tenantId='" + tenantId + "'")
                    .toString();
        }
    }

}
