package org.flowable.engine.impl.agenda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.MultiInstanceCompletionConditionEvaluator;
import org.flowable.engine.impl.bpmn.helper.MultiInstanceRootCleaner;
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

        execution.setVariable(NUMBER_OF_COMPLETED_INSTANCES, nrOfCompletedInstances);
        Collection<ExecutionEntity> inactiveExecutions = getInactiveExecutions(execution);

        int numberOfFailedInstances = findNumberOfFailedInstances(execution);

        boolean isCompletionConditionSatisfied = new MultiInstanceCompletionConditionEvaluator().evaluateCompletionCondition(execution);

        if (nrOfCompletedInstances + numberOfFailedInstances >= nrOfInstances || inactiveExecutions.size() - 1 >= nrOfInstances
                        || isCompletionConditionSatisfied) {
            if (numberOfFailedInstances > 0) {
                throw new RuntimeException(FAILING_THE_MONITOR);
            }
            new MultiInstanceRootCleaner(isCompletionConditionSatisfied).finishMultiInstanceRootExecution(execution);
        } else {
            executeAsynchronousJob();
        }

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
