package org.flowable.engine.test.db;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;

public class IdGeneratorDataSourceDoNothing implements ActivityBehavior {

  private static final long serialVersionUID = 1L;

  public void execute(DelegateExecution execution) {
  }

}
