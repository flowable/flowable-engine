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
package org.flowable.engine.impl.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessStartEventInitializerContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.eventregistry.BpmnEventInstanceOutParameterHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.interceptor.StartProcessInstanceAfterContext;
import org.flowable.engine.interceptor.StartProcessInstanceBeforeContext;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.identitylink.api.IdentityLinkType;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ProcessInstanceHelper {

    public ProcessInstance createProcessInstance(ProcessDefinition processDefinition, String businessKey, String businessStatus, String processInstanceName,
            Map<String, Object> variables, Map<String, Object> transientVariables, String ownerId, String assigneeId) {

        return createProcessInstance(processDefinition, businessKey, businessStatus, processInstanceName, null, null, null,
                variables, transientVariables, null, null, null, null, ownerId, assigneeId, null, false);
    }

    public ProcessInstance createProcessInstance(ProcessDefinition processDefinition, String businessKey, String businessStatus, String processInstanceName,
            String startEventId, String overrideDefinitionTenantId, String predefinedProcessInstanceId, Map<String, Object> variables, Map<String, Object> transientVariables,
            String callbackId, String callbackType, String referenceId, String referenceType, String ownerId, String assigneeId,
            String stageInstanceId, boolean startProcessInstance) {

        CommandContext commandContext = Context.getCommandContext();
        if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext)) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            return compatibilityHandler.startProcessInstance(processDefinition.getKey(), processDefinition.getId(),
                    variables, transientVariables, businessKey, processDefinition.getTenantId(), processInstanceName);
        }

        // Do not start process a process instance if the process definition is suspended
        if (ProcessDefinitionUtil.isProcessDefinitionSuspended(processDefinition.getId())) {
            throw new FlowableException("Cannot start process instance. Process definition " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") is suspended");
        }

        // Get model from cache
        Process process = ProcessDefinitionUtil.getProcess(processDefinition.getId());
        if (process == null) {
            throw new FlowableException("Cannot start process instance. Process model " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") could not be found");
        }
        
        FlowElement initialFlowElement = null;
        if (StringUtils.isNotEmpty(startEventId)) {
            FlowElement startEventFlowElement = process.getFlowElement(startEventId);
            if (startEventFlowElement == null) {
                throw new FlowableException("No start element found with id " + startEventId + " for process definition " + processDefinition.getId());
            }
            
            if (!(startEventFlowElement instanceof StartEvent)) {
                throw new FlowableException("Provide start event id is not a start event " + startEventId + " for process definition " + processDefinition.getId());
            }
            
            initialFlowElement = startEventFlowElement;
            
        } else {
            initialFlowElement = process.getInitialFlowElement();
        }
        
        if (initialFlowElement == null) {
            throw new FlowableException("No start element found for process definition " + processDefinition.getId());
        }

        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, businessStatus, processInstanceName,
                overrideDefinitionTenantId,
                predefinedProcessInstanceId, initialFlowElement, process, variables, transientVariables,
                callbackId, callbackType, referenceId, referenceType, ownerId, assigneeId, stageInstanceId, startProcessInstance);
    }

    public ProcessInstance createAndStartProcessInstanceByMessage(ProcessDefinition processDefinition, String messageName, String businessKey,
            String businessStatus, Map<String, Object> variables, Map<String, Object> transientVariables, String callbackId, String callbackType,
            String referenceId, String referenceType, String ownerId, String assigneeId) {

        CommandContext commandContext = Context.getCommandContext();
        if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext)) {
            return CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().startProcessInstanceByMessage(
                    messageName, variables, transientVariables, businessKey, processDefinition.getTenantId());
        }

        // Do not start process a process instance if the process definition is suspended
        if (ProcessDefinitionUtil.isProcessDefinitionSuspended(processDefinition.getId())) {
            throw new FlowableException("Cannot start process instance. Process definition " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") is suspended");
        }

        // Get model from cache
        Process process = ProcessDefinitionUtil.getProcess(processDefinition.getId());
        if (process == null) {
            throw new FlowableException("Cannot start process instance. Process model " + processDefinition.getName() + " (id = " + processDefinition.getId() + ") could not be found");
        }

        FlowElement initialFlowElement = null;
        for (FlowElement flowElement : process.getFlowElements()) {
            if (flowElement instanceof StartEvent startEvent) {
                if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions()) && startEvent.getEventDefinitions()
                        .get(0) instanceof MessageEventDefinition messageEventDefinition) {
                    String actualMessageName = EventDefinitionExpressionUtil.determineMessageName(commandContext, messageEventDefinition, processDefinition);
                    if (Objects.equals(actualMessageName, messageName)) {
                        initialFlowElement = flowElement;
                        break;
                    }
                }
            }
        }
        if (initialFlowElement == null) {
            throw new FlowableException("No message start event found for process definition " + processDefinition.getId() + " and message name " + messageName);
        }

        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, businessStatus, null, null, null, initialFlowElement,
                process, variables, transientVariables, callbackId, callbackType, referenceId, referenceType, ownerId, assigneeId, null, true);
    }
    
    public ProcessInstance createAndStartProcessInstanceWithInitialFlowElement(ProcessDefinition processDefinition,
            String businessKey, String businessStatus, String processInstanceName, FlowElement initialFlowElement, Process process,
            Map<String, Object> variables,
            Map<String, Object> transientVariables, String ownerId, String assigneeId, boolean startProcessInstance) {
        
        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, businessStatus, processInstanceName, null, null,
                initialFlowElement, process, variables, transientVariables, null, null, null, null,
                ownerId, assigneeId, null, startProcessInstance);
    }

    public ProcessInstance createAndStartProcessInstanceWithInitialFlowElement(ProcessDefinition processDefinition,
            String businessKey, String businessStatus, String processInstanceName,
            String overrideDefinitionTenantId, String predefinedProcessInstanceId,
            FlowElement initialFlowElement, Process process,
            Map<String, Object> variables, Map<String, Object> transientVariables,
            String callbackId, String callbackType, String referenceId, String referenceType, String ownerId, String assigneeId,
            String stageInstanceId, boolean startProcessInstance) {

        CommandContext commandContext = Context.getCommandContext();

        // Create the process instance
        String initiatorVariableName = null;
        if (initialFlowElement instanceof StartEvent) {
            initiatorVariableName = ((StartEvent) initialFlowElement).getInitiator();
        }
        
        String tenantId;
        if (overrideDefinitionTenantId != null) {
            tenantId = overrideDefinitionTenantId;
        } else {
            tenantId = processDefinition.getTenantId();
        }
        
        StartProcessInstanceBeforeContext startInstanceBeforeContext = new StartProcessInstanceBeforeContext(businessKey, businessStatus, processInstanceName,
                callbackId, callbackType, referenceId, referenceType,
                variables, transientVariables, tenantId, ownerId, assigneeId, initiatorVariableName, initialFlowElement.getId(),
                initialFlowElement, process, processDefinition, overrideDefinitionTenantId, predefinedProcessInstanceId);
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (processEngineConfiguration.getStartProcessInstanceInterceptor() != null) {
            processEngineConfiguration.getStartProcessInstanceInterceptor().beforeStartProcessInstance(startInstanceBeforeContext);
        }

        ExecutionEntity processInstance = processEngineConfiguration.getExecutionEntityManager()
                .createProcessInstanceExecution(startInstanceBeforeContext.getProcessDefinition(), startInstanceBeforeContext.getPredefinedProcessInstanceId(),
                        startInstanceBeforeContext.getBusinessKey(), startInstanceBeforeContext.getBusinessStatus(),
                        startInstanceBeforeContext.getProcessInstanceName(), startInstanceBeforeContext.getCallbackId(),
                        startInstanceBeforeContext.getCallbackType(), startInstanceBeforeContext.getReferenceId(),
                        startInstanceBeforeContext.getReferenceType(), stageInstanceId, startInstanceBeforeContext.getTenantId(),
                        startInstanceBeforeContext.getInitiatorVariableName(), startInstanceBeforeContext.getInitialActivityId());

        processEngineConfiguration.getHistoryManager().recordProcessInstanceStart(processInstance);
        
        if (processEngineConfiguration.isLoggingSessionEnabled()) {
            BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_PROCESS_STARTED, "Started process instance with id " + processInstance.getId(), processInstance);
        }

        // add owner and assignee identity links, if set
        if (StringUtils.isNotEmpty(startInstanceBeforeContext.getOwnerId())) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, ownerId, null, IdentityLinkType.OWNER);
        }
        if (StringUtils.isNotEmpty(startInstanceBeforeContext.getAssigneeId())) {
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, assigneeId, null, IdentityLinkType.ASSIGNEE);
        }

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        boolean eventDispatcherEnabled = eventDispatcher != null && eventDispatcher.isEnabled();
        if (eventDispatcherEnabled) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, processInstance),
                    processEngineConfiguration.getEngineCfgKey());
        }

        processInstance.setVariables(processDataObjects(process.getDataObjects()));

        // Set the variables passed into the start command
        if (startInstanceBeforeContext.getVariables() != null) {
            for (String varName : startInstanceBeforeContext.getVariables().keySet()) {
                processInstance.setVariable(varName, startInstanceBeforeContext.getVariables().get(varName));
            }
        }
        
        if (startInstanceBeforeContext.getTransientVariables() != null) {
            
            Object eventInstance = startInstanceBeforeContext.getTransientVariables().get(EventConstants.EVENT_INSTANCE);
            if (eventInstance instanceof EventInstance) {
                BpmnEventInstanceOutParameterHandler outParameterHandler = processEngineConfiguration.getBpmnEventInstanceOutParameterHandler();
                outParameterHandler.handleOutParameters(processInstance, startInstanceBeforeContext.getInitialFlowElement(), (EventInstance) eventInstance);
            }
            
            for (String varName : startInstanceBeforeContext.getTransientVariables().keySet()) {
                processInstance.setTransientVariable(varName, startInstanceBeforeContext.getTransientVariables().get(varName));
            }
        }
        
        // Fire events
        if (eventDispatcherEnabled) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(FlowableEngineEventType.ENTITY_INITIALIZED, 
                    processInstance, startInstanceBeforeContext.getVariables(), false), processEngineConfiguration.getEngineCfgKey());
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(processInstance);
        execution.setCurrentFlowElement(startInstanceBeforeContext.getInitialFlowElement());

        processEngineConfiguration.getActivityInstanceEntityManager().recordActivityStart(execution);

        if (startProcessInstance) {
            startProcessInstance(processInstance, commandContext, startInstanceBeforeContext.getVariables());
        }
        
        if (callbackId != null) {
            callCaseInstanceStateChangeCallbacks(commandContext, processInstance, null, ProcessInstanceState.RUNNING);
        }
        
        if (processEngineConfiguration.getStartProcessInstanceInterceptor() != null) {
            StartProcessInstanceAfterContext startInstanceAfterContext = new StartProcessInstanceAfterContext(processInstance, execution, 
                            startInstanceBeforeContext.getVariables(), startInstanceBeforeContext.getTransientVariables(), 
                            startInstanceBeforeContext.getInitialFlowElement(), startInstanceBeforeContext.getProcess(), 
                            startInstanceBeforeContext.getProcessDefinition());
            
            processEngineConfiguration.getStartProcessInstanceInterceptor().afterStartProcessInstance(startInstanceAfterContext);
        }

        return processInstance;
    }

    public void startProcessInstance(ExecutionEntity processInstance, CommandContext commandContext, Map<String, Object> variables) {

        Process process = ProcessDefinitionUtil.getProcess(processInstance.getProcessDefinitionId());
        
        processAvailableEventSubProcesses(processInstance, process, commandContext);

        ExecutionEntity execution = processInstance.getExecutions().get(0); // There will always be one child execution created
        CommandContextUtil.getAgenda(commandContext).planContinueProcessOperation(execution);

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(execution, variables, false),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }
    
    public void processAvailableEventSubProcesses(ExecutionEntity parentExecution, FlowElementsContainer parentContainer, CommandContext commandContext) {

        for (FlowElement flowElement : parentContainer.getFlowElements()) {
            if (!(flowElement instanceof EventSubProcess)) {
                continue;
            }
            processEventSubProcess(parentExecution, (EventSubProcess) flowElement, commandContext);
        }
    }

    public void processEventSubProcess(ExecutionEntity parentExecution, EventSubProcess eventSubProcess, CommandContext commandContext) {
        List<EventSubscriptionEntity> messageEventSubscriptions = new LinkedList<>();
        List<EventSubscriptionEntity> signalEventSubscriptions = new LinkedList<>();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        for (FlowElement subElement : eventSubProcess.getFlowElements()) {
            if (!(subElement instanceof StartEvent)) {
                continue;
            }
            
            processEventSubProcessStartEvent(subElement, parentExecution, messageEventSubscriptions, 
                    signalEventSubscriptions, processEngineConfiguration, commandContext);
        }
        
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            for (EventSubscriptionEntity messageEventSubscription : messageEventSubscriptions) {
                processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEventSubscription.getActivityId(),
                        messageEventSubscription.getEventName(), null, messageEventSubscription.getExecutionId(),
                        messageEventSubscription.getProcessInstanceId(), messageEventSubscription.getProcessDefinitionId()),
                        processEngineConfiguration.getEngineCfgKey());
            }

            for (EventSubscriptionEntity signalEventSubscription : signalEventSubscriptions) {
                processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEventSubscription.getActivityId(),
                        signalEventSubscription.getEventName(), null, signalEventSubscription.getExecutionId(),
                        signalEventSubscription.getProcessInstanceId(), signalEventSubscription.getProcessDefinitionId()),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }
    }
    
    public void processEventSubProcessStartEvent(FlowElement subElement, ExecutionEntity parentExecution, 
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {
    
        processEventSubProcessStartEvent(subElement, parentExecution, null, null, processEngineConfiguration, commandContext);
    }
    
    public void processEventSubProcessStartEvent(FlowElement subElement, ExecutionEntity parentExecution, 
            List<EventSubscriptionEntity> messageEventSubscriptions, List<EventSubscriptionEntity> signalEventSubscriptions,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {
        
        StartEvent startEvent = (StartEvent) subElement;
        if (CollectionUtil.isEmpty(startEvent.getEventDefinitions())) {
            return;
        }

        if (startEvent.getBehavior() instanceof EventSubProcessStartEventActivityBehavior behavior) {
            behavior.initializeEventSubProcessStart(new EventSubProcessStartEventInitializerContext(
                    parentExecution, startEvent, processEngineConfiguration, commandContext,
                    messageEventSubscriptions, signalEventSubscriptions));
        }
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }
    
    public ProcessDefinition resolveProcessDefinition(String processDefinitionKey, String tenantId,
            boolean fallbackToDefaultTenant, String parentDeploymentId,
            ProcessEngineConfigurationImpl processEngineConfiguration) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = processEngineConfiguration.getProcessDefinitionEntityManager();
        ProcessDefinition processDefinition = null;

        if (tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            if (parentDeploymentId != null) {
                processDefinition = processDefinitionEntityManager
                        .findProcessDefinitionByParentDeploymentAndKeyAndTenantId(parentDeploymentId, processDefinitionKey, tenantId);
            }
            if (processDefinition == null) {
                processDefinition = processDefinitionEntityManager
                        .findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            }
            if (processDefinition == null && fallbackToDefaultTenant) {
                String defaultTenant = processEngineConfiguration.getDefaultTenantProvider()
                        .getDefaultTenant(tenantId, ScopeTypes.BPMN, processDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    processDefinition = processDefinitionEntityManager
                            .findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, defaultTenant);
                } else {
                    processDefinition = processDefinitionEntityManager
                            .findLatestProcessDefinitionByKey(processDefinitionKey);
                }
            }
        } else {
            if (parentDeploymentId != null) {
                processDefinition = processDefinitionEntityManager
                        .findProcessDefinitionByParentDeploymentAndKey(parentDeploymentId, processDefinitionKey);
            }
            if (processDefinition == null) {
                processDefinition = processDefinitionEntityManager
                        .findLatestProcessDefinitionByKey(processDefinitionKey);
            }
        }

        if (processDefinition == null) {
            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                throw new FlowableObjectNotFoundException(
                        "No process definition found for key '" + processDefinitionKey + "'", ProcessDefinition.class);
            } else if (fallbackToDefaultTenant) {
                throw new FlowableObjectNotFoundException(
                        "No process definition found for key '" + processDefinitionKey + "'. Fallback to default tenant was also applied.", ProcessDefinition.class);
            } else {
                throw new FlowableObjectNotFoundException(
                        "Process definition with key '" + processDefinitionKey + "' and tenantId '" + tenantId + "' was not found", ProcessDefinition.class);
            }
        }

        return processDefinition;
    }

    public void callCaseInstanceStateChangeCallbacks(CommandContext commandContext, ProcessInstance processInstance, String oldState, String newState) {
        if (processInstance.getCallbackId() != null && processInstance.getCallbackType() != null) {
            Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceCallbacks = CommandContextUtil
                    .getProcessEngineConfiguration(commandContext).getProcessInstanceStateChangedCallbacks();

            if (caseInstanceCallbacks != null && caseInstanceCallbacks.containsKey(processInstance.getCallbackType())) {
                for (RuntimeInstanceStateChangeCallback caseInstanceCallback : caseInstanceCallbacks.get(processInstance.getCallbackType())) {

                    caseInstanceCallback.stateChanged(new CallbackData(processInstance.getCallbackId(), 
                        processInstance.getCallbackType(), processInstance.getId(), oldState, newState));

                }
            }
        }
    }
    
}
