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
package org.flowable.job.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.service.impl.HistoryJobServiceImpl;
import org.flowable.job.service.impl.JobServiceImpl;
import org.flowable.job.service.impl.TimerJobServiceImpl;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.DefaultJobManager;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.data.DeadLetterJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.HistoryJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.JobByteArrayDataManager;
import org.flowable.job.service.impl.persistence.entity.data.JobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisDeadLetterJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisHistoryJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisJobByteArrayDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisSuspendedJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisTimerJobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class JobServiceConfiguration extends AbstractServiceConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JobServiceConfiguration.class);
    
    public static String JOB_EXECUTION_SCOPE_ALL = "all";
    public static String JOB_EXECUTION_SCOPE_CMMN = "cmmn";

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected JobService jobService = new JobServiceImpl(this);
    protected TimerJobService timerJobService = new TimerJobServiceImpl(this);
    protected HistoryJobService historyJobService = new HistoryJobServiceImpl(this);

    protected JobManager jobManager;

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected JobDataManager jobDataManager;
    protected DeadLetterJobDataManager deadLetterJobDataManager;
    protected SuspendedJobDataManager suspendedJobDataManager;
    protected TimerJobDataManager timerJobDataManager;
    protected HistoryJobDataManager historyJobDataManager;
    protected JobByteArrayDataManager jobByteArrayDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////

    protected JobEntityManager jobEntityManager;
    protected DeadLetterJobEntityManager deadLetterJobEntityManager;
    protected SuspendedJobEntityManager suspendedJobEntityManager;
    protected TimerJobEntityManager timerJobEntityManager;
    protected HistoryJobEntityManager historyJobEntityManager;
    protected JobByteArrayEntityManager jobByteArrayEntityManager;

    protected CommandExecutor commandExecutor;

    protected ExpressionManager expressionManager;
    protected BusinessCalendarManager businessCalendarManager;

    protected HistoryLevel historyLevel;

    protected InternalJobManager internalJobManager;
    protected InternalJobCompatibilityManager internalJobCompatibilityManager;

    protected AsyncExecutor asyncExecutor;
    
    protected String jobExecutionScope;

    protected Map<String, JobHandler> jobHandlers;
    protected FailedJobCommandFactory failedJobCommandFactory;
    protected List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers;

    protected Map<String, HistoryJobHandler> historyJobHandlers;

    protected int asyncExecutorNumberOfRetries;
    protected int asyncExecutorResetExpiredJobsMaxTimeout;

    protected ObjectMapper objectMapper;

    protected List<JobProcessor> jobProcessors;
    protected List<HistoryJobProcessor> historyJobProcessors;
    protected InternalJobParentStateResolver jobParentStateResolver;

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initJobManager();
        initDataManagers();
        initEntityManagers();
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    @Override
    public boolean isHistoryEnabled() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}", historyLevel);
        }
        return historyLevel != HistoryLevel.NONE;
    }

    // Job manager ///////////////////////////////////////////////////////////

    public void initJobManager() {
        if (jobManager == null) {
            jobManager = new DefaultJobManager(this);
        }

        jobManager.setJobServiceConfiguration(this);
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (jobDataManager == null) {
            jobDataManager = new MybatisJobDataManager();
        }
        if (deadLetterJobDataManager == null) {
            deadLetterJobDataManager = new MybatisDeadLetterJobDataManager();
        }
        if (suspendedJobDataManager == null) {
            suspendedJobDataManager = new MybatisSuspendedJobDataManager();
        }
        if (timerJobDataManager == null) {
            timerJobDataManager = new MybatisTimerJobDataManager();
        }
        if (historyJobDataManager == null) {
            historyJobDataManager = new MybatisHistoryJobDataManager();
        }
        if (jobByteArrayDataManager == null) {
            jobByteArrayDataManager = new MybatisJobByteArrayDataManager();
        }
    }

    public void initEntityManagers() {
        if (jobEntityManager == null) {
            jobEntityManager = new JobEntityManagerImpl(this, jobDataManager);
        }
        if (deadLetterJobEntityManager == null) {
            deadLetterJobEntityManager = new DeadLetterJobEntityManagerImpl(this, deadLetterJobDataManager);
        }
        if (suspendedJobEntityManager == null) {
            suspendedJobEntityManager = new SuspendedJobEntityManagerImpl(this, suspendedJobDataManager);
        }
        if (timerJobEntityManager == null) {
            timerJobEntityManager = new TimerJobEntityManagerImpl(this, timerJobDataManager);
        }
        if (historyJobEntityManager == null) {
            historyJobEntityManager = new HistoryJobEntityManagerImpl(this, historyJobDataManager);
        }
        if (jobByteArrayEntityManager == null) {
            jobByteArrayEntityManager = new JobByteArrayEntityManagerImpl(this, jobByteArrayDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public JobServiceConfiguration getIdentityLinkServiceConfiguration() {
        return this;
    }

    public JobService getJobService() {
        return jobService;
    }

    public JobServiceConfiguration setJobService(JobService jobService) {
        this.jobService = jobService;
        return this;
    }

    public TimerJobService getTimerJobService() {
        return timerJobService;
    }

    public JobServiceConfiguration setTimerJobService(TimerJobService timerJobService) {
        this.timerJobService = timerJobService;
        return this;
    }

    public HistoryJobService getHistoryJobService() {
        return historyJobService;
    }

    public JobServiceConfiguration setHistoryJobService(HistoryJobService historyJobService) {
        this.historyJobService = historyJobService;
        return this;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public void setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    public JobDataManager getJobDataManager() {
        return jobDataManager;
    }

    public JobServiceConfiguration setJobDataManager(JobDataManager jobDataManager) {
        this.jobDataManager = jobDataManager;
        return this;
    }

    public DeadLetterJobDataManager getDeadLetterJobDataManager() {
        return deadLetterJobDataManager;
    }

    public JobServiceConfiguration setDeadLetterJobDataManager(DeadLetterJobDataManager deadLetterJobDataManager) {
        this.deadLetterJobDataManager = deadLetterJobDataManager;
        return this;
    }

    public SuspendedJobDataManager getSuspendedJobDataManager() {
        return suspendedJobDataManager;
    }

    public JobServiceConfiguration setSuspendedJobDataManager(SuspendedJobDataManager suspendedJobDataManager) {
        this.suspendedJobDataManager = suspendedJobDataManager;
        return this;
    }

    public TimerJobDataManager getTimerJobDataManager() {
        return timerJobDataManager;
    }

    public JobServiceConfiguration setTimerJobDataManager(TimerJobDataManager timerJobDataManager) {
        this.timerJobDataManager = timerJobDataManager;
        return this;
    }

    public HistoryJobDataManager getHistoryJobDataManager() {
        return historyJobDataManager;
    }

    public JobServiceConfiguration setHistoryJobDataManager(HistoryJobDataManager historyJobDataManager) {
        this.historyJobDataManager = historyJobDataManager;
        return this;
    }

    public JobByteArrayDataManager getJobByteArrayDataManager() {
        return jobByteArrayDataManager;
    }

    public JobServiceConfiguration setJobByteArrayDataManager(JobByteArrayDataManager jobByteArrayDataManager) {
        this.jobByteArrayDataManager = jobByteArrayDataManager;
        return this;
    }

    public JobEntityManager getJobEntityManager() {
        return jobEntityManager;
    }

    public JobServiceConfiguration setJobEntityManager(JobEntityManager jobEntityManager) {
        this.jobEntityManager = jobEntityManager;
        return this;
    }

    public DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
        return deadLetterJobEntityManager;
    }

    public JobServiceConfiguration setDeadLetterJobEntityManager(DeadLetterJobEntityManager deadLetterJobEntityManager) {
        this.deadLetterJobEntityManager = deadLetterJobEntityManager;
        return this;
    }

    public SuspendedJobEntityManager getSuspendedJobEntityManager() {
        return suspendedJobEntityManager;
    }

    public JobServiceConfiguration setSuspendedJobEntityManager(SuspendedJobEntityManager suspendedJobEntityManager) {
        this.suspendedJobEntityManager = suspendedJobEntityManager;
        return this;
    }

    public TimerJobEntityManager getTimerJobEntityManager() {
        return timerJobEntityManager;
    }

    public JobServiceConfiguration setTimerJobEntityManager(TimerJobEntityManager timerJobEntityManager) {
        this.timerJobEntityManager = timerJobEntityManager;
        return this;
    }

    public HistoryJobEntityManager getHistoryJobEntityManager() {
        return historyJobEntityManager;
    }

    public JobServiceConfiguration setHistoryJobEntityManager(HistoryJobEntityManager historyJobEntityManager) {
        this.historyJobEntityManager = historyJobEntityManager;
        return this;
    }

    public JobByteArrayEntityManager getJobByteArrayEntityManager() {
        return jobByteArrayEntityManager;
    }

    public JobServiceConfiguration setJobByteArrayEntityManager(JobByteArrayEntityManager jobByteArrayEntityManager) {
        this.jobByteArrayEntityManager = jobByteArrayEntityManager;
        return this;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    @Override
    public JobServiceConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }

    public InternalJobManager getInternalJobManager() {
        return internalJobManager;
    }

    public void setInternalJobManager(InternalJobManager internalJobManager) {
        this.internalJobManager = internalJobManager;
    }
    
    public InternalJobCompatibilityManager getInternalJobCompatibilityManager() {
        return internalJobCompatibilityManager;
    }

    public void setInternalJobCompatibilityManager(InternalJobCompatibilityManager internalJobCompatibilityManager) {
        this.internalJobCompatibilityManager = internalJobCompatibilityManager;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public JobServiceConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }

    public String getJobExecutionScope() {
        return jobExecutionScope;
    }

    public JobServiceConfiguration setJobExecutionScope(String jobExecutionScope) {
        this.jobExecutionScope = jobExecutionScope;
        return this;
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public JobServiceConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        return businessCalendarManager;
    }

    public JobServiceConfiguration setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
        this.businessCalendarManager = businessCalendarManager;
        return this;
    }

    public Map<String, JobHandler> getJobHandlers() {
        return jobHandlers;
    }

    public JobServiceConfiguration setJobHandlers(Map<String, JobHandler> jobHandlers) {
        this.jobHandlers = jobHandlers;
        return this;
    }

    public FailedJobCommandFactory getFailedJobCommandFactory() {
        return failedJobCommandFactory;
    }

    public JobServiceConfiguration setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
        this.failedJobCommandFactory = failedJobCommandFactory;
        return this;
    }

    public List<AsyncRunnableExecutionExceptionHandler> getAsyncRunnableExecutionExceptionHandlers() {
        return asyncRunnableExecutionExceptionHandlers;
    }

    public JobServiceConfiguration setAsyncRunnableExecutionExceptionHandlers(List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers) {
        this.asyncRunnableExecutionExceptionHandlers = asyncRunnableExecutionExceptionHandlers;
        return this;
    }

    public Map<String, HistoryJobHandler> getHistoryJobHandlers() {
        return historyJobHandlers;
    }

    public JobServiceConfiguration setHistoryJobHandlers(Map<String, HistoryJobHandler> historyJobHandlers) {
        this.historyJobHandlers = historyJobHandlers;
        return this;
    }

    public int getAsyncExecutorNumberOfRetries() {
        return asyncExecutorNumberOfRetries;
    }

    public JobServiceConfiguration setAsyncExecutorNumberOfRetries(int asyncExecutorNumberOfRetries) {
        this.asyncExecutorNumberOfRetries = asyncExecutorNumberOfRetries;
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsMaxTimeout() {
        return asyncExecutorResetExpiredJobsMaxTimeout;
    }

    public JobServiceConfiguration setAsyncExecutorResetExpiredJobsMaxTimeout(int asyncExecutorResetExpiredJobsMaxTimeout) {
        this.asyncExecutorResetExpiredJobsMaxTimeout = asyncExecutorResetExpiredJobsMaxTimeout;
        return this;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public JobServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public List<JobProcessor> getJobProcessors() {
        return jobProcessors;
    }

    public JobServiceConfiguration setJobProcessors(List<JobProcessor> jobProcessors) {
        this.jobProcessors = Collections.unmodifiableList(jobProcessors);
        return this;
    }

    public List<HistoryJobProcessor> getHistoryJobProcessors() {
        return historyJobProcessors;
    }

    public JobServiceConfiguration setHistoryJobProcessors(List<HistoryJobProcessor> historyJobProcessors) {
        this.historyJobProcessors = Collections.unmodifiableList(historyJobProcessors);
        return this;
    }

    public void setJobParentStateResolver(InternalJobParentStateResolver jobParentStateResolver) {
        this.jobParentStateResolver = jobParentStateResolver;
    }

    public InternalJobParentStateResolver getJobParentStateResolver() {
        return jobParentStateResolver;
    }
}
