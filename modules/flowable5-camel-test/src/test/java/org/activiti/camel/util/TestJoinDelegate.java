package org.activiti.camel.util;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class TestJoinDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    // dummy task
  }

}
