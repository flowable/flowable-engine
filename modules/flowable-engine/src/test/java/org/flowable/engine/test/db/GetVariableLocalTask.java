package org.flowable.engine.test.db;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;

public class GetVariableLocalTask implements JavaDelegate {
  @Override
  public void execute(DelegateExecution execution) {
    RuntimeService runtimeService = Context.getProcessEngineConfiguration().getRuntimeService();
    runtimeService.getVariableLocal(execution.getProcessInstanceId(), "Variable-That-Does-Not-Exist");
  }
}