package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.bpmn.model.Activity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author martin.grofcik
 */
public class RuntimeMultiInstanceActivityBehavior extends MultiInstanceActivityBehavior {

    public RuntimeMultiInstanceActivityBehavior(Activity activity,
                                                AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
    }

    @Override
    public void execute(DelegateExecution delegateExecution) {
        getInternalBehavior(delegateExecution).execute(delegateExecution);
    }

    @Override
    protected int createInstances(DelegateExecution execution) {
        return getInternalBehavior(execution).createInstances(execution);
    }

    @Override
    public int getLoopCounterValue(ExecutionEntity execution) {
        return getInternalBehavior(execution).getLoopCounterValue(execution);
    }

    @Override
    public void leave(DelegateExecution execution) {
        getInternalBehavior(execution).leave(execution);
    }

    @Override
    public void continueMultiInstance(DelegateExecution execution, int loopCounter, ExecutionEntity multiInstanceRootExecution) {
        getInternalBehavior(execution).continueMultiInstance(execution, loopCounter, multiInstanceRootExecution);
    }

    @Override
    public void configureAddedExecutions(ExecutionEntity miExecution, ExecutionEntity childExecution) {
        getInternalBehavior(miExecution).configureAddedExecutions(miExecution, childExecution);
    }

    protected MultiInstanceActivityBehavior getInternalBehavior(DelegateExecution delegateExecution) {
        MultiInstanceActivityBehavior multiInstanceBehavior;

        if (isSequential(delegateExecution)) {
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
        return multiInstanceBehavior;
    }

    protected boolean isSequential(DelegateExecution delegateExecution) {
        boolean isSequential;
        if (delegateExecution.hasVariableLocal("_isSequential")) {
            isSequential = delegateExecution.getVariableLocal("_isSequential", Boolean.class);
        } else {
            isSequential = evaluateIsSequential(delegateExecution);
            delegateExecution.setVariableLocal("_isSequential", isSequential);
        } return isSequential;
    }

    protected boolean evaluateIsSequential(DelegateExecution delegateExecution) {
        Object sequential = getSequential() != null ? getSequential().getValue(delegateExecution) : null;
        boolean isSequential;
        if (sequential == null) {
            isSequential = false;
        } else if (sequential instanceof String) {
            if ("true".equalsIgnoreCase((String) sequential) || "false".equalsIgnoreCase((String) sequential)) {
                isSequential = Boolean.valueOf((String) sequential);
            } else {
                throw new FlowableIllegalArgumentException("isSequential value ["+sequential+"] is not allowed.");
            }
        } else if (sequential instanceof Boolean) {
            isSequential = (boolean) sequential;
        } else {
            throw new FlowableException("unable to recognize sequential attribute " + sequential);
        }
        return isSequential;
    }

}
