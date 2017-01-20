package org.activiti.engine.test.profiler;

import org.activiti.engine.common.impl.interceptor.AbstractCommandContext;
import org.activiti.engine.common.impl.interceptor.Session;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class ProfilingDbSqlSessionFactory extends DbSqlSessionFactory {

  @Override
  public Session openSession(AbstractCommandContext commandContext) {
    CommandContext currentCommandContext = (CommandContext) commandContext;
    return new ProfilingDbSqlSession(this, currentCommandContext.getEntityCache());
  }
}
