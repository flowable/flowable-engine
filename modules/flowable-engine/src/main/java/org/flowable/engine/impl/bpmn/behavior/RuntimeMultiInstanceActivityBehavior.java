package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.bpmn.model.Activity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 *
 * @author martin.grofcik
 */
public class RuntimeMultiInstanceActivityBehavior extends MultiInstanceActivityBehavior {

    protected MultiInstanceActivityBehavior multiInstanceBehavior;

    public RuntimeMultiInstanceActivityBehavior(Activity activity,
        AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
    }

    @Override
    public void execute(DelegateExecution delegateExecution) {
        multiInstanceBehavior = getInternalBehavior(delegateExecution);
        multiInstanceBehavior.execute(delegateExecution);
    }

    @Override
    protected int createInstances(DelegateExecution execution) {
        return multiInstanceBehavior.createInstances(execution);
    }

    @Override
    public int getLoopCounterValue(ExecutionEntity execution) {
        return multiInstanceBehavior.getLoopCounterValue(execution);
    }

    @Override
    public void leave(DelegateExecution execution) {
        multiInstanceBehavior.leave(execution);
    }

    @Override
    public void continueMultiInstance(DelegateExecution execution, int loopCounter, ExecutionEntity multiInstanceRootExecution) {
        multiInstanceBehavior.continueMultiInstance(execution, loopCounter, multiInstanceRootExecution);
    }

    @Override
    public void configureAddedExecutions(ExecutionEntity miExecution, ExecutionEntity childExecution) {
        multiInstanceBehavior.configureAddedExecutions(miExecution, childExecution);
    }

    protected MultiInstanceActivityBehavior getInternalBehavior(DelegateExecution delegateExecution) {
        if (multiInstanceBehavior == null) {
            Object sequential = getSequential() != null ? getSequential().getValue(delegateExecution) : null;
            boolean isSequential;
            if (sequential == null) {
                isSequential = false;
            } else if (sequential instanceof String) {
                isSequential = Boolean.valueOf((String) sequential);
            } else if (sequential instanceof Boolean) {
                isSequential = (boolean) sequential;
            } else {
                throw new FlowableException("unable to recognize sequential attribute " + sequential);
            }

            if (isSequential) {
                multiInstanceBehavior = new SequentialMultiInstanceBehavior(this.activity, this.innerActivityBehavior);
            } else {
                multiInstanceBehavior = new ParallelMultiInstanceBehavior(this.activity, this.innerActivityBehavior);
            }
            multiInstanceBehavior.loopCardinalityExpression = this.loopCardinalityExpression;
            multiInstanceBehavior.completionCondition = this.completionCondition;
            multiInstanceBehavior.collectionExpression = this.collectionExpression;
            multiInstanceBehavior.collectionVariable = this.collectionVariable;
            multiInstanceBehavior.collectionElementVariable = this.collectionElementVariable;
            multiInstanceBehavior.collectionString = this.collectionString;
            multiInstanceBehavior.collectionHandler = this.collectionHandler;
            multiInstanceBehavior.collectionElementIndexVariable = this.collectionElementIndexVariable;
        }
        return multiInstanceBehavior;
    }

}
