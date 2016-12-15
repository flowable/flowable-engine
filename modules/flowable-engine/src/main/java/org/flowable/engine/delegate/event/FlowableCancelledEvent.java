package org.flowable.engine.delegate.event;

import org.flowable.engine.impl.delegate.event.FlowableEngineEvent;

/**
 * @author martin.grofcik
 */
public interface FlowableCancelledEvent extends FlowableEngineEvent {
  /**
   * @return the cause of the cancel event. Returns null, if no specific cause has been specified.
   */
  public Object getCause();
}
