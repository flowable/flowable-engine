package org.flowable.engine.impl.jobexecutor;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

public class AsyncParallelMultiInstanceMonitorJobHandler implements JobHandler {

    public static final String TYPE = "async-parallel-multi-instance-monitor";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;
        CommandContextUtil.getAgenda(commandContext).planMonitorParallelMultiInstanceOperation(executionEntity, executionEntity.getExecutions());
    }

}
