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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.flowable.job.service.impl.history.async.AsyncHistoryJobHandler;
import org.flowable.job.service.impl.history.async.transformer.HistoryJsonTransformer;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManagerImpl;
import org.flowable.job.service.impl.persistence.entity.data.DeadLetterJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.ExternalWorkerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.HistoryJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.JobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisDeadLetterJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisExternalWorkerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisHistoryJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisSuspendedJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.MybatisTimerJobDataManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This service configuration contains all settings and instances around job execution and management.
 * Note that a {@link JobServiceConfiguration} is not shared between engines and instantiated for each engine.
 * 
 * @author Tijs Rademakers
 */
public class JobServiceConfiguration extends AbstractServiceConfiguration {

    public static final String JOB_EXECUTION_SCOPE_ALL = "all";
    public static final String JOB_EXECUTION_SCOPE_CMMN = "cmmn";

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
    protected ExternalWorkerJobDataManager externalWorkerJobDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////

    protected JobEntityManager jobEntityManager;
    protected DeadLetterJobEntityManager deadLetterJobEntityManager;
    protected SuspendedJobEntityManager suspendedJobEntityManager;
    protected TimerJobEntityManager timerJobEntityManager;
    protected HistoryJobEntityManager historyJobEntityManager;
    protected ExternalWorkerJobEntityManager externalWorkerJobEntityManager;

    protected CommandExecutor commandExecutor;

    protected ExpressionManager expressionManager;
    protected BusinessCalendarManager businessCalendarManager;

    protected InternalJobManager internalJobManager;
    protected InternalJobCompatibilityManager internalJobCompatibilityManager;
    protected InternalJobParentStateResolver jobParentStateResolver;

    protected AsyncExecutor asyncExecutor;
    protected int asyncExecutorNumberOfRetries;
    protected int asyncExecutorResetExpiredJobsMaxTimeout;
    
    protected String jobExecutionScope;
    protected Map<String, JobHandler> jobHandlers;
    protected FailedJobCommandFactory failedJobCommandFactory;
    protected List<AsyncRunnableExecutionExceptionHandler> asyncRunnableExecutionExceptionHandlers;
    protected List<JobProcessor> jobProcessors;
    
    protected List<String> enabledJobCategories;
    
    protected AsyncExecutor asyncHistoryExecutor;
    protected int asyncHistoryExecutorNumberOfRetries;
    protected String historyJobExecutionScope;
    
    protected Map<String, HistoryJobHandler> historyJobHandlers;
    protected List<HistoryJobProcessor> historyJobProcessors;
    
    protected String jobTypeAsyncHistory;
    protected String jobTypeAsyncHistoryZipped;
    
    protected boolean asyncHistoryJsonGzipCompressionEnabled;
    protected boolean asyncHistoryJsonGroupingEnabled;
    protected boolean asyncHistoryExecutorMessageQueueMode;
    protected int asyncHistoryJsonGroupingThreshold = 10;
    
