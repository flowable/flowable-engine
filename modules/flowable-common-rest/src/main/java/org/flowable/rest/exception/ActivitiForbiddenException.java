package org.flowable.rest.exception;

import org.flowable.engine.common.api.FlowableException;

public class ActivitiForbiddenException extends FlowableException {

  private static final long serialVersionUID = 1L;

  public ActivitiForbiddenException(String message) {
    super(message);
  }
}
