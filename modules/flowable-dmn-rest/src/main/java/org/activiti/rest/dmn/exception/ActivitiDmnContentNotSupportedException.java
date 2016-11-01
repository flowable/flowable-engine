package org.activiti.rest.dmn.exception;

import org.activiti.engine.ActivitiException;

public class ActivitiDmnContentNotSupportedException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  public ActivitiDmnContentNotSupportedException(String message) {
    super(message);
  }
}
