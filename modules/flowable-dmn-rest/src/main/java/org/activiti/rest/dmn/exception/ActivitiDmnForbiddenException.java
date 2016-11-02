package org.activiti.rest.dmn.exception;

import org.activiti.engine.ActivitiException;

public class ActivitiDmnForbiddenException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  public ActivitiDmnForbiddenException(String message) {
    super(message);
  }
}
