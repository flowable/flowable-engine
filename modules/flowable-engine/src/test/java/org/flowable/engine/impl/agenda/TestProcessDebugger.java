package org.flowable.engine.impl.agenda;

import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessDebugger;

/**
 * This class evaluates each execution as a breakpoint
 */
public class TestProcessDebugger implements ProcessDebugger {

    @Override
    public boolean isBreakpoint(Execution execution) {
        return true;
    }

}
