package org.activiti.engine.test.db;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.context.Context;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class GetVariableLocalTask implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        RuntimeService runtimeService = Context.getProcessEngineConfiguration().getRuntimeService();
        runtimeService.getVariableLocal(execution.getProcessInstanceId(), "Variable-That-Does-Not-Exist");
    }
}