    public JobServiceConfiguration(String engineName) {
        super(engineName);
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initJobManager();
        initDataManagers();
        initEntityManagers();
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (logger.isDebugEnabled()) {
            logger.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    @Override
    public boolean isHistoryEnabled() {
        if (logger.isDebugEnabled()) {
            logger.debug("Current history level: {}", historyLevel);
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
            jobDataManager = new MybatisJobDataManager(this);
        }
        if (deadLetterJobDataManager == null) {
            deadLetterJobDataManager = new MybatisDeadLetterJobDataManager(this);
        }
        if (suspendedJobDataManager == null) {
            suspendedJobDataManager = new MybatisSuspendedJobDataManager(this);
        }
        if (timerJobDataManager == null) {
            timerJobDataManager = new MybatisTimerJobDataManager(this);
        }
        if (historyJobDataManager == null) {
            historyJobDataManager = new MybatisHistoryJobDataManager(this);
        }
        if (externalWorkerJobDataManager == null) {
            externalWorkerJobDataManager = new MybatisExternalWorkerJobDataManager(this);
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
        if (externalWorkerJobEntityManager == null) {
            externalWorkerJobEntityManager = new ExternalWorkerJobEntityManagerImpl(this, externalWorkerJobDataManager);
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

    public ExternalWorkerJobDataManager getExternalWorkerJobDataManager() {
        return externalWorkerJobDataManager;
    }

    public JobServiceConfiguration setExternalWorkerJobDataManager(ExternalWorkerJobDataManager externalWorkerJobDataManager) {
        this.externalWorkerJobDataManager = externalWorkerJobDataManager;
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

    public ExternalWorkerJobEntityManager getExternalWorkerJobEntityManager() {
        return externalWorkerJobEntityManager;
    }

    public JobServiceConfiguration setExternalWorkerJobEntityManager(ExternalWorkerJobEntityManager externalWorkerJobEntityManager) {
        this.externalWorkerJobEntityManager = externalWorkerJobEntityManager;
        return this;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
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
    
    public AsyncExecutor getAsyncHistoryExecutor() {
        return asyncHistoryExecutor;
    }

    public JobServiceConfiguration setAsyncHistoryExecutor(AsyncExecutor asyncHistoryExecutor) {
        this.asyncHistoryExecutor = asyncHistoryExecutor;
        return this;
    }
    
    public int getAsyncHistoryExecutorNumberOfRetries() {
        return asyncHistoryExecutorNumberOfRetries;
    }

    public JobServiceConfiguration setAsyncHistoryExecutorNumberOfRetries(int asyncHistoryExecutorNumberOfRetries) {
        this.asyncHistoryExecutorNumberOfRetries = asyncHistoryExecutorNumberOfRetries;
        return this;
    }

    public String getJobExecutionScope() {
        return jobExecutionScope;
    }

    public JobServiceConfiguration setJobExecutionScope(String jobExecutionScope) {
        this.jobExecutionScope = jobExecutionScope;
        return this;
    }
    
    public String getHistoryJobExecutionScope() {
        return historyJobExecutionScope;
    }

    public JobServiceConfiguration setHistoryJobExecutionScope(String historyJobExecutionScope) {
        this.historyJobExecutionScope = historyJobExecutionScope;
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
    
    public JobServiceConfiguration addJobHandler(String type, JobHandler jobHandler) {
        if (this.jobHandlers == null) {
            this.jobHandlers = new HashMap<>();
        }
        this.jobHandlers.put(type, jobHandler);
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
    
    public JobServiceConfiguration addHistoryJobHandler(String type, HistoryJobHandler historyJobHandler) {
        if (this.historyJobHandlers == null) {
            this.historyJobHandlers = new HashMap<>();
        }
        this.historyJobHandlers.put(type, historyJobHandler);
        return this;
    }
    
    /**
     * Registers the given {@link HistoryJobHandler} under the provided type and checks for 
     * existing <b>default and internal</b> {@link HistoryJobHandler} instances to be of the same class.
     * 
     * If no such instances are found, a {@link #addHistoryJobHandler(String, HistoryJobHandler)} is done.
     * 
     * If such instances are found, they are merged, meaning the {@link HistoryJsonTransformer} instances of the provided {@link HistoryJobHandler} 
     * are copied into the already registered {@link HistoryJobHandler} and vice versa.
     * 
     * If a type is already registered, the provided history job handler is simply ignored.
     * 
     * This is especially useful when multiple engines (e.g. bpmn and cmmn) share an async history executor.
     * In this case, both {@link AsyncHistoryJobHandler} instances should be able to handle history jobs from any engine.
     */
    public JobServiceConfiguration mergeHistoryJobHandler(HistoryJobHandler historyJobHandler) {
        if (historyJobHandlers != null 
                && historyJobHandler instanceof AsyncHistoryJobHandler
                && !historyJobHandlers.containsKey(historyJobHandler.getType())) {
            
            for (HistoryJobHandler existingHistoryJobHandler : historyJobHandlers.values()) {
                if (existingHistoryJobHandler.getClass().equals(historyJobHandler.getClass())) {
                    copyHistoryJsonTransformers((AsyncHistoryJobHandler) historyJobHandler, (AsyncHistoryJobHandler) existingHistoryJobHandler);
                    copyHistoryJsonTransformers((AsyncHistoryJobHandler) existingHistoryJobHandler, (AsyncHistoryJobHandler) historyJobHandler);
                }
            }
        }
        addHistoryJobHandler(historyJobHandler.getType(), historyJobHandler);
        return this;
    }

    protected void copyHistoryJsonTransformers(AsyncHistoryJobHandler source, AsyncHistoryJobHandler target) {
        source.getHistoryJsonTransformers().forEach((transformerType, transformersList) -> {
            for (HistoryJsonTransformer historyJsonTransformer : transformersList) {
                if (!target.getHistoryJsonTransformers().containsKey(transformerType)) {
                    target.addHistoryJsonTransformer(historyJsonTransformer);
                }
            }
        });
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
    
    public List<String> getEnabledJobCategories() {
        return enabledJobCategories;
    }

    public void setEnabledJobCategories(List<String> enabledJobCategories) {
        this.enabledJobCategories = enabledJobCategories;
    }

    public void addEnabledJobCategory(String jobCategory) {
        if (enabledJobCategories == null) {
            enabledJobCategories = new ArrayList<>();
        }
        
        enabledJobCategories.add(jobCategory);
    }

    public String getJobTypeAsyncHistory() {
        return jobTypeAsyncHistory;
    }

    public void setJobTypeAsyncHistory(String jobTypeAsyncHistory) {
        this.jobTypeAsyncHistory = jobTypeAsyncHistory;
    }

    public String getJobTypeAsyncHistoryZipped() {
        return jobTypeAsyncHistoryZipped;
    }

    public void setJobTypeAsyncHistoryZipped(String jobTypeAsyncHistoryZipped) {
        this.jobTypeAsyncHistoryZipped = jobTypeAsyncHistoryZipped;
    }

    public boolean isAsyncHistoryJsonGzipCompressionEnabled() {
        return asyncHistoryJsonGzipCompressionEnabled;
    }

    public void setAsyncHistoryJsonGzipCompressionEnabled(boolean asyncHistoryJsonGzipCompressionEnabled) {
        this.asyncHistoryJsonGzipCompressionEnabled = asyncHistoryJsonGzipCompressionEnabled;
    }

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return asyncHistoryJsonGroupingEnabled;
    }

    public void setAsyncHistoryJsonGroupingEnabled(boolean asyncHistoryJsonGroupingEnabled) {
        this.asyncHistoryJsonGroupingEnabled = asyncHistoryJsonGroupingEnabled;
    }

    public boolean isAsyncHistoryExecutorMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    public void setAsyncHistoryExecutorMessageQueueMode(boolean asyncHistoryExecutorMessageQueueMode) {
        this.asyncHistoryExecutorMessageQueueMode = asyncHistoryExecutorMessageQueueMode;
    }

    public int getAsyncHistoryJsonGroupingThreshold() {
        return asyncHistoryJsonGroupingThreshold;
    }

    public void setAsyncHistoryJsonGroupingThreshold(int asyncHistoryJsonGroupingThreshold) {
        this.asyncHistoryJsonGroupingThreshold = asyncHistoryJsonGroupingThreshold;
    }
    
}
