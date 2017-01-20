package org.activiti.crystalball.simulator;

import org.activiti.engine.common.api.ActivitiException;

/**
 * @author martin.grofcik
 */
public class CrystalballException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  public CrystalballException(String msg) {
    super(msg);
  }

  public CrystalballException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
