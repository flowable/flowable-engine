package org.flowable.app.events;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.event.logger.handler.AbstractDatabaseEventLoggerEventHandler;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;

import java.util.HashMap;

/**
 * Log {@link FlowableEngineEventType#TIMER_FIRED} to the database event log
 */
public class TimerFiredEventHandler extends AbstractDatabaseEventLoggerEventHandler {

    @Override
    public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
        JobEntity job = (JobEntity) ((FlowableEntityEvent) event).getEntity();
        return createEventLogEntry(job.getProcessDefinitionId(), job.getProcessInstanceId(), job.getExecutionId(), job.getId(), new HashMap());
    }

}
