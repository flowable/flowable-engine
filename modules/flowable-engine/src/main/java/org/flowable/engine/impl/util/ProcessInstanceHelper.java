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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ProcessInstanceHelper {

    public ProcessInstance createProcessInstance(ProcessDefinition processDefinition, String businessKey, String processInstanceName, 
                    Map<String, Object> variables, Map<String, Object> transientVariables) {
        
        return createProcessInstance(processDefinition, businessKey, processInstanceName, variables, transientVariables, null, null, false);
    }

    public ProcessInstance createAndStartProcessInstance(ProcessDefinition processDefinition, String businessKey, String processInstanceName, 
                    Map<String, Object> variables, Map<String, Object> transientVariables) {
        
        return createProcessInstance(processDefinition, businessKey, processInstanceName, variables, transientVariables, null, null, true);
    }

    public ProcessInstance createProcessInstance(ProcessDefinition processDefinition, String businessKey, String processInstanceName, Map<String, Object> variables, 
                    Map<String, Object> transientVariables, String callbackId,String callbackType, boolean startProcessInstance) {

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

        FlowElement initialFlowElement = process.getInitialFlowElement();
        if (initialFlowElement == null) {
            throw new FlowableException("No start element found for process definition " + processDefinition.getId());
        }

        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, processInstanceName, initialFlowElement, process, 
                        variables, transientVariables, callbackId, callbackType, startProcessInstance);
    }

    public ProcessInstance createAndStartProcessInstanceByMessage(ProcessDefinition processDefinition, String messageName, String businessKey, 
                    Map<String, Object> variables, Map<String, Object> transientVariables, String callbackId, String callbackType) {

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
            if (flowElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) flowElement;
                if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions()) && startEvent.getEventDefinitions().get(0) instanceof MessageEventDefinition) {

                    MessageEventDefinition messageEventDefinition = (MessageEventDefinition) startEvent.getEventDefinitions().get(0);
                    if (messageEventDefinition.getMessageRef().equals(messageName)) {
                        initialFlowElement = flowElement;
                        break;
                    }
                }
            }
        }
        if (initialFlowElement == null) {
            throw new FlowableException("No message start event found for process definition " + processDefinition.getId() + " and message name " + messageName);
        }

        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, null, initialFlowElement, process, variables, 
                        transientVariables, callbackId, callbackType, true);
    }
    
    public ProcessInstance createAndStartProcessInstanceWithInitialFlowElement(ProcessDefinition processDefinition,
            String businessKey, String processInstanceName, FlowElement initialFlowElement, Process process, Map<String, Object> variables, 
            Map<String, Object> transientVariables, boolean startProcessInstance) {
        
        return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, processInstanceName, initialFlowElement, 
                        process, variables, transientVariables, null, null, startProcessInstance);
    }

    public ProcessInstance createAndStartProcessInstanceWithInitialFlowElement(ProcessDefinition processDefinition, String businessKey, String processInstanceName, 
                    FlowElement initialFlowElement, Process process, Map<String, Object> variables, Map<String, Object> transientVariables, 
                    String callbackId, String callbackType, boolean startProcessInstance) {

        CommandContext commandContext = Context.getCommandContext();

        // Create the process instance
        String initiatorVariableName = null;
        if (initialFlowElement instanceof StartEvent) {
            initiatorVariableName = ((StartEvent) initialFlowElement).getInitiator();
        }

        ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager(commandContext)
                .createProcessInstanceExecution(processDefinition, businessKey, processDefinition.getTenantId(), 
                                initiatorVariableName, initialFlowElement.getId());

        processInstance.setName(processInstanceName);
        
        // Callbacks
        if (callbackId != null) {
            processInstance.setCallbackId(callbackId);
        }
        if (callbackType != null) {
            processInstance.setCallbackType(callbackType);
        }

        CommandContextUtil.getHistoryManager(commandContext).recordProcessInstanceStart(processInstance);

        boolean eventDispatcherEnabled = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled();
        if (eventDispatcherEnabled) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, processInstance));
        }

        processInstance.setVariables(processDataObjects(process.getDataObjects()));

        // Set the variables passed into the start command
        if (variables != null) {
            for (String varName : variables.keySet()) {
                processInstance.setVariable(varName, variables.get(varName));
            }
        }
        if (transientVariables != null) {
            for (String varName : transientVariables.keySet()) {
                processInstance.setTransientVariable(varName, transientVariables.get(varName));
            }
        }
        
        // Fire events
        if (eventDispatcherEnabled) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher()
                    .dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(FlowableEngineEventType.ENTITY_INITIALIZED, processInstance, variables, false));
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(processInstance);
        execution.setCurrentFlowElement(initialFlowElement);

        if (startProcessInstance) {
            startProcessInstance(processInstance, commandContext, variables);
        }
        
        if (callbackId != null) {
            callCaseInstanceStateChangeCallbacks(commandContext, processInstance, null, ProcessInstanceState.RUNNING);
        }

        return processInstance;
    }

    public void startProcessInstance(ExecutionEntity processInstance, CommandContext commandContext, Map<String, Object> variables) {

        Process process = ProcessDefinitionUtil.getProcess(processInstance.getProcessDefinitionId());
        
        processAvailableEventSubProcesses(processInstance, process, commandContext);

        ExecutionEntity execution = processInstance.getExecutions().get(0); // There will always be one child execution created
        CommandContextUtil.getAgenda(commandContext).planContinueProcessOperation(execution);

        if (CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(execution, variables, false));
        }
    }
    
    public void processAvailableEventSubProcesses(ExecutionEntity parentExecution, FlowElementsContainer parentContainer, CommandContext commandContext) {
        // Event sub process handling
        List<MessageEventSubscriptionEntity> messageEventSubscriptions = new LinkedList<>();
        List<SignalEventSubscriptionEntity> signalEventSubscriptions = new LinkedList<>();

        for (FlowElement flowElement : parentContainer.getFlowElements()) {
            if (!(flowElement instanceof EventSubProcess)) {
                continue;
            }

            EventSubProcess eventSubProcess = (EventSubProcess) flowElement;
            for (FlowElement subElement : eventSubProcess.getFlowElements()) {
                if (!(subElement instanceof StartEvent)) {
                    continue;
                }

                StartEvent startEvent = (StartEvent) subElement;
                if (CollectionUtil.isEmpty(startEvent.getEventDefinitions())) {
                    continue;
                }

                EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                if (eventDefinition instanceof MessageEventDefinition) {
                    MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
                    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
                    if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
                        messageEventDefinition.setMessageRef(bpmnModel.getMessage(messageEventDefinition.getMessageRef()).getName());
                    }

                    ExecutionEntity messageExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(parentExecution);
                    messageExecution.setCurrentFlowElement(startEvent);
                    messageExecution.setEventScope(true);
                    messageExecution.setActive(false);

                    messageEventSubscriptions.add(CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insertMessageEvent(
                            messageEventDefinition.getMessageRef(), messageExecution));

                } else if (eventDefinition instanceof SignalEventDefinition) {
                    SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
                    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentExecution.getProcessDefinitionId());
                    Signal signal = null;
                    if (bpmnModel.containsSignalId(signalEventDefinition.getSignalRef())) {
                        signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
                        signalEventDefinition.setSignalRef(signal.getName());
                    }

                    ExecutionEntity signalExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(parentExecution);
                    signalExecution.setCurrentFlowElement(startEvent);
                    signalExecution.setEventScope(true);
                    signalExecution.setActive(false);

                    signalEventSubscriptions.add(CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insertSignalEvent(
                            signalEventDefinition.getSignalRef(), signal, signalExecution));

                } else if (eventDefinition instanceof TimerEventDefinition) {
                    TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;

                    ExecutionEntity timerExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(parentExecution);
                    timerExecution.setCurrentFlowElement(startEvent);
                    timerExecution.setEventScope(true);
                    timerExecution.setActive(false);

                    TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, false, timerExecution, TriggerTimerEventJobHandler.TYPE, 
                                    TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));
                    
                    if (timerJob != null) {
                        CommandContextUtil.getTimerJobService().scheduleTimerJob(timerJob);
                    }
                }
            }
        }

        if (CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            for (MessageEventSubscriptionEntity messageEventSubscription : messageEventSubscriptions) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher()
                        .dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEventSubscription.getActivityId(),
                                messageEventSubscription.getEventName(), null, messageEventSubscription.getExecution().getId(),
                                messageEventSubscription.getProcessInstanceId(), messageEventSubscription.getProcessDefinitionId()));
            }

            for (SignalEventSubscriptionEntity signalEventSubscription : signalEventSubscriptions) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher()
                        .dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEventSubscription.getActivityId(),
                                signalEventSubscription.getEventName(), null, signalEventSubscription.getExecution().getId(),
                                signalEventSubscription.getProcessInstanceId(), signalEventSubscription.getProcessDefinitionId()));
            }
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
    
    public void callCaseInstanceStateChangeCallbacks(CommandContext commandContext, ProcessInstance processInstance, String oldState, String newState) {
        if (processInstance.getCallbackId() != null && processInstance.getCallbackType() != null) {
            Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceCallbacks = CommandContextUtil
                    .getProcessEngineConfiguration(commandContext).getProcessInstanceStateChangedCallbacks();
            if (caseInstanceCallbacks != null && caseInstanceCallbacks.containsKey(processInstance.getCallbackType())) {
                for (RuntimeInstanceStateChangeCallback caseInstanceCallback : caseInstanceCallbacks.get(processInstance.getCallbackType())) {
                    caseInstanceCallback.stateChanged(new CallbackData(processInstance.getCallbackId(), 
                                                                       processInstance.getCallbackType(), 
                                                                       processInstance.getId(), 
                                                                       oldState, 
                                                                       newState));
                }
            }
        }
    }
    
}
