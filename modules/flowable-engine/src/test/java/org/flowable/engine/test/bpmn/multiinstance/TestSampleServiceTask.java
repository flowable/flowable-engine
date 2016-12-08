package org.flowable.engine.test.bpmn.multiinstance;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;

/**
 * @author Andreas Karnahl
 */
public class TestSampleServiceTask extends AbstractBpmnActivityBehavior {
  
  private static final long serialVersionUID = 1L;

  @Override
  public void execute(DelegateExecution execution) {
    System.out.println("###: execution: " + execution.getId() + "; " + execution.getVariable("value") + "; " + getMultiInstanceActivityBehavior());
    leave(execution);
  }
}
