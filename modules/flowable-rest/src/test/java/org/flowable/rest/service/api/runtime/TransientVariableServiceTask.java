package org.flowable.rest.service.api.runtime;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * Processes the transient variable and puts the relevant bits in real variables
 */
public class TransientVariableServiceTask implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        execution.getTransientVariables().forEach((s, o) -> execution.setVariable(s, o.toString()));
    }
}
