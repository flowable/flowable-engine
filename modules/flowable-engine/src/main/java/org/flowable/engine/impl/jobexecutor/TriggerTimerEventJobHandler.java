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
package org.flowable.engine.impl.jobexecutor;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class TriggerTimerEventJobHandler implements JobHandler {

    public static final String TYPE = "trigger-timer";

    public String getType() {
        return TYPE;
    }

    public void execute(JobEntity job, String configuration, ExecutionEntity execution, CommandContext commandContext) {

        CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(execution);

        if (CommandContextUtil.getEventDispatcher().isEnabled()) {
            CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_FIRED, job));
        }

        if (execution.getCurrentFlowElement() instanceof BoundaryEvent) {
            List<String> processedElements = new ArrayList<String>();
            dispatchExecutionTimeOut(job, execution, processedElements, commandContext);
        }
    }

    protected void dispatchExecutionTimeOut(JobEntity timerEntity, ExecutionEntity execution, List<String> processedElements, CommandContext commandContext) {
        FlowElement currentElement = execution.getCurrentFlowElement();
        if (currentElement instanceof BoundaryEvent) {
            BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();
            if (boundaryEvent.isCancelActivity() && boundaryEvent.getAttachedToRef() != null) {

                if (!processedElements.contains(boundaryEvent.getId())) {
                    processedElements.add(boundaryEvent.getId());
                    ExecutionEntity parentExecution = execution.getParent();
                    dispatchExecutionTimeOut(timerEntity, parentExecution, processedElements, commandContext);
                }
            }

        } else {

            // flow nodes
            if (execution.getCurrentFlowElement() instanceof FlowNode) {
                processedElements.add(execution.getCurrentActivityId());
                dispatchActivityTimeOut(timerEntity, (FlowNode) execution.getCurrentFlowElement(), execution, commandContext);

                if (execution.getCurrentFlowElement() instanceof UserTask && !execution.isMultiInstanceRoot()) {
                    List<TaskEntity> tasks = execution.getTasks();
                    if (tasks.size() > 0) {
                        tasks.get(0).setCanceled(true);
                    }
                }
            }

            // subprocesses
            if (execution.getCurrentFlowElement() instanceof SubProcess) {
                for (ExecutionEntity subExecution : execution.getExecutions()) {
                    if (!processedElements.contains(subExecution.getCurrentActivityId())) {
                        dispatchExecutionTimeOut(timerEntity, subExecution, processedElements, commandContext);
                    }
                }

                // call activities
            } else if (execution.getCurrentFlowElement() instanceof CallActivity) {
                ExecutionEntity subProcessInstance = CommandContextUtil.getExecutionEntityManager(commandContext).findSubProcessInstanceBySuperExecutionId(execution.getId());
                if (subProcessInstance != null) {
                    List<? extends ExecutionEntity> childExecutions = subProcessInstance.getExecutions();
                    for (ExecutionEntity subExecution : childExecutions) {
                        if (!processedElements.contains(subExecution.getCurrentActivityId())) {
                            dispatchExecutionTimeOut(timerEntity, subExecution, processedElements, commandContext);
                        }
                    }
                }
            }
        }
    }

    protected void dispatchActivityTimeOut(JobEntity timerEntity, FlowNode flowNode, ExecutionEntity execution, CommandContext commandContext) {
        CommandContextUtil.getEventDispatcher().dispatchEvent(
                FlowableEventBuilder.createActivityCancelledEvent(flowNode.getId(), flowNode.getName(), execution.getId(),
                        execution.getProcessInstanceId(), execution.getProcessDefinitionId(), parseActivityType(flowNode), timerEntity));
    }

    protected String parseActivityType(FlowNode flowNode) {
        String elementType = flowNode.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }
}
