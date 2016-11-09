package org.activiti.dmn.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.dmn.engine.impl.interceptor.Command;
import org.activiti.dmn.engine.impl.interceptor.CommandContext;
import org.activiti.engine.ActivitiIllegalArgumentException;

public class GetTableNameCmd implements Command<String>, Serializable {

  private static final long serialVersionUID = 1L;

  private Class<?> entityClass;

  public GetTableNameCmd(Class<?> entityClass) {
    this.entityClass = entityClass;
  }

  public String execute(CommandContext commandContext) {
    if (entityClass == null) {
      throw new ActivitiIllegalArgumentException("entityClass is null");
    }
    return commandContext.getTableDataManager().getTableName(entityClass, true);
  }

}
