package org.flowable.engine.delegate.event.impl;

import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableJobRescheduledEvent;
import org.flowable.engine.runtime.Job;

public class FlowableJobRescheduledEventImpl extends FlowableEntityEventImpl implements FlowableJobRescheduledEvent {

    /**
     * The id of the original job that was rescheduled.
     */
    protected String rescheduledJobId;

    public FlowableJobRescheduledEventImpl(Job entity, String rescheduledJobId, FlowableEngineEventType type) {
        super(entity, type);
        this.rescheduledJobId = rescheduledJobId;
    }

    public String getRescheduledJobId() {
        return rescheduledJobId;
    }
}
