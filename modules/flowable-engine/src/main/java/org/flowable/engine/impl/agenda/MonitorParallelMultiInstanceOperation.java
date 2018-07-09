package org.flowable.engine.impl.agenda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.jobexecutor.AsyncParallelMultiInstanceMonitorJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MonitorParallelMultiInstanceOperation extends AbstractOperation {

    private static final String NUMBER_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";

    private static final String NUMBER_OF_INSTANCES = "nrOfInstances";

    private static final String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";

    public static final String FAILING_THE_MONITOR = "Failing the monitor";

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorParallelMultiInstanceOperation.class);

    private List< ? extends ExecutionEntity> childExecutions;

    public MonitorParallelMultiInstanceOperation(CommandContext commandContext, ExecutionEntity rootExecution, List< ? extends ExecutionEntity> executions) {
        super(commandContext, rootExecution);
        this.childExecutions = executions;
    }

    @Override
    public void run() {
        List<ExecutionEntity> completedInstances = determineCompletedInstances(execution);
        int nrOfCompletedInstances = completedInstances.size();
        int nrOfInstances = getVariableOrDefault(execution, NUMBER_OF_INSTANCES, 0);

        LOGGER.debug("nrOfInstances {}", nrOfInstances);
        LOGGER.debug("nrOfCompletedInstances {}", nrOfCompletedInstances);

        Collection<ExecutionEntity> inactiveExecutions = getInactiveExecutions(execution);

        int numberOfFailedInstances = findNumberOfFailedInstances(execution);

        boolean isCompletionConditionSatisfied = completionConditionSatisfied(execution);
        if (nrOfCompletedInstances + numberOfFailedInstances >= nrOfInstances || inactiveExecutions.size() - 1 >= nrOfInstances
                        || isCompletionConditionSatisfied) {
            if (numberOfFailedInstances > 0) {
                throw new RuntimeException(FAILING_THE_MONITOR);
            }
            CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);

            Activity activity = (Activity) execution.getCurrentFlowElement();
            verifyCompensation(execution, execution, activity);
            verifyCallActivity(execution, execution.getActivityId());
            if (isCompletionConditionSatisfied) {
                LinkedList<DelegateExecution> toVerify = new LinkedList<>(execution.getExecutions());
                while (!toVerify.isEmpty()) {
                    DelegateExecution childExecution = toVerify.pop();
                    if (((ExecutionEntity) childExecution).isInserted()) {
                        childExecution.inactivate();
                    }

                    List<DelegateExecution> childExecutions = (List<DelegateExecution>) childExecution.getExecutions();
                    if (childExecutions != null && !childExecutions.isEmpty()) {
                        toVerify.addAll(childExecutions);
                    }
                }
                sendCompletedWithConditionEvent(execution);
            } else {
                sendCompletedEvent(execution);
            }
            cleanupMiRoot(execution);
        } else {
            executeAsynchronousJob();
        }

    }

    private boolean completionConditionSatisfied(ExecutionEntity execution) {
        MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = getMultiInstanceLoopCharacteristics(execution);
        if (multiInstanceLoopCharacteristics.getCompletionCondition() != null) {

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

            String activeCompletionCondition = null;

            if (CommandContextUtil.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
                ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(execution.getActivityId(),
                                execution.getProcessDefinitionId());
                activeCompletionCondition = getActiveValue(multiInstanceLoopCharacteristics.getCompletionCondition(),
                                DynamicBpmnConstants.MULTI_INSTANCE_COMPLETION_CONDITION, taskElementProperties);

            } else {
                activeCompletionCondition = multiInstanceLoopCharacteristics.getCompletionCondition();
            }

            Object value = expressionManager.createExpression(activeCompletionCondition).getValue(execution);

            if (!(value instanceof Boolean)) {
                throw new FlowableIllegalArgumentException("completionCondition '" + activeCompletionCondition + "' does not evaluate to a boolean value");
            }

            Boolean booleanValue = (Boolean) value;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Completion condition of multi-instance satisfied: {}", booleanValue);
            }
            return booleanValue;
        }
        return false;
    }

    protected String getActiveValue(String originalValue, String propertyName, ObjectNode taskElementProperties) {
        String activeValue = originalValue;
        if (taskElementProperties != null) {
            JsonNode overrideValueNode = taskElementProperties.get(propertyName);
            if (overrideValueNode != null) {
                if (overrideValueNode.isNull()) {
                    activeValue = null;
                } else {
                    activeValue = overrideValueNode.asText();
                }
            }
        }
        return activeValue;
    }

    private MultiInstanceLoopCharacteristics getMultiInstanceLoopCharacteristics(ExecutionEntity execution) {
        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        if (currentFlowElement instanceof CallActivity) {
            return ((CallActivity) currentFlowElement).getLoopCharacteristics();
        }

        return ((SubProcess) currentFlowElement).getLoopCharacteristics();
    }

    protected void sendCompletedWithConditionEvent(DelegateExecution execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext())
                        .dispatchEvent(buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION));
    }

    private int findNumberOfFailedInstances(ExecutionEntity execution) {
        List<ExecutionEntity> allChildExecutions = CommandContextUtil.getExecutionEntityManager().collectChildren(execution);
        int result = 0;
        for (ExecutionEntity executionEntity : allChildExecutions) {
            if (hasFailedJobs(executionEntity)) {
                result++;
            }
        }
        return result;
    }

    private boolean hasFailedJobs(ExecutionEntity executionEntity) {
        return findFailedJobsForExecution(executionEntity).size() > 0;
    }

    private List<DeadLetterJobEntity> findFailedJobsForExecution(ExecutionEntity executionEntity) {
        return CommandContextUtil.getJobService().findDeadLetterJobsByExecutionId(executionEntity.getId());
    }

    private Collection<ExecutionEntity> getInactiveExecutions(ExecutionEntity execution) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        return executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(execution.getActivityId(), execution.getProcessInstanceId());
    }

    protected void cleanupMiRoot(DelegateExecution execution) {
        // Delete multi instance root and all child executions.
        // Create a fresh execution to continue
        ExecutionEntity multiInstanceRootExecution = (ExecutionEntity) getMultiInstanceRootExecution(execution);
        FlowElement flowElement = multiInstanceRootExecution.getCurrentFlowElement();
        ExecutionEntity parentExecution = multiInstanceRootExecution.getParent();

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        Collection<String> executionIdsNotToSendCancelledEventsFor = execution.isMultiInstanceRoot() ? null : Collections.singletonList(execution.getId());
        executionEntityManager.deleteChildExecutions(multiInstanceRootExecution, null, executionIdsNotToSendCancelledEventsFor, "MI_END", true, flowElement);
        executionEntityManager.deleteRelatedDataForExecution(multiInstanceRootExecution, "MI_END");
        executionEntityManager.delete(multiInstanceRootExecution);

        ExecutionEntity newExecution = executionEntityManager.createChildExecution(parentExecution);
        newExecution.setCurrentFlowElement(flowElement);
        takeNextTransition(newExecution);
    }

    protected DelegateExecution getMultiInstanceRootExecution(DelegateExecution executionEntity) {
        DelegateExecution multiInstanceRootExecution = null;
        DelegateExecution currentExecution = executionEntity;
        while (currentExecution != null && multiInstanceRootExecution == null && currentExecution.getParent() != null) {
            if (currentExecution.isMultiInstanceRoot()) {
                multiInstanceRootExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return multiInstanceRootExecution;
    }

    private void takeNextTransition(ExecutionEntity newExecution) {
        CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation(newExecution, true);
    }

    private Activity verifyCompensation(DelegateExecution execution, ExecutionEntity executionToUse, Activity activity) {
        boolean hasCompensation = false;
        if (activity instanceof Transaction) {
            hasCompensation = true;
        } else if (activity instanceof SubProcess) {
            SubProcess subProcess = (SubProcess) activity;
            for (FlowElement subElement : subProcess.getFlowElements()) {
                if (subElement instanceof Activity) {
                    Activity subActivity = (Activity) subElement;
                    if (CollectionUtil.isNotEmpty(subActivity.getBoundaryEvents())) {
                        for (BoundaryEvent boundaryEvent : subActivity.getBoundaryEvents()) {
                            if (CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions())
                                            && boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {

                                hasCompensation = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (hasCompensation) {
            ScopeUtil.createCopyOfSubProcessExecutionForCompensation(executionToUse);
        }
        return activity;
    }

    private void verifyCallActivity(ExecutionEntity execution, String activityId) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        if (execution != null) {
            List<String> callActivityExecutionIds = new ArrayList<>();

            // Find all execution entities that are at the call activity
            List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(execution);
            if (childExecutions != null) {
                for (ExecutionEntity childExecution : childExecutions) {
                    // LOGGER.info("Checking child execution {}",
                    // childExecution.getPersistentState());
                    if (activityId.equals(childExecution.getCurrentActivityId())) {
                        callActivityExecutionIds.add(childExecution.getId());
                    }
                }

                // Now all call activity executions have been collected, loop
                // again and check which should be removed
                for (int i = childExecutions.size() - 1; i >= 0; i--) {
                    ExecutionEntity childExecution = childExecutions.get(i);
                    if (StringUtils.isNotEmpty(childExecution.getSuperExecutionId())
                                    && callActivityExecutionIds.contains(childExecution.getSuperExecutionId())) {
                        if (!childExecution.isDeleted()) {
                            // LOGGER.info("deleted {}", childExecution);
                            executionEntityManager.deleteProcessInstanceExecutionEntity(childExecution.getId(), activityId,
                                            "call activity completion condition met", true, false, true);
                        } else {
                            LOGGER.info("The child execution {} is already deleted", childExecution.getId());
                        }
                    }
                }

            }
        }
    }

    private void sendCompletedEvent(ExecutionEntity execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext())
                        .dispatchEvent(buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED));
    }

    protected FlowableMultiInstanceActivityCompletedEvent buildCompletedEvent(DelegateExecution execution, FlowableEngineEventType eventType) {
        FlowElement flowNode = getCurrentFlowElement((ExecutionEntity) execution);

        // verify the variables
        return FlowableEventBuilder.createMultiInstanceActivityCompletedEvent(eventType, (int) execution.getVariable(NUMBER_OF_INSTANCES),
                        (int) execution.getVariable(NUMBER_OF_ACTIVE_INSTANCES), (int) execution.getVariable(NUMBER_OF_COMPLETED_INSTANCES), flowNode.getId(),
                        flowNode.getName(), execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode);
    }

    private List<ExecutionEntity> determineCompletedInstances(ExecutionEntity miRootExecution) {
        List<ExecutionEntity> completedChildEntities = new ArrayList<>();
        for (ExecutionEntity childExecution : childExecutions) {
            int nrOfCompletedInstancesFromChild = getVariableOrDefault(childExecution, NUMBER_OF_COMPLETED_INSTANCES, 0);
            if (nrOfCompletedInstancesFromChild == 1) {
                completedChildEntities.add(childExecution);
            }
        }
        return completedChildEntities;
    }

    @SuppressWarnings("unchecked")
    private <T> T getVariableOrDefault(ExecutionEntity childExecution, String variableName, T defaultValue) {
        T valueFromDb = tryToFindInDb(childExecution, variableName);
        if (valueFromDb != null) {
            return valueFromDb;
        }
        Object value = childExecution.getVariableLocal(variableName);
        DelegateExecution parent = childExecution.getParent();
        while (value == null && parent != null) {
            value = parent.getVariableLocal(variableName);
            parent = parent.getParent();
        }
        return value == null ? defaultValue : (T) value;
    }

    @SuppressWarnings("unchecked")
    private <T> T tryToFindInDb(ExecutionEntity childExecution, String variableName) {
        VariableInstanceEntity variableInstace = CommandContextUtil.getVariableService().findVariableInstanceByExecutionAndName(childExecution.getId(),
                        variableName);
        return variableInstace == null ? null : (T) variableInstace.getValue();
    }

    protected void executeAsynchronousJob() {
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        JobEntity jobForExecution = getJobEntity(jobService);
        jobService.createAsyncJob(jobForExecution, false);
        jobService.scheduleAsyncJob(jobForExecution);
    }

    private JobEntity getJobEntity(JobService jobService) {
        JobEntity job = jobService.createJob();
        job.setExecutionId(execution.getId());
        job.setProcessInstanceId(execution.getProcessInstanceId());
        job.setProcessDefinitionId(execution.getProcessDefinitionId());
        job.setJobHandlerType(AsyncParallelMultiInstanceMonitorJobHandler.TYPE);
        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            job.setTenantId(execution.getTenantId());
        }

        execution.getJobs().add(job);
        return job;
    }
}
