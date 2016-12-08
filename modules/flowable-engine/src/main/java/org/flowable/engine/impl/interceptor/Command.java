package org.flowable.engine.impl.interceptor;

import org.flowable.engine.common.impl.interceptor.BaseCommand;

public interface Command<T> extends BaseCommand<T, CommandContext> {

  T execute(CommandContext commandContext);
}
