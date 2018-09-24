package org.flowable.common.engine.impl.interceptor;

import org.flowable.common.engine.impl.context.Context;

public class DefaultCommandInvoker extends AbstractCommandInterceptor {

    @Override
    public <T> T execute(final CommandConfig config, final Command<T> command) {
        final CommandContext commandContext = Context.getCommandContext();
        T result = command.execute(commandContext);
        return result;
    }

    @Override
    public CommandInterceptor getNext() {
        return null;
    }

    @Override
    public void setNext(CommandInterceptor next) {
        throw new UnsupportedOperationException("CommandInvoker must be the last interceptor in the chain");
    }

}