package org.activiti.idm.engine.impl;

import java.util.List;
import java.util.Map;

import org.activiti.idm.api.Group;
import org.activiti.idm.api.NativeGroupQuery;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;

public class NativeGroupQueryImpl extends AbstractNativeQuery<NativeGroupQuery, Group> implements NativeGroupQuery {

  private static final long serialVersionUID = 1L;

  public NativeGroupQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeGroupQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // results ////////////////////////////////////////////////////////////////

  public List<Group> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return commandContext.getGroupEntityManager().findGroupsByNativeQuery(parameterMap, firstResult, maxResults);
  }

  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext.getGroupEntityManager().findGroupCountByNativeQuery(parameterMap);
  }

}