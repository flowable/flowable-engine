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
package org.flowable.engine.impl.agenda;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.debug.ExecutionTreeUtil;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.condition.ConditionUtil;

/**
 * Operation that triggers conditional events for which the condition evaluate to true and continues the process, leaving that activity.
 * 
 * @author Tijs Rademakers
 */
public class EvaluateConditionalEventsOperation extends AbstractOperation {

    public EvaluateConditionalEventsOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    @Override
    public void run() {
        List<ExecutionEntity> allExecutions = new ArrayList<>();
        ExecutionTreeUtil.collectChildExecutions(execution, allExecutions);
        
        String processDefinitionId = execution.getProcessDefinitionId();
        org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        
        List<EventSubProcess> eventSubProcesses = process.findFlowElementsOfType(EventSubProcess.class, false);
        evaluateEventSubProcesses(eventSubProcesses, execution);
        
        for (ExecutionEntity childExecutionEntity : allExecutions) {
            String activityId = childExecutionEntity.getCurrentActivityId();
            FlowElement currentFlowElement = process.getFlowElement(activityId, true);
            if (currentFlowElement instanceof Event event) {
                if (!event.getEventDefinitions().isEmpty() && event.getEventDefinitions().get(0) instanceof ConditionalEventDefinition) {
                
                    ActivityBehavior activityBehavior = (ActivityBehavior) ((FlowNode) currentFlowElement).getBehavior();
                    if (activityBehavior instanceof TriggerableActivityBehavior) {
                        ((TriggerableActivityBehavior) activityBehavior).trigger(childExecutionEntity, null, null);
                    }
                }
            
            } else if (currentFlowElement instanceof SubProcess subProcess) {
                List<EventSubProcess> childEventSubProcesses = subProcess.findAllSubFlowElementInFlowMapOfType(EventSubProcess.class);
                evaluateEventSubProcesses(childEventSubProcesses, childExecutionEntity);
            }
        }
    }
    
    protected void evaluateEventSubProcesses(List<EventSubProcess> eventSubProcesses, ExecutionEntity parentExecution) {
        if (eventSubProcesses != null) {
            for (EventSubProcess eventSubProcess : eventSubProcesses) {
                List<StartEvent> startEvents = eventSubProcess.findAllSubFlowElementInFlowMapOfType(StartEvent.class);
                if (startEvents != null) {
                    for (StartEvent startEvent : startEvents) {
                        
                        if (startEvent.getEventDefinitions() != null && !startEvent.getEventDefinitions().isEmpty() &&
                                startEvent.getEventDefinitions().get(0) instanceof ConditionalEventDefinition conditionalEventDefinition) {
                            
                            CommandContext commandContext = CommandContextUtil.getCommandContext();

                            boolean conditionIsTrue = false;
                            String conditionExpression = conditionalEventDefinition.getConditionExpression();
                            if (StringUtils.isNotEmpty(conditionExpression)) {
	                            String conditionLanguage = conditionalEventDefinition.getConditionLanguage();
	                            conditionIsTrue = ConditionUtil.hasTrueCondition(startEvent.getId(), conditionExpression, conditionLanguage, parentExecution);
                            
                            } else {
                                conditionIsTrue = true;
                            }
                            
                            if (conditionIsTrue) {
                                ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
                                if (startEvent.isInterrupting()) {
                                    executionEntityManager.deleteChildExecutions(parentExecution, null, true);
                                }
    
                                ExecutionEntity eventSubProcessExecution = executionEntityManager.createChildExecution(parentExecution);
                                eventSubProcessExecution.setScope(true);
                                eventSubProcessExecution.setCurrentFlowElement(eventSubProcess);

                                CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityStart(eventSubProcessExecution);
                                
                                ExecutionEntity startEventSubProcessExecution = executionEntityManager.createChildExecution(eventSubProcessExecution);
                                startEventSubProcessExecution.setCurrentFlowElement(startEvent);
                                
                                CommandContextUtil.getAgenda(commandContext).planContinueProcessOperation(startEventSubProcessExecution);
                            }
                        }
                    }
                }
            }
        }
    }

}
