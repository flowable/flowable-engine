package org.activiti.engine.impl.agenda;

import org.activiti.engine.FlowableEngineAgenda;
import org.activiti.engine.FlowableEngineAgendaFactory;
import org.activiti.engine.impl.interceptor.CommandContext;

public class DefaultFlowableEngineAgendaFactory implements FlowableEngineAgendaFactory {
  
  public FlowableEngineAgenda createAgenda(CommandContext commandContext) {
    return new DefaultFlowableEngineAgenda(commandContext);
  }
}
