package org.activiti.rest.exception;

import org.activiti.engine.common.api.ActivitiException;

public class ActivitiConflictException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  public ActivitiConflictException(String message) {
    super(message);
  }
}
