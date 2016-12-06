package org.activiti5.engine.test.db;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;


public class IdGeneratorDataSourceDoNothing implements ActivityBehavior {

  public void execute(DelegateExecution execution) {
  }

}
