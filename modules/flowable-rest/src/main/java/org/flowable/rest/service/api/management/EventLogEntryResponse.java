package org.flowable.rest.service.api.management;

import org.flowable.engine.event.EventLogEntry;

import java.util.Date;

/**
 * Response for {@link EventLogBaseResource}
 *
 * @author martin.grofcik
 */
public class EventLogEntryResponse implements  EventLogEntry {
    protected final EventLogEntry eventLogEntry;

    public EventLogEntryResponse(EventLogEntry eventLogEntry) {
        this.eventLogEntry = eventLogEntry;
    }

    @Override
    public long getLogNumber() {
        return eventLogEntry.getLogNumber();
    }

    @Override
    public String getType() {
        return eventLogEntry.getType();
    }

    @Override
    public String getProcessDefinitionId() {
        return eventLogEntry.getProcessDefinitionId();
    }

    @Override
    public String getProcessInstanceId() {
        return eventLogEntry.getProcessInstanceId();
    }

    @Override
    public String getExecutionId() {
        return eventLogEntry.getExecutionId();
    }

    @Override
    public String getTaskId() {
        return eventLogEntry.getTaskId();
    }

    @Override
    public Date getTimeStamp() {
        return eventLogEntry.getTimeStamp();
    }

    @Override
    public String getUserId() {
        return eventLogEntry.getUserId();
    }

    @Override
    public byte[] getData() {
        return eventLogEntry.getData();
    }
}
