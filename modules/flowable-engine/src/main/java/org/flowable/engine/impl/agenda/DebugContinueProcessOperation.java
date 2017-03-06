package org.flowable.engine.impl.agenda;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityManager;

/**
 * This class extends {@link ContinueProcessOperation} with the possibility to check whether execution is trying to
 * execute a breakpoint
 *
 * @author martin.grofcik
 */
public class DebugContinueProcessOperation extends ContinueProcessOperation {

    protected ProcessDebugger debugger;

    public DebugContinueProcessOperation(ProcessDebugger debugger, CommandContext commandContext,
                                         ExecutionEntity execution, boolean forceSynchronousOperation,
                                         boolean inCompensation) {
        super(commandContext, execution, forceSynchronousOperation, inCompensation);
        this.debugger = debugger;
    }

    public DebugContinueProcessOperation(ProcessDebugger debugger, CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
        this.debugger = debugger;
    }

    protected void continueThroughFlowNode(FlowNode flowNode) {
        if (debugger.isBreakPoint(execution)) {
            breakExecution(flowNode);
        } else {
            super.continueThroughFlowNode(flowNode);
        }
    }

    private void breakExecution(FlowNode flowNode) {
        JobEntity brokenExecutionJob = getJobEntityManager().create();
        brokenExecutionJob.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        brokenExecutionJob.setRevision(1);
        brokenExecutionJob.setExecutionId(execution.getId());
        brokenExecutionJob.setProcessInstanceId(execution.getProcessInstanceId());
        brokenExecutionJob.setProcessDefinitionId(execution.getProcessDefinitionId());
        brokenExecutionJob.setExclusive(false);
        brokenExecutionJob.setJobHandlerType("breakPoint");

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            brokenExecutionJob.setTenantId(execution.getTenantId());
        }

        this.commandContext.getJobManager().scheduleAsyncJob(brokenExecutionJob);
    }

    private JobEntityManager getJobEntityManager() {
        return this.commandContext.getJobEntityManager();
    }

}
