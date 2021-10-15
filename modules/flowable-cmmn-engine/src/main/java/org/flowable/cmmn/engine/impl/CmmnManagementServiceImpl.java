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
package org.flowable.cmmn.engine.impl;

import java.util.Collection;
import java.util.Map;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.api.BatchPartBuilder;
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.batch.api.BatchQuery;
import org.flowable.batch.service.BatchPartBuilderImpl;
import org.flowable.batch.service.impl.BatchBuilderImpl;
import org.flowable.batch.service.impl.BatchPartQueryImpl;
import org.flowable.batch.service.impl.BatchQueryImpl;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.runtime.CmmnExternalWorkerTransitionBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.DeleteBatchCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTableNamesCmd;
import org.flowable.cmmn.engine.impl.cmd.HandleHistoryCleanupTimerJobCmd;
import org.flowable.cmmn.engine.impl.runtime.CmmnExternalWorkerTransitionBuilderImpl;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.impl.cmd.GetTableCountCmd;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdBuilderImpl;
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
import org.flowable.job.service.impl.cmd.DeleteHistoryJobCmd;
import org.flowable.job.service.impl.cmd.DeleteJobCmd;
import org.flowable.job.service.impl.cmd.DeleteSuspendedJobCmd;
import org.flowable.job.service.impl.cmd.DeleteTimerJobCmd;
import org.flowable.job.service.impl.cmd.ExecuteHistoryJobCmd;
import org.flowable.job.service.impl.cmd.ExecuteJobCmd;
import org.flowable.job.service.impl.cmd.GetHistoryJobAdvancedConfigurationCmd;
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
 * @author Joram Barrez
 */
public class CmmnManagementServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnManagementService {

    public CmmnManagementServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public Map<String, Long> getTableCounts() {
        return commandExecutor.execute(new GetTableCountCmd(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG));
    }

    @Override
    public Collection<String> getTableNames() {
        return commandExecutor.execute(new GetTableNamesCmd());
    }
    
    @Override
    public void executeJob(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Job id is null");
        }

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
    public void deleteHistoryJob(String jobId) {
        commandExecutor.execute(new DeleteHistoryJobCmd(jobId, configuration.getJobServiceConfiguration()));
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
    public void setJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetJobRetriesCmd(jobId, retries, configuration.getJobServiceConfiguration()));
    }

    @Override
    public void setTimerJobRetries(String jobId, int retries) {
        commandExecutor.execute(new SetTimerJobRetriesCmd(jobId, retries, configuration.getJobServiceConfiguration()));
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
    public HistoryJobQuery createHistoryJobQuery() {
        return new HistoryJobQueryImpl(commandExecutor, configuration.getJobServiceConfiguration());
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
    public CmmnExternalWorkerTransitionBuilder createCmmnExternalWorkerTransitionBuilder(String externalJobId, String workerId) {
        return new CmmnExternalWorkerTransitionBuilderImpl(commandExecutor, externalJobId, workerId);
    }
    
    @Override
    public ChangeTenantIdBuilder createChangeTenantIdBuilder(String fromTenantId, String toTenantId) {
        return new ChangeTenantIdBuilderImpl(fromTenantId, toTenantId, configuration.getChangeTenantIdManager());
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

}
