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

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchPartBuilder;
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.batch.api.BatchQuery;
import org.flowable.batch.service.BatchPartBuilderImpl;
import org.flowable.batch.service.impl.BatchBuilderImpl;
import org.flowable.batch.service.impl.BatchPartQueryImpl;
import org.flowable.batch.service.impl.BatchQueryImpl;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.management.TablePageQuery;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.common.engine.impl.cmd.GetPropertiesCmd;
import org.flowable.common.engine.impl.cmd.GetTableCountCmd;
import org.flowable.common.engine.impl.cmd.GetTableMetaDataCmd;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.lock.LockManagerImpl;
import org.flowable.common.engine.impl.persistence.entity.TablePageQueryImpl;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdBuilderImpl;
import org.flowable.engine.ManagementService;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeleteBatchCmd;
import org.flowable.engine.impl.cmd.DeleteEventLogEntry;
import org.flowable.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.flowable.engine.impl.cmd.FindBatchPartsByBatchIdCmd;
import org.flowable.engine.impl.cmd.FindBatchesBySearchKeyCmd;
import org.flowable.engine.impl.cmd.GetAllBatchesCmd;
import org.flowable.engine.impl.cmd.GetBatchDocumentCmd;
import org.flowable.engine.impl.cmd.GetBatchPartCmd;
import org.flowable.engine.impl.cmd.GetBatchPartDocumentCmd;
import org.flowable.engine.impl.cmd.GetEventLogEntriesCmd;
import org.flowable.engine.impl.cmd.GetTableNameCmd;
import org.flowable.engine.impl.cmd.HandleHistoryCleanupTimerJobCmd;
import org.flowable.engine.impl.cmd.RescheduleTimerJobCmd;
import org.flowable.engine.impl.externalworker.ExternalWorkerCompletionBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ExternalWorkerCompletionBuilder;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.ExternalWorkerJobAcquireBuilder;
import org.flowable.job.api.ExternalWorkerJobFailureBuilder;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobFailureBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.cmd.DeleteDeadLetterJobCmd;
import org.flowable.job.service.impl.cmd.DeleteExternalWorkerJobCmd;
import org.flowable.job.service.impl.cmd.DeleteHistoryJobCmd;
import org.flowable.job.service.impl.cmd.DeleteJobCmd;
import org.flowable.job.service.impl.cmd.DeleteSuspendedJobCmd;
import org.flowable.job.service.impl.cmd.DeleteTimerJobCmd;
import org.flowable.job.service.impl.cmd.ExecuteHistoryJobCmd;
import org.flowable.job.service.impl.cmd.ExecuteJobCmd;
import org.flowable.job.service.impl.cmd.GetHistoryJobAdvancedConfigurationCmd;
import org.flowable.job.service.impl.cmd.GetJobByCorrelationIdCmd;
import org.flowable.job.service.impl.cmd.GetJobExceptionStacktraceCmd;
import org.flowable.job.service.impl.cmd.JobType;
import org.flowable.job.service.impl.cmd.MoveDeadLetterJobToExecutableJobCmd;
import org.flowable.job.service.impl.cmd.MoveDeadLetterJobToHistoryJobCmd;
import org.flowable.job.service.impl.cmd.MoveJobToDeadLetterJobCmd;
import org.flowable.job.service.impl.cmd.MoveSuspendedJobToExecutableJobCmd;
import org.flowable.job.service.impl.cmd.MoveTimerToExecutableJobCmd;
import org.flowable.job.service.impl.cmd.SetJobRetriesCmd;
import org.flowable.job.service.impl.cmd.SetTimerJobRetriesCmd;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Saeid Mizaei
 */
public class ManagementServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements ManagementService {
    
    public ManagementServiceImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Map<String, Long> getTableCount() {
        return commandExecutor.execute(new GetTableCountCmd(configuration.getEngineCfgKey()));
    }

    @Override
    public String getTableName(Class<?> entityClass) {
        return commandExecutor.execute(new GetTableNameCmd(entityClass));
    }

    @Override
    public String getTableName(Class<?> entityClass, boolean includePrefix) {
        return commandExecutor.execute(new GetTableNameCmd(entityClass, includePrefix));
    }

    @Override
    public TableMetaData getTableMetaData(String tableName) {
        return commandExecutor.execute(new GetTableMetaDataCmd(tableName, configuration.getEngineCfgKey()));
    }

