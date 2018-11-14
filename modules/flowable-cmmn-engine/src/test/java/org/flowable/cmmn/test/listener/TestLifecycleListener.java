package org.flowable.cmmn.test.listener;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;

public class TestLifecycleListener implements PlanItemInstanceLifecycleListener {

    @Override
    public String getSourceState() {
        return null;
    }

    @Override
    public String getTargetState() {
        return null;
    }

    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        planItemInstance.setVariable("classDelegateVariable", "Hello World");
    }

}
