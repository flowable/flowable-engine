package org.activiti.engine.delegate.event;

import org.activiti.engine.impl.delegate.event.ActivitiEngineEvent;

/**
 * An {@link org.activiti.engine.common.api.delegate.event.ActivitiEvent} related to cancel event being sent when activiti object is cancelled.
 * 
 * @author martin.grofcik
 */
public interface ActivitiCancelledEvent extends ActivitiEngineEvent {
  /**
   * @return the cause of the cancel event. Returns null, if no specific cause has been specified.
   */
  public Object getCause();
}
