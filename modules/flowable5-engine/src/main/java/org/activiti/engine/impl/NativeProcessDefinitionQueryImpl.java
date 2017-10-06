package org.activiti.engine.impl;

import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.repository.NativeProcessDefinitionQuery;
import org.flowable.engine.repository.ProcessDefinition;

public class NativeProcessDefinitionQueryImpl extends AbstractNativeQuery<NativeProcessDefinitionQuery, ProcessDefinition> implements NativeProcessDefinitionQuery {

    private static final long serialVersionUID = 1L;

    public NativeProcessDefinitionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeProcessDefinitionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<ProcessDefinition> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
        return commandContext
                .getProcessDefinitionEntityManager()
                .findProcessDefinitionsByNativeQuery(parameterMap, firstResult, maxResults);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return commandContext
                .getProcessDefinitionEntityManager()
                .findProcessDefinitionCountByNativeQuery(parameterMap);
    }

}
