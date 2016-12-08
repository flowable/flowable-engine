package org.flowable.rest.exception;

import org.flowable.engine.common.api.FlowableException;

public class ActivitiContentNotSupportedException extends FlowableException {

  private static final long serialVersionUID = 1L;

  public ActivitiContentNotSupportedException(String message) {
    super(message);
  }
}
