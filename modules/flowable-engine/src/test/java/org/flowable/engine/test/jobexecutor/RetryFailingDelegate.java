package org.flowable.engine.test.jobexecutor;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class RetryFailingDelegate implements JavaDelegate {

  public static final String EXCEPTION_MESSAGE = "Expected exception.";

  public static boolean shallThrow;
  public static List<Long> times;

  static public void resetTimeList() {
    times = new ArrayList<Long>();
  }

  @Override
  public void execute(DelegateExecution execution) {

    times.add(System.currentTimeMillis());

    if (shallThrow) {
      throw new FlowableException(EXCEPTION_MESSAGE);
    }
  }
}