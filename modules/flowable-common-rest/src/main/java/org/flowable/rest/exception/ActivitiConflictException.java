package org.flowable.rest.exception;

import org.flowable.engine.common.api.FlowableException;

public class ActivitiConflictException extends FlowableException {

  private static final long serialVersionUID = 1L;

  public ActivitiConflictException(String message) {
    super(message);
  }
}
