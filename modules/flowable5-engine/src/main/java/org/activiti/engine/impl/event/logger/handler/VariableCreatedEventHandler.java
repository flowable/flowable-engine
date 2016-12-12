package org.activiti.engine.impl.event.logger.handler;

import java.util.Map;

import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.delegate.event.FlowableVariableEvent;

/**
 * @author Joram Barrez
 */
public class VariableCreatedEventHandler extends VariableEventHandler {
	
	@Override
	public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
		FlowableVariableEvent variableEvent = (FlowableVariableEvent) event;
		Map<String, Object> data = createData(variableEvent);
		
		data.put(Fields.CREATE_TIME, timeStamp);
		
    return createEventLogEntry(variableEvent.getProcessDefinitionId(), variableEvent.getProcessInstanceId(), 
    		variableEvent.getExecutionId(), variableEvent.getTaskId(), data);
	}

}
