/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.flowable.engine.ManagementService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.management.TableMetaData;
import org.flowable.engine.common.api.management.TablePageQuery;
import org.flowable.engine.common.impl.cmd.CustomSqlExecution;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.db.DbSqlSessionFactory;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandConfig;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.cmd.DeleteDeadLetterJobCmd;
import org.flowable.engine.impl.cmd.DeleteEventLogEntry;
import org.flowable.engine.impl.cmd.DeleteHistoryJobCmd;
import org.flowable.engine.impl.cmd.DeleteJobCmd;
import org.flowable.engine.impl.cmd.DeleteSuspendedJobCmd;
import org.flowable.engine.impl.cmd.DeleteTimerJobCmd;
import org.flowable.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.flowable.engine.impl.cmd.ExecuteJobCmd;
import org.flowable.engine.impl.cmd.GetEventLogEntriesCmd;
import org.flowable.engine.impl.cmd.GetJobExceptionStacktraceCmd;
import org.flowable.engine.impl.cmd.GetPropertiesCmd;
import org.flowable.engine.impl.cmd.GetTableCountCmd;
import org.flowable.engine.impl.cmd.GetTableMetaDataCmd;
import org.flowable.engine.impl.cmd.GetTableNameCmd;
import org.flowable.engine.impl.cmd.JobType;
import org.flowable.engine.impl.cmd.MoveDeadLetterJobToExecutableJobCmd;
import org.flowable.engine.impl.cmd.MoveJobToDeadLetterJobCmd;
import org.flowable.engine.impl.cmd.MoveTimerToExecutableJobCmd;
import org.flowable.engine.impl.cmd.RescheduleTimerJobCmd;
import org.flowable.engine.impl.cmd.SetJobRetriesCmd;
import org.flowable.engine.impl.cmd.SetTimerJobRetriesCmd;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.DeadLetterJobQuery;
import org.flowable.engine.runtime.HistoryJobQuery;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.JobQuery;
import org.flowable.engine.runtime.SuspendedJobQuery;
import org.flowable.engine.runtime.TimerJobQuery;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Saeid Mizaei
 */
public class ManagementServiceImpl extends ServiceImpl implements ManagementService {

    public Map<String, Long> getTableCount() {
        return commandExecutor.execute(new GetTableCountCmd());
    }

    public String getTableName(Class<?> entityClass) {
        return commandExecutor.execute(new GetTableNameCmd(entityClass));
    }

    public TableMetaData getTableMetaData(String tableName) {
        return commandExecutor.execute(new GetTableMetaDataCmd(tableName));
    }

    public void executeJob(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("JobId is null");
        }

