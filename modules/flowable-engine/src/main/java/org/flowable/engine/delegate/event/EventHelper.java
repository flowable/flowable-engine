package org.flowable.engine.delegate.event;

import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.CommandContextUtil;

public class EventHelper {

    public static DelegateExecution getExecution(String executionId) {
        if (executionId != null) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext != null) {
                return CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
            }
        }
        return null;
    }

}
