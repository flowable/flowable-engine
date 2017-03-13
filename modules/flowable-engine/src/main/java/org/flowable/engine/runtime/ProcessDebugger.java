package org.flowable.engine.runtime;

/**
 * @author martin.grofcik
 */
public interface ProcessDebugger {

    /**
     * Indicates that execution is in the breakpoint state
     *
     * @param execution execution to evaluate
     *
     * @return true in the case when breakpoint was reached in the execution, false in the case when not
     * @throws RuntimeException in the case when it was not possible to decide
     */
    boolean isBreakPoint(Execution execution);

}