        try {
            commandExecutor.execute(new ExecuteJobCmd(jobId));

        } catch (RuntimeException e) {
            if (e instanceof FlowableException) {
                throw e;
            } else {
                throw new FlowableException("Job " + jobId + " failed", e);
            }
        }
    }

    public Job moveTimerToExecutableJob(String jobId) {
        return commandExecutor.execute(new MoveTimerToExecutableJobCmd(jobId));
    }

    public Job moveJobToDeadLetterJob(String jobId) {
        return commandExecutor.execute(new MoveJobToDeadLetterJobCmd(jobId));
    }

    @Override
    public Job moveDeadLetterJobToExecutableJob(String jobId, int retries) {
        return commandExecutor.execute(new MoveDeadLetterJobToExecutableJobCmd(jobId, retries));
    }

    public void deleteJob(String jobId) {
        commandExecutor.execute(new DeleteJobCmd(jobId));
    }

    public void deleteTimerJob(String jobId) {
        commandExecutor.execute(new DeleteTimerJobCmd(jobId));
    }
    
    public void deleteSuspendedJob(String jobId) {
        commandExecutor.execute(new DeleteSuspendedJobCmd(jobId));
    }

    public void deleteDeadLetterJob(String jobId) {
        commandExecutor.execute(new DeleteDeadLetterJobCmd(jobId));
    }
    
    public void deleteHistoryJob(String jobId) {
        commandExecutor.execute(new DeleteHistoryJobCmd(jobId));
    }

    public void setJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetJobRetriesCmd(jobId, retries));
    }

    public void setTimerJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetTimerJobRetriesCmd(jobId, retries));
    }

    @Override
    public Job rescheduleTimeDateJob(String jobId, String timeDate) {
        return commandExecutor.execute(new RescheduleTimerJobCmd(jobId, timeDate, null, null, null, null));
    }

    @Override
    public Job rescheduleTimeDurationJob(String jobId, String timeDuration) {
        return commandExecutor.execute(new RescheduleTimerJobCmd(jobId, null, timeDuration, null, null, null));
    }

    @Override
    public Job rescheduleTimeCycleJob(String jobId, String timeCycle) {
        return commandExecutor.execute(new RescheduleTimerJobCmd(jobId, null, null, timeCycle, null, null));
    }

    @Override
    public Job rescheduleTimerJob(String jobId, String timeDate, String timeDuration, String timeCycle, String endDate, String calendarName) {
        return commandExecutor.execute(new RescheduleTimerJobCmd(jobId, timeDate, timeDuration, timeCycle, endDate, calendarName));
    }

    public TablePageQuery createTablePageQuery() {
        return new TablePageQueryImpl(commandExecutor);
    }

    public JobQuery createJobQuery() {
        return new JobQueryImpl(commandExecutor);
    }

    public TimerJobQuery createTimerJobQuery() {
        return new TimerJobQueryImpl(commandExecutor);
    }

    public SuspendedJobQuery createSuspendedJobQuery() {
        return new SuspendedJobQueryImpl(commandExecutor);
    }

    public DeadLetterJobQuery createDeadLetterJobQuery() {
        return new DeadLetterJobQueryImpl(commandExecutor);
    }
    
    public HistoryJobQuery createHistoryJobQuery() {
        return new HistoryJobQueryImpl(commandExecutor);
    }

    public String getJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.ASYNC));
    }

    public String getTimerJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.TIMER));
    }

    public String getSuspendedJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.SUSPENDED));
    }

    public String getDeadLetterJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.DEADLETTER));
    }

    public Map<String, String> getProperties() {
        return commandExecutor.execute(new GetPropertiesCmd());
    }

    public String databaseSchemaUpgrade(final Connection connection, final String catalog, final String schema) {
        CommandConfig config = commandExecutor.getDefaultConfig().transactionNotSupported();
        return commandExecutor.execute(config, new Command<String>() {
            public String execute(CommandContext commandContext) {
                DbSqlSessionFactory dbSqlSessionFactory = (DbSqlSessionFactory) commandContext.getSessionFactories().get(DbSqlSession.class);
                DbSqlSession dbSqlSession = new DbSqlSession(dbSqlSessionFactory, CommandContextUtil.getEntityCache(commandContext), connection, catalog, schema);
                commandContext.getSessions().put(DbSqlSession.class, dbSqlSession);
                return CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager().dbSchemaUpdate();
            }
        });
    }

    public <T> T executeCommand(Command<T> command) {
        if (command == null) {
            throw new FlowableIllegalArgumentException("The command is null");
        }
        return commandExecutor.execute(command);
    }

    public <T> T executeCommand(CommandConfig config, Command<T> command) {
        if (config == null) {
            throw new FlowableIllegalArgumentException("The config is null");
        }
        if (command == null) {
            throw new FlowableIllegalArgumentException("The command is null");
        }
        return commandExecutor.execute(config, command);
    }

    @Override
    public <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution) {
        Class<MapperType> mapperClass = customSqlExecution.getMapperClass();
        return commandExecutor.execute(new ExecuteCustomSqlCmd<>(mapperClass, customSqlExecution));
    }

    @Override
    public List<EventLogEntry> getEventLogEntries(Long startLogNr, Long pageSize) {
        return commandExecutor.execute(new GetEventLogEntriesCmd(startLogNr, pageSize));
    }

    @Override
    public List<EventLogEntry> getEventLogEntriesByProcessInstanceId(String processInstanceId) {
        return commandExecutor.execute(new GetEventLogEntriesCmd(processInstanceId));
    }

    @Override
    public void deleteEventLogEntry(long logNr) {
        commandExecutor.execute(new DeleteEventLogEntry(logNr));
    }

}
