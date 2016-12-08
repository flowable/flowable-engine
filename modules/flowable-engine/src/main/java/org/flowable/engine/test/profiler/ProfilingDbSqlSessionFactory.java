package org.flowable.engine.test.profiler;

import org.flowable.engine.common.impl.interceptor.AbstractCommandContext;
import org.flowable.engine.common.impl.interceptor.Session;
import org.flowable.engine.impl.db.DbSqlSessionFactory;
import org.flowable.engine.impl.interceptor.CommandContext;

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
