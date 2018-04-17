package org.flowable.engine.delegate.event;

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;

public interface FlowableJobRescheduledEvent extends FlowableEntityEvent {
    /**
     * @return the job id of the original job that was rescheduled
     */
    public String getRescheduledJobId();
}
