package org.activiti.idm.engine.impl;

import java.util.List;
import java.util.Map;

import org.activiti.idm.api.NativeUserQuery;
import org.activiti.idm.api.User;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;

public class NativeUserQueryImpl extends AbstractNativeQuery<NativeUserQuery, User> implements NativeUserQuery {

  private static final long serialVersionUID = 1L;

  public NativeUserQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeUserQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // results ////////////////////////////////////////////////////////////////

  public List<User> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return commandContext.getUserEntityManager().findUsersByNativeQuery(parameterMap, firstResult, maxResults);
  }

  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext.getUserEntityManager().findUserCountByNativeQuery(parameterMap);
  }

}