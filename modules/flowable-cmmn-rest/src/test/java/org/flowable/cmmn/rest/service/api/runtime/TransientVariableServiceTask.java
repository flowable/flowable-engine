package org.flowable.cmmn.rest.service.api.runtime;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;

/**
 * Processes the transient variable and puts the relevant bits in real variables
 */
public class TransientVariableServiceTask implements PlanItemJavaDelegate {
    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        planItemInstance.getTransientVariables().forEach((s, o) -> planItemInstance.setVariable(s, o.toString()));
    }
}
