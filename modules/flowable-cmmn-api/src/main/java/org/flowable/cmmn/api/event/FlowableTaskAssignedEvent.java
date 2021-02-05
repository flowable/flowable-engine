package org.flowable.cmmn.api.event;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.task.api.Task;

/**
 * @author David Lamas
 */
public interface FlowableTaskAssignedEvent extends FlowableEngineEntityEvent {

    @Override
    Task getEntity();
}
