package org.activiti.engine.impl.interceptor;

import org.activiti.engine.common.impl.interceptor.BaseCommand;

public interface Command<T> extends BaseCommand<T, CommandContext> {

  T execute(CommandContext commandContext);
}