    @Override
    public void executeJob(String jobId) {
        try {
            commandExecutor.execute(new ExecuteJobCmd(jobId, configuration.getJobServiceConfiguration()));

        } catch (RuntimeException e) {
            if (e instanceof FlowableException) {
                throw e;
            } else {
                throw new FlowableException("Job " + jobId + " failed", e);
            }
        }
    }
    
    @Override
    public void executeHistoryJob(String historyJobId) {
        commandExecutor.execute(new ExecuteHistoryJobCmd(historyJobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public String getHistoryJobHistoryJson(String historyJobId) {
        return commandExecutor.execute(new GetHistoryJobAdvancedConfigurationCmd(historyJobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public Job moveTimerToExecutableJob(String jobId) {
        return commandExecutor.execute(new MoveTimerToExecutableJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public Job moveJobToDeadLetterJob(String jobId) {
        return commandExecutor.execute(new MoveJobToDeadLetterJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public Job moveDeadLetterJobToExecutableJob(String jobId, int retries) {
        return commandExecutor.execute(new MoveDeadLetterJobToExecutableJobCmd(jobId, retries, configuration.getJobServiceConfiguration()));
    }

    @Override
    public HistoryJob moveDeadLetterJobToHistoryJob(String jobId, int retries) {
        return commandExecutor.execute(new MoveDeadLetterJobToHistoryJobCmd(jobId, retries, configuration.getJobServiceConfiguration()));
    }

    @Override
    public Job moveSuspendedJobToExecutableJob(String jobId) {
        return commandExecutor.execute(new MoveSuspendedJobToExecutableJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void deleteJob(String jobId) {
        commandExecutor.execute(new DeleteJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void deleteTimerJob(String jobId) {
        commandExecutor.execute(new DeleteTimerJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }
    
    @Override
    public void deleteSuspendedJob(String jobId) {
        commandExecutor.execute(new DeleteSuspendedJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void deleteDeadLetterJob(String jobId) {
        commandExecutor.execute(new DeleteDeadLetterJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void deleteExternalWorkerJob(String jobId) {
        commandExecutor.execute(new DeleteExternalWorkerJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }
    
    @Override
    public void deleteHistoryJob(String jobId) {
        commandExecutor.execute(new DeleteHistoryJobCmd(jobId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void setJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetJobRetriesCmd(jobId, retries, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void setTimerJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetTimerJobRetriesCmd(jobId, retries, configuration.getJobServiceConfiguration()));
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

    @Override
    public TablePageQuery createTablePageQuery() {
        return new TablePageQueryImpl(commandExecutor, configuration);
    }

    @Override
    public JobQuery createJobQuery() {
        return new JobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public ExternalWorkerJobQuery createExternalWorkerJobQuery() {
        return new ExternalWorkerJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public TimerJobQuery createTimerJobQuery() {
        return new TimerJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public SuspendedJobQuery createSuspendedJobQuery() {
        return new SuspendedJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public DeadLetterJobQuery createDeadLetterJobQuery() {
        return new DeadLetterJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }
    
    @Override
    public HistoryJobQuery createHistoryJobQuery() {
        return new HistoryJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public Job findJobByCorrelationId(String jobCorrelationId) {
        return commandExecutor.execute(new GetJobByCorrelationIdCmd(jobCorrelationId, configuration.getJobServiceConfiguration()));
    }

    @Override
    public String getJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.ASYNC, configuration.getJobServiceConfiguration()));
    }

    @Override
    public String getTimerJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.TIMER, configuration.getJobServiceConfiguration()));
    }

    @Override
    public String getSuspendedJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.SUSPENDED, configuration.getJobServiceConfiguration()));
    }

    @Override
    public String getDeadLetterJobExceptionStacktrace(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.DEADLETTER, configuration.getJobServiceConfiguration()));
    }
    
    @Override
    public String getExternalWorkerJobErrorDetails(String jobId) {
        return commandExecutor.execute(new GetJobExceptionStacktraceCmd(jobId, JobType.EXTERNAL_WORKER, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void handleHistoryCleanupTimerJob() {
        commandExecutor.execute(new HandleHistoryCleanupTimerJobCmd());
    }
    
    @Override
    public List<Batch> getAllBatches() {
        return commandExecutor.execute(new GetAllBatchesCmd());
    }
    
    @Override
    public List<Batch> findBatchesBySearchKey(String searchKey) {
        return commandExecutor.execute(new FindBatchesBySearchKeyCmd(searchKey));
    }
    
    @Override
    public String getBatchDocument(String batchId) {
        return commandExecutor.execute(new GetBatchDocumentCmd(batchId));
    }
    
    @Override
    public BatchPart getBatchPart(String batchPartId) {
        return commandExecutor.execute(new GetBatchPartCmd(batchPartId));
    }
    
    @Override
    public List<BatchPart> findBatchPartsByBatchId(String batchId) {
        return commandExecutor.execute(new FindBatchPartsByBatchIdCmd(batchId));
    }
    
    @Override
    public List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status) {
        return commandExecutor.execute(new FindBatchPartsByBatchIdCmd(batchId, status));
    }
    
    @Override
    public String getBatchPartDocument(String batchPartId) {
        return commandExecutor.execute(new GetBatchPartDocumentCmd(batchPartId));
    }
    
    @Override
    public BatchQuery createBatchQuery() {
        return new BatchQueryImpl(commandExecutor, configuration.getBatchServiceConfiguration());
    }
    
    @Override
    public BatchBuilder createBatchBuilder() {
        return new BatchBuilderImpl(commandExecutor, configuration.getBatchServiceConfiguration());
    }
    
    @Override
    public BatchPartQuery createBatchPartQuery() {
        return new BatchPartQueryImpl(commandExecutor, configuration.getBatchServiceConfiguration());
    }

    @Override
    public BatchPartBuilder createBatchPartBuilder(Batch batch) {
        return new BatchPartBuilderImpl(batch, configuration.getBatchServiceConfiguration(), commandExecutor);
    }

    @Override
    public void deleteBatch(String batchId) {
        commandExecutor.execute(new DeleteBatchCmd(batchId));
    }

    @Override
    public Map<String, String> getProperties() {
        return commandExecutor.execute(new GetPropertiesCmd(configuration.getEngineCfgKey()));
    }

    @Override
    public String databaseSchemaUpgrade(final Connection connection, final String catalog, final String schema) {
        CommandConfig config = commandExecutor.getDefaultConfig().transactionNotSupported();
        return commandExecutor.execute(config, new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                DbSqlSessionFactory dbSqlSessionFactory = (DbSqlSessionFactory) commandContext.getSessionFactories().get(DbSqlSession.class);
                DbSqlSession dbSqlSession = new DbSqlSession(dbSqlSessionFactory, CommandContextUtil.getEntityCache(commandContext), connection, catalog, schema);
                commandContext.getSessions().put(DbSqlSession.class, dbSqlSession);
                return CommandContextUtil.getProcessEngineConfiguration(commandContext).getSchemaManager().schemaUpdate();
            }
        });
    }

    @Override
    public <T> T executeCommand(Command<T> command) {
        if (command == null) {
            throw new FlowableIllegalArgumentException("The command is null");
        }
        return commandExecutor.execute(command);
    }

    @Override
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
    public LockManager getLockManager(String lockName) {
        return new LockManagerImpl(commandExecutor, lockName, getConfiguration().getLockPollRate(), configuration.getEngineCfgKey());
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

    @Override
    public ExternalWorkerJobAcquireBuilder createExternalWorkerJobAcquireBuilder() {
        return new ExternalWorkerJobAcquireBuilderImpl(commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public ExternalWorkerJobFailureBuilder createExternalWorkerJobFailureBuilder(String externalJobId, String workerId) {
        return new ExternalWorkerJobFailureBuilderImpl(externalJobId, workerId, commandExecutor, configuration.getJobServiceConfiguration());
    }

    @Override
    public ExternalWorkerCompletionBuilder createExternalWorkerCompletionBuilder(String externalJobId, String workerId) {
        return new ExternalWorkerCompletionBuilderImpl(commandExecutor, externalJobId, workerId, configuration.getJobServiceConfiguration());
    }

    @Override
    public ChangeTenantIdBuilder createChangeTenantIdBuilder(String fromTenantId, String toTenantId) {
        return new ChangeTenantIdBuilderImpl(fromTenantId, toTenantId, configuration.getChangeTenantIdManager());
    }

}
