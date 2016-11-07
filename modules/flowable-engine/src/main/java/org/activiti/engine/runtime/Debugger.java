package org.activiti.engine.runtime;


import org.activiti.engine.Agenda;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;

/**
 * This interface provides methods for the debugging
 *
 * @author martin.grofcik
 */
public interface Debugger {
    /**
     * evaluates whether given invocation is defined as a break point or not
     *
     * @param runnable the runnable to execute
     * @return true in the case when the execution should be stopped, false otherwise
     */
    boolean isBreakPoint(Runnable runnable);

    /**
     * Continue in the broken operation execution
     *
     * @param commandExecutor executor to run command with
     */
    void continueOperationExecution(CommandExecutor commandExecutor, Agenda agenda);

    /**
     * Break current operation execution
     *
     * @param commandContext command context for which execution is broken
     */
    void breakOperationExecution(CommandContext commandContext);
}
