package org.flowable.engine.impl.agenda;

import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.impl.interceptor.CommandContext;

public class DefaultFlowableEngineAgendaFactory implements FlowableEngineAgendaFactory {
  
  public FlowableEngineAgenda createAgenda(CommandContext commandContext) {
    return new DefaultFlowableEngineAgenda(commandContext);
  }
}
