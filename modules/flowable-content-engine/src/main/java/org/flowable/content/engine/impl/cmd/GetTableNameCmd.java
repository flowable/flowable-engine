package org.flowable.content.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.engine.impl.util.CommandContextUtil;

public class GetTableNameCmd implements Command<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private Class<?> entityClass;

    public GetTableNameCmd(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (entityClass == null) {
            throw new FlowableIllegalArgumentException("entityClass is null");
        }
        return CommandContextUtil.getTableDataManager(commandContext).getTableName(entityClass, true);
    }

}
