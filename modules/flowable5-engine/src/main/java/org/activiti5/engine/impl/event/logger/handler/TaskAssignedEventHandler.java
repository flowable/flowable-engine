package org.activiti5.engine.impl.event.logger.handler;

import java.util.Map;

import org.activiti5.engine.impl.interceptor.CommandContext;
import org.activiti5.engine.impl.persistence.entity.EventLogEntryEntity;
import org.activiti5.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;

/**
 * @author Joram Barrez
 */
public class TaskAssignedEventHandler extends AbstractTaskEventHandler {
	
	@Override
	public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
		TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
		Map<String, Object> data = handleCommonTaskFields(task);
    return createEventLogEntry(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getExecutionId(), task.getId(), data);
	}

}
