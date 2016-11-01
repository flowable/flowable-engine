package org.activiti.rest.dmn.exception;

import org.activiti.engine.ActivitiException;

public class ActivitiDmnConflictException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  public ActivitiDmnConflictException(String message) {
    super(message);
  }
}
