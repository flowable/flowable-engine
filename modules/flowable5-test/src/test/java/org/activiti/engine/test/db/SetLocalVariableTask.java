package org.activiti.engine.test.db;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class SetLocalVariableTask implements JavaDelegate {

    public void execute(DelegateExecution execution) {
        execution.setVariableLocal("test", "test2");
    }

}
