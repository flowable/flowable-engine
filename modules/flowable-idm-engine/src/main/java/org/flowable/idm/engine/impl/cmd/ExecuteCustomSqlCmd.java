package org.flowable.idm.engine.impl.cmd;

import org.flowable.engine.common.impl.cmd.CustomSqlExecution;
import org.flowable.idm.engine.impl.interceptor.Command;
import org.flowable.idm.engine.impl.interceptor.CommandContext;

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
        Mapper mapper = commandContext.getDbSqlSession().getSqlSession().getMapper(mapperClass);
        return customSqlExecution.execute(mapper);
    }

}
