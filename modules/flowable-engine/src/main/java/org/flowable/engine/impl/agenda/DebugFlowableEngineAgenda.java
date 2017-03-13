package org.flowable.engine.impl.agenda;

import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessDebugger;

/**
 * This {@link org.flowable.engine.FlowableEngineAgenda} schedules operations which allow debugging
 */
public class DebugFlowableEngineAgenda extends DefaultFlowableEngineAgenda {

    protected ProcessDebugger processDebugger;

    public DebugFlowableEngineAgenda(CommandContext commandContext, ProcessDebugger processDebugger) {
        super(commandContext);
        this.processDebugger = processDebugger;
    }

    public void planContinueProcessOperation(ExecutionEntity execution) {
        planOperation(new DebugContinueProcessOperation(processDebugger, commandContext, execution), execution);
    }

    public void planContinueProcessSynchronousOperation(ExecutionEntity execution) {
        planOperation(new DebugContinueProcessOperation(processDebugger, commandContext, execution, true, false), execution);
    }

    public void planContinueProcessInCompensation(ExecutionEntity execution) {
        planOperation(new DebugContinueProcessOperation(processDebugger, commandContext, execution, false, true), execution);
    }

}
