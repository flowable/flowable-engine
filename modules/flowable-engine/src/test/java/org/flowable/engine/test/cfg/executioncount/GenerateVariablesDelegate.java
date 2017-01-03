package org.flowable.engine.test.cfg.executioncount;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;

public class GenerateVariablesDelegate implements JavaDelegate {

    private Expression numberOfVariablesString;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        int numberOfVariables = Integer.valueOf(numberOfVariablesString.getValue(delegateExecution).toString());
        for (int i=0; i<numberOfVariables; i++) {
            if (i%2 == 0) {
                delegateExecution.setVariable("var" + i, i); // integer
            } else {
                delegateExecution.setVariable("var" + i, String.valueOf(i)); // string
            }
        }
    }

}
