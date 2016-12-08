package org.flowable.engine.impl.event.logger.handler;

import java.util.Map;

import org.flowable.engine.delegate.event.FlowableVariableEvent;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;

/**
 * @author Joram Barrez
 */
public class VariableUpdatedEventHandler extends VariableEventHandler {

  @Override
  public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
    FlowableVariableEvent variableEvent = (FlowableVariableEvent) event;
    Map<String, Object> data = createData(variableEvent);

    return createEventLogEntry(variableEvent.getProcessDefinitionId(), variableEvent.getProcessInstanceId(), variableEvent.getExecutionId(), variableEvent.getTaskId(), data);
  }

}
