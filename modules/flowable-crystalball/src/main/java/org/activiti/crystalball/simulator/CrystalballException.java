package org.activiti.crystalball.simulator;

import org.flowable.engine.common.api.FlowableException;

/**
 * @author martin.grofcik
 */
public class CrystalballException extends FlowableException {

  private static final long serialVersionUID = 1L;

  public CrystalballException(String msg) {
    super(msg);
  }

  public CrystalballException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
