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
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.asyncexecutor.JobManager;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ProcessInstanceHelper {
  
  public ProcessInstance createProcessInstance(ProcessDefinition processDefinition, 
      String businessKey, String processInstanceName, Map<String, Object> variables, Map<String, Object> transientVariables) {
    
    return createAndStartProcessInstance(processDefinition, businessKey, processInstanceName, variables, transientVariables, false);
  }
  
  public ProcessInstance createAndStartProcessInstance(ProcessDefinition processDefinition, 
      String businessKey, String processInstanceName, Map<String, Object> variables, Map<String, Object> transientVariables) {
    
    return createAndStartProcessInstance(processDefinition, businessKey, processInstanceName, variables, transientVariables, true);
  }
  
  protected ProcessInstance createAndStartProcessInstance(ProcessDefinition processDefinition, 
      String businessKey, String processInstanceName, 
      Map<String, Object> variables, Map<String, Object> transientVariables, boolean startProcessInstance) {
    
    CommandContext commandContext = Context.getCommandContext(); // Todo: ideally, context should be passed here
    if (Flowable5Util.isFlowable5ProcessDefinition(commandContext, processDefinition)) {
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

    return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, businessKey, 
        processInstanceName, initialFlowElement, process, variables, transientVariables, startProcessInstance);
  }

  public ProcessInstance createAndStartProcessInstanceByMessage(ProcessDefinition processDefinition, String messageName, 
      Map<String, Object> variables, Map<String, Object> transientVariables) {
    
    CommandContext commandContext = Context.getCommandContext();
    if (processDefinition.getEngineVersion() != null) {
      if (commandContext.getProcessEngineConfiguration().getFlowable5CompatibilityHandler().isVersion5Tag(processDefinition.getEngineVersion())) {
        Flowable5CompatibilityHandler compatibilityHandler = commandContext.getProcessEngineConfiguration().getFlowable5CompatibilityHandler();

        if (compatibilityHandler == null) {
          throw new FlowableException("Found V5 process definition, but no compatibility handler on the classpath");
        }

        return compatibilityHandler.startProcessInstanceByMessage(messageName, variables, transientVariables, null, processDefinition.getTenantId());
      
      } else {
        throw new FlowableException("Invalid 'engine' for process definition " + processDefinition.getId() + " : " + processDefinition.getEngineVersion());
      }
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

    return createAndStartProcessInstanceWithInitialFlowElement(processDefinition, null, null, initialFlowElement, process, variables, transientVariables, true);
  }

  public ProcessInstance createAndStartProcessInstanceWithInitialFlowElement(ProcessDefinition processDefinition, 
      String businessKey, String processInstanceName, FlowElement initialFlowElement, 
      Process process, Map<String, Object> variables, Map<String, Object> transientVariables, boolean startProcessInstance) {

    CommandContext commandContext = Context.getCommandContext();

    // Create the process instance
    String initiatorVariableName = null;
    if (initialFlowElement instanceof StartEvent) {
      initiatorVariableName = ((StartEvent) initialFlowElement).getInitiator();
    }
    
    ExecutionEntity processInstance = commandContext.getExecutionEntityManager()
    		.createProcessInstanceExecution(processDefinition, businessKey, processDefinition.getTenantId(), initiatorVariableName);
    
    commandContext.getHistoryManager().recordProcessInstanceStart(processInstance, initialFlowElement);

    boolean eventDispatcherEnabled = Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled();
    if (eventDispatcherEnabled) {
      Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
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

    // Set processInstance name
    if (processInstanceName != null) {
      processInstance.setName(processInstanceName);
      commandContext.getHistoryManager().recordProcessInstanceNameChange(processInstance.getId(), processInstanceName);
    }
    
    // Fire events
    if (eventDispatcherEnabled) {
      Context.getProcessEngineConfiguration().getEventDispatcher()
        .dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(FlowableEngineEventType.ENTITY_INITIALIZED, processInstance, variables, false));
    }

    // Create the first execution that will visit all the process definition elements
    ExecutionEntity execution = commandContext.getExecutionEntityManager().createChildExecution(processInstance); 
    execution.setCurrentFlowElement(initialFlowElement);
    
    if (startProcessInstance) {
      startProcessInstance(processInstance, commandContext, variables);
    }

    return processInstance;
  }

  public void startProcessInstance(ExecutionEntity processInstance, CommandContext commandContext, Map<String, Object> variables) {

    Process process = ProcessDefinitionUtil.getProcess(processInstance.getProcessDefinitionId());

    // Event sub process handling
    List<MessageEventSubscriptionEntity> messageEventSubscriptions = new LinkedList<>();
    List<SignalEventSubscriptionEntity> signalEventSubscriptions = new LinkedList<>();
    
    for (FlowElement flowElement : process.getFlowElements()) {
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
          BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processInstance.getProcessDefinitionId());
          if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
            messageEventDefinition.setMessageRef(bpmnModel.getMessage(messageEventDefinition.getMessageRef()).getName());
          }
          
          ExecutionEntity messageExecution = commandContext.getExecutionEntityManager().createChildExecution(processInstance);
          messageExecution.setCurrentFlowElement(startEvent);
          messageExecution.setEventScope(true);
          
          messageEventSubscriptions.add(commandContext.getEventSubscriptionEntityManager().insertMessageEvent(
              messageEventDefinition.getMessageRef(), messageExecution));
        
        } else if (eventDefinition instanceof SignalEventDefinition) {
          SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
          BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processInstance.getProcessDefinitionId());
          Signal signal = null;
          if (bpmnModel.containsSignalId(signalEventDefinition.getSignalRef())) {
            signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
            signalEventDefinition.setSignalRef(signal.getName());
          }
          
          ExecutionEntity signalExecution = commandContext.getExecutionEntityManager().createChildExecution(processInstance);
          signalExecution.setCurrentFlowElement(startEvent);
          signalExecution.setEventScope(true);
          
          signalEventSubscriptions.add(commandContext.getEventSubscriptionEntityManager().insertSignalEvent(
              signalEventDefinition.getSignalRef(), signal, signalExecution));
        
        } else if (eventDefinition instanceof TimerEventDefinition) {
          TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
          
          ExecutionEntity timerExecution = commandContext.getExecutionEntityManager().createChildExecution(processInstance);
          timerExecution.setCurrentFlowElement(startEvent);
          timerExecution.setEventScope(true);
          
          JobManager jobManager = commandContext.getJobManager();
          
          TimerJobEntity timerJob = jobManager.createTimerJob(timerEventDefinition, false, timerExecution, TriggerTimerEventJobHandler.TYPE,
              TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));
          
          if (timerJob != null) {
            jobManager.scheduleTimerJob(timerJob);
          }
        }
      }
    }

    ExecutionEntity execution = processInstance.getExecutions().get(0); // There will always be one child execution created
    commandContext.getAgenda().planContinueProcessOperation(execution);

    if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      FlowableEventDispatcher eventDispatcher = Context.getProcessEngineConfiguration().getEventDispatcher();
      eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(execution, variables, false));

      for (MessageEventSubscriptionEntity messageEventSubscription : messageEventSubscriptions) {
        commandContext.getProcessEngineConfiguration().getEventDispatcher()
            .dispatchEvent(FlowableEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, messageEventSubscription.getActivityId(),
                messageEventSubscription.getEventName(), null, messageEventSubscription.getExecution().getId(),
                messageEventSubscription.getProcessInstanceId(), messageEventSubscription.getProcessDefinitionId()));
      }
      
      for (SignalEventSubscriptionEntity signalEventSubscription : signalEventSubscriptions) {
        commandContext.getProcessEngineConfiguration().getEventDispatcher()
            .dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, signalEventSubscription.getActivityId(),
                signalEventSubscription.getEventName(), null, signalEventSubscription.getExecution().getId(),
                signalEventSubscription.getProcessInstanceId(), signalEventSubscription.getProcessDefinitionId()));
      }
    }
  }
  
  protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
    Map<String, Object> variablesMap = new HashMap<String, Object>();
    // convert data objects to process variables
    if (dataObjects != null) {
      for (ValuedDataObject dataObject : dataObjects) {
        variablesMap.put(dataObject.getName(), dataObject.getValue());
      }
    }
    return variablesMap;
  }
}
