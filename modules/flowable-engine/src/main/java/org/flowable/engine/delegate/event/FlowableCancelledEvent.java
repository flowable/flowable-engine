package org.flowable.engine.delegate.event;

import org.flowable.engine.impl.delegate.event.ActivitiEngineEvent;

/**
 * An {@link org.flowable.engine.common.api.delegate.event.FlowableEvent} related to cancel event being sent when activiti object is cancelled.
 * 
 * @author martin.grofcik
 */
public interface FlowableCancelledEvent extends ActivitiEngineEvent {
  /**
   * @return the cause of the cancel event. Returns null, if no specific cause has been specified.
   */
  public Object getCause();
}
