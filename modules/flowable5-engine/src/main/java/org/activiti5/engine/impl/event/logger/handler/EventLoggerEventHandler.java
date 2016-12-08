package org.activiti5.engine.impl.event.logger.handler;

import java.util.Date;

import org.activiti5.engine.impl.interceptor.CommandContext;
import org.activiti5.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;

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
