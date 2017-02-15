package org.flowable.osgi.blueprint.bean;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

public class ActivityBehaviourBean implements ActivityBehavior {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("visitedActivityBehaviour", true);
        Context.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution, true);
    }
}
