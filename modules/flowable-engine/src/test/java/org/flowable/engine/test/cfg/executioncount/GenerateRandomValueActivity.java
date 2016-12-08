package org.flowable.engine.test.cfg.executioncount;

import java.util.Random;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;


public class GenerateRandomValueActivity implements JavaDelegate {
  
  private static final long serialVersionUID = 1L;
  
  private static Random random = new Random();

  public void execute(DelegateExecution execution) {
    Integer value = random.nextInt(10);
    execution.setVariable("var", value);
  }

}