package org.flowable.dmn.engine.impl.cmd;

import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * @author jbarrez
 */
public class ExecuteCustomSqlCmd<Mapper, ResultType> implements Command<ResultType> {

    protected Class<Mapper> mapperClass;
    protected CustomSqlExecution<Mapper, ResultType> customSqlExecution;

    public ExecuteCustomSqlCmd(Class<Mapper> mapperClass, CustomSqlExecution<Mapper, ResultType> customSqlExecution) {
        this.mapperClass = mapperClass;
        this.customSqlExecution = customSqlExecution;
    }

    @Override
    public ResultType execute(CommandContext commandContext) {
        Mapper mapper = CommandContextUtil.getDbSqlSession(commandContext).getSqlSession().getMapper(mapperClass);
        return customSqlExecution.execute(mapper);
    }

}
