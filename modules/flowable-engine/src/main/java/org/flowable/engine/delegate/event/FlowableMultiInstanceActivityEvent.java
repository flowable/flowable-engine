package org.flowable.engine.delegate.event;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;

/**
 * A {@link FlowableEvent} related to a multi-instance activity within an execution.
 *
 * @author Robert Hafner
 */
public interface FlowableMultiInstanceActivityEvent extends FlowableActivityEvent {
    public boolean isSequential();
}
