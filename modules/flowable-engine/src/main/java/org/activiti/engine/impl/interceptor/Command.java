package org.activiti.engine.impl.interceptor;

public interface Command<T> extends BaseCommand<T, CommandContext> {

  T execute(CommandContext commandContext);
}
