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

import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.logging.LogMDC;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operation that takes the current {@link FlowElement} set on the {@link ExecutionEntity} and executes the associated {@link ActivityBehavior}. In the case of async, schedules a {@link Job}.
 * 
 * Also makes sure the {@link ExecutionListener} instances are called.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ContinueProcessOperation extends AbstractOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContinueProcessOperation.class);

    protected boolean forceSynchronousOperation;
    protected boolean inCompensation;

    public ContinueProcessOperation(CommandContext commandContext, ExecutionEntity execution,
            boolean forceSynchronousOperation, boolean inCompensation) {

        super(commandContext, execution);
        this.forceSynchronousOperation = forceSynchronousOperation;
        this.inCompensation = inCompensation;
    }

    public ContinueProcessOperation(CommandContext commandContext, ExecutionEntity execution) {
        this(commandContext, execution, false, false);
    }

    @Override
    public void run() {
        FlowElement currentFlowElement = getCurrentFlowElement(execution);
        if (currentFlowElement instanceof FlowNode) {
            continueThroughFlowNode((FlowNode) currentFlowElement);
        } else if (currentFlowElement instanceof SequenceFlow) {
            continueThroughSequenceFlow((SequenceFlow) currentFlowElement);
        } else {
            throw new FlowableException("Programmatic error: no current flow element found or invalid type: " + currentFlowElement + ". Halting.");
        }
    }

    protected void executeProcessStartExecutionListeners() {
        org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
        executeExecutionListeners(process, execution.getParent(), ExecutionListener.EVENTNAME_START);
    }

    protected void continueThroughFlowNode(FlowNode flowNode) {
        
        execution.setActive(true);

        // Check if it's the initial flow element. If so, we must fire the execution listeners for the process too
        if (flowNode.getIncomingFlows() != null
                && flowNode.getIncomingFlows().size() == 0
                && flowNode.getSubProcess() == null) {
            executeProcessStartExecutionListeners();
        }

        // For a subprocess, a new child execution is created that will visit the steps of the subprocess
        // The original execution that arrived here will wait until the subprocess is finished
        // and will then be used to continue the process instance.
        if (flowNode instanceof SubProcess) {
            createChildExecutionForSubProcess((SubProcess) flowNode);
        }

        if (flowNode instanceof Activity && ((Activity) flowNode).hasMultiInstanceLoopCharacteristics()) {
            // the multi instance execution will look at async
            executeMultiInstanceSynchronous(flowNode);

        } else if (forceSynchronousOperation || !flowNode.isAsynchronous()) {
            executeSynchronous(flowNode);

        } else {
            executeAsynchronous(flowNode);
        }
    }

    protected void createChildExecutionForSubProcess(SubProcess subProcess) {
        ExecutionEntity parentScopeExecution = findFirstParentScopeExecution(execution);

        // Create the sub process execution that can be used to set variables
        // We create a new execution and delete the incoming one to have a proper scope that
        // does not conflict anything with any existing scopes

        ExecutionEntity subProcessExecution = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(parentScopeExecution);
        subProcessExecution.setCurrentFlowElement(subProcess);
        subProcessExecution.setScope(true);

        CommandContextUtil.getExecutionEntityManager(commandContext).deleteRelatedDataForExecution(execution, null);
        CommandContextUtil.getExecutionEntityManager(commandContext).delete(execution);
        execution = subProcessExecution;
    }

    protected void executeSynchronous(FlowNode flowNode) {
        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(execution);

        // Execution listener: event 'start'
        if (CollectionUtil.isNotEmpty(flowNode.getExecutionListeners())) {
            executeExecutionListeners(flowNode, ExecutionListener.EVENTNAME_START);
        }

        // Execute any boundary events, sub process boundary events will be executed from the activity behavior
        if (!inCompensation && flowNode instanceof Activity) { // Only activities can have boundary events
            List<BoundaryEvent> boundaryEvents = ((Activity) flowNode).getBoundaryEvents();
            if (CollectionUtil.isNotEmpty(boundaryEvents)) {
                executeBoundaryEvents(boundaryEvents, execution);
            }
        }

        // Execute actual behavior
        ActivityBehavior activityBehavior = (ActivityBehavior) flowNode.getBehavior();

        if (activityBehavior != null) {
            executeActivityBehavior(activityBehavior, flowNode);

        } else {
            LOGGER.debug("No activityBehavior on activity '{}' with execution {}", flowNode.getId(), execution.getId());
            CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation(execution, true);
        }
    }

    protected void executeAsynchronous(FlowNode flowNode) {
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        
        JobEntity job = jobService.createJob();
        job.setExecutionId(execution.getId());
        job.setProcessInstanceId(execution.getProcessInstanceId());
        job.setProcessDefinitionId(execution.getProcessDefinitionId());
        job.setJobHandlerType(AsyncContinuationJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            job.setTenantId(execution.getTenantId());
        }
        
        execution.getJobs().add(job);
        
        jobService.createAsyncJob(job, flowNode.isExclusive());
        jobService.scheduleAsyncJob(job);
    }

    protected void executeMultiInstanceSynchronous(FlowNode flowNode) {

        // Execution listener: event 'start'
        if (CollectionUtil.isNotEmpty(flowNode.getExecutionListeners())) {
            executeExecutionListeners(flowNode, ExecutionListener.EVENTNAME_START);
        }
        
        if (!hasMultiInstanceRootExecution(execution, flowNode)) {
            execution = createMultiInstanceRootExecution(execution);
        }
        
        // Execute any boundary events, sub process boundary events will be executed from the activity behavior
        if (!inCompensation && flowNode instanceof Activity) { // Only activities can have boundary events
            List<BoundaryEvent> boundaryEvents = ((Activity) flowNode).getBoundaryEvents();
            if (CollectionUtil.isNotEmpty(boundaryEvents)) {
                executeBoundaryEvents(boundaryEvents, execution);
            }
        }

        // Execute the multi instance behavior
        ActivityBehavior activityBehavior = (ActivityBehavior) flowNode.getBehavior();

        if (activityBehavior != null) {
            executeActivityBehavior(activityBehavior, flowNode);

        } else {
            throw new FlowableException("Expected an activity behavior in flow node " + flowNode.getId());
        }
    }
    
    protected boolean hasMultiInstanceRootExecution(ExecutionEntity execution, FlowNode flowNode) {
        ExecutionEntity currentExecution = execution.getParent();
        while (currentExecution != null) {
            if (currentExecution.isMultiInstanceRoot() && flowNode.getId().equals(currentExecution.getActivityId())) {
                return true;
            }
            currentExecution = currentExecution.getParent();
        }
        return false;
    }
    
    protected ExecutionEntity createMultiInstanceRootExecution(ExecutionEntity execution) {
        ExecutionEntity parentExecution = execution.getParent();
        FlowElement flowElement = execution.getCurrentFlowElement();
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        executionEntityManager.deleteRelatedDataForExecution(execution, null);
        executionEntityManager.delete(execution);
        
        ExecutionEntity multiInstanceRootExecution = executionEntityManager.createChildExecution(parentExecution);
        multiInstanceRootExecution.setCurrentFlowElement(flowElement);
        multiInstanceRootExecution.setMultiInstanceRoot(true);
        multiInstanceRootExecution.setActive(false);
        return multiInstanceRootExecution;
    }

    protected void executeActivityBehavior(ActivityBehavior activityBehavior, FlowNode flowNode) {
        LOGGER.debug("Executing activityBehavior {} on activity '{}' with execution {}", activityBehavior.getClass(), flowNode.getId(), execution.getId());

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration != null && processEngineConfiguration.getEventDispatcher().isEnabled()) {

            if (flowNode instanceof Activity && ((Activity) flowNode).hasMultiInstanceLoopCharacteristics()) {
                processEngineConfiguration.getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createMultiInstanceActivityEvent(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED, flowNode.getId(),
                                flowNode.getName(), execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode));
            }
            else {
                processEngineConfiguration.getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED, flowNode.getId(), flowNode.getName(), execution.getId(),
                                execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode));
            }
        }

        try {
            activityBehavior.execute(execution);
        } catch (RuntimeException e) {
            if (LogMDC.isMDCEnabled()) {
                LogMDC.putMDCExecution(execution);
            }
            throw e;
        }
    }

    protected void continueThroughSequenceFlow(SequenceFlow sequenceFlow) {

        // Execution listener. Sequenceflow only 'take' makes sense ... but we've supported all three since the beginning
        if (CollectionUtil.isNotEmpty(sequenceFlow.getExecutionListeners())) {
            executeExecutionListeners(sequenceFlow, ExecutionListener.EVENTNAME_START);
            executeExecutionListeners(sequenceFlow, ExecutionListener.EVENTNAME_TAKE);
            executeExecutionListeners(sequenceFlow, ExecutionListener.EVENTNAME_END);
        }

        // Firing event that transition is being taken
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration != null && processEngineConfiguration.getEventDispatcher().isEnabled()) {
            FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
            FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
            processEngineConfiguration.getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createSequenceFlowTakenEvent(
                            execution,
                            FlowableEngineEventType.SEQUENCEFLOW_TAKEN,
                            sequenceFlow.getId(),
                            sourceFlowElement != null ? sourceFlowElement.getId() : null,
                            sourceFlowElement != null ? sourceFlowElement.getName() : null,
                            sourceFlowElement != null ? sourceFlowElement.getClass().getName() : null,
                            sourceFlowElement != null ? ((FlowNode) sourceFlowElement).getBehavior() : null,
                            targetFlowElement != null ? targetFlowElement.getId() : null,
                            targetFlowElement != null ? targetFlowElement.getName() : null,
                            targetFlowElement != null ? targetFlowElement.getClass().getName() : null,
                            targetFlowElement != null ? ((FlowNode) targetFlowElement).getBehavior() : null));
        }

        FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
        execution.setCurrentFlowElement(targetFlowElement);

        LOGGER.debug("Sequence flow '{}' encountered. Continuing process by following it using execution {}", sequenceFlow.getId(), execution.getId());
        
        execution.setActive(false);
        //agenda.planContinueProcessOperation(execution);
        
        if (targetFlowElement instanceof FlowNode) {
            continueThroughFlowNode((FlowNode) targetFlowElement);
        } else {
            agenda.planContinueProcessOperation(execution);
        }
    }

    protected void executeBoundaryEvents(Collection<BoundaryEvent> boundaryEvents, ExecutionEntity execution) {

        // The parent execution becomes a scope, and a child execution is created for each of the boundary events
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())
                    || (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                continue;
            }

            // A Child execution of the current execution is created to represent the boundary event being active
            ExecutionEntity childExecutionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).createChildExecution(execution);
            childExecutionEntity.setParentId(execution.getId());
            childExecutionEntity.setCurrentFlowElement(boundaryEvent);
            childExecutionEntity.setScope(false);

            ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
            LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), childExecutionEntity.getId());
            boundaryEventBehavior.execute(childExecutionEntity);
        }
    }

}
