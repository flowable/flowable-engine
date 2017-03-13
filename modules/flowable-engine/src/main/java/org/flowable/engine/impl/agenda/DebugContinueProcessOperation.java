package org.flowable.engine.impl.agenda;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.*;
import org.flowable.engine.runtime.ProcessDebugger;

/**
 * This class extends {@link ContinueProcessOperation} with the possibility to check whether execution is trying to
 * execute a breakpoint
 *
 * @author martin.grofcik
 */
public class DebugContinueProcessOperation extends ContinueProcessOperation {

    public static final String HANDLER_TYPE_BREAK_POINT = "breakPoint";
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

    protected void breakExecution(FlowNode flowNode) {
        SuspendedJobEntity brokenJob = getSuspendedJobEntityManager().create();
        brokenJob.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        brokenJob.setRevision(1);
        brokenJob.setRetries(0);
        brokenJob.setExecutionId(execution.getId());
        brokenJob.setProcessInstanceId(execution.getProcessInstanceId());
        brokenJob.setProcessDefinitionId(execution.getProcessDefinitionId());
        brokenJob.setExclusive(false);
        brokenJob.setJobHandlerType(HANDLER_TYPE_BREAK_POINT);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            brokenJob.setTenantId(execution.getTenantId());
        }

        getSuspendedJobEntityManager().insert(brokenJob);
    }

    protected SuspendedJobEntityManager getSuspendedJobEntityManager() {
        return this.commandContext.getSuspendedJobEntityManager();
    }

}
