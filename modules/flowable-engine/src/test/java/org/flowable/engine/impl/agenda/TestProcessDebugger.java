package org.flowable.engine.impl.agenda;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * This class evaluates each execution as a breakpoint
 */
public class TestProcessDebugger implements ProcessDebugger {

    @Override
    public boolean isBreakPoint(ExecutionEntity execution) {
        return true;
    }

}
