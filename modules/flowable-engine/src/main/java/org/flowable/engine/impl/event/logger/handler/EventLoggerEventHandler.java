package org.flowable.engine.impl.event.logger.handler;

import java.util.Date;

import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public interface EventLoggerEventHandler {

  EventLogEntryEntity generateEventLogEntry(CommandContext commandContext);

  void setEvent(FlowableEvent event);

  void setTimeStamp(Date timeStamp);

  void setObjectMapper(ObjectMapper objectMapper);

}
