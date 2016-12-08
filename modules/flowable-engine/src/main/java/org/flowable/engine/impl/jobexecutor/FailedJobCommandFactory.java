package org.flowable.engine.impl.jobexecutor;

import org.flowable.engine.impl.interceptor.Command;

public interface FailedJobCommandFactory {

  public Command<Object> getCommand(String jobId, Throwable exception);

}
