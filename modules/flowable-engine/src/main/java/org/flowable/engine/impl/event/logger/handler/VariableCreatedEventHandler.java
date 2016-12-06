package org.flowable.engine.impl.event.logger.handler;

import java.util.Map;

import org.flowable.engine.delegate.event.FlowableVariableEvent;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;

/**
 * @author Joram Barrez
 */
public class VariableCreatedEventHandler extends VariableEventHandler {

  @Override
  public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
    FlowableVariableEvent variableEvent = (FlowableVariableEvent) event;
    Map<String, Object> data = createData(variableEvent);

    data.put(Fields.CREATE_TIME, timeStamp);

    return createEventLogEntry(variableEvent.getProcessDefinitionId(), variableEvent.getProcessInstanceId(), variableEvent.getExecutionId(), variableEvent.getTaskId(), data);
  }

}
