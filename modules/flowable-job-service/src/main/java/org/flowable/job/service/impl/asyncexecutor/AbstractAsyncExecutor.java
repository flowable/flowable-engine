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
package org.flowable.job.service.impl.asyncexecutor;

import java.time.Duration;
import java.util.LinkedList;

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.UnacquireOwnedJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Marcus Klimstra
 */
public abstract class AbstractAsyncExecutor implements AsyncExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAsyncExecutor.class);

    protected AsyncJobExecutorConfiguration configuration;

    protected AcquireTimerJobsRunnable timerJobRunnable;
    protected AcquireTimerLifecycleListener timerLifecycleListener;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected AcquireAsyncJobsDueRunnable asyncJobsDueRunnable;
    protected AcquireAsyncJobsDueLifecycleListener asyncJobsDueLifecycleListener;
    protected ResetExpiredJobsRunnable resetExpiredJobsRunnable;

    protected ExecuteAsyncRunnableFactory executeAsyncRunnableFactory;
    
    protected AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler;

    protected boolean isAutoActivate;
    protected boolean isActive;
    protected boolean isMessageQueueMode;

    // Job queue used when async executor is not yet started and jobs are already added.
    // This is mainly used for testing purpose.
    protected LinkedList<JobInfo> temporaryJobQueue = new LinkedList<>();

    protected JobServiceConfiguration jobServiceConfiguration;

    public AbstractAsyncExecutor() {
        this(new AsyncJobExecutorConfiguration());
    }

    public AbstractAsyncExecutor(AsyncJobExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean executeAsyncJob(final JobInfo job) {
        if (isMessageQueueMode) {
            // When running with a message queue based job executor,
            // the job is not executed here.
            return true;
        }

        Runnable runnable = null;
        if (isActive) {
            runnable = createRunnableForJob(job);
            return executeAsyncJob(job, runnable);
        } else {
            temporaryJobQueue.add(job);
        }

        return true;
    }

    protected abstract boolean executeAsyncJob(final JobInfo job, Runnable runnable);

    protected void unlockOwnedJobs() {
        jobServiceConfiguration.getCommandExecutor().execute(new UnacquireOwnedJobsCmd(configuration.getLockOwner(), configuration.getTenantId(), jobServiceConfiguration));
    }

    protected Runnable createRunnableForJob(final JobInfo job) {
        if (executeAsyncRunnableFactory == null) {
            return new ExecuteAsyncRunnable(job, jobServiceConfiguration, jobEntityManager, asyncRunnableExecutionExceptionHandler);
        } else {
            return executeAsyncRunnableFactory.createExecuteAsyncRunnable(job, jobServiceConfiguration);
        }
    }

    /** Starts the async executor */
    @Override
    public void start() {
        if (isActive) {
            return;
        }

        isActive = true;

        LOGGER.info("Starting up the async job executor [{}] for engine {}", getClass().getName(), getJobServiceConfiguration().getEngineName());

        initializeJobEntityManager();
        initializeRunnables();
        startAdditionalComponents();
        executeTemporaryJobs();
    }
    
    protected void initializeJobEntityManager() {
        if (jobEntityManager == null) {
            jobEntityManager = jobServiceConfiguration.getJobEntityManager();
        }
    }

    protected void initializeRunnables() {
        if (configuration.isTimerRunnableNeeded() && timerJobRunnable == null) {
            timerJobRunnable = new AcquireTimerJobsRunnable(this, jobServiceConfiguration.getJobManager(),
                timerLifecycleListener, new AcquireTimerRunnableConfiguration(), configuration.getMoveTimerExecutorPoolSize());
        }

        JobInfoEntityManager<? extends JobInfoEntity> jobEntityManagerToUse = jobEntityManager != null
                ? jobEntityManager : jobServiceConfiguration.getJobEntityManager();

        if (resetExpiredJobsRunnable == null) {
            String resetExpiredRunnableName = configuration.getResetExpiredRunnableName();
            String resetRunnableName = resetExpiredRunnableName != null ?
                    resetExpiredRunnableName : "flowable-" + getJobServiceConfiguration().getEngineName() + "-reset-expired-jobs";
            resetExpiredJobsRunnable = createResetExpiredJobsRunnable(resetRunnableName);
        }

        if (!isMessageQueueMode && asyncJobsDueRunnable == null) {
            String acquireRunnableThreadName = configuration.getAcquireRunnableThreadName();
            String acquireJobsRunnableName = acquireRunnableThreadName != null ?
                    acquireRunnableThreadName : "flowable-" + getJobServiceConfiguration().getEngineName() + "-acquire-async-jobs";
            asyncJobsDueRunnable = new AcquireAsyncJobsDueRunnable(acquireJobsRunnableName, this, jobEntityManagerToUse,
                asyncJobsDueLifecycleListener, new AcquireAsyncJobsDueRunnableConfiguration());

        }
    }

    protected abstract ResetExpiredJobsRunnable createResetExpiredJobsRunnable(String resetRunnableName);

    protected abstract void startAdditionalComponents();

    protected void executeTemporaryJobs() {
        while (!temporaryJobQueue.isEmpty()) {
            JobInfo job = temporaryJobQueue.pop();
            executeAsyncJob(job);
        }
    }

    /** Shuts down the whole job executor */
    @Override
    public synchronized void shutdown() {
        if (!isActive) {
            return;
        }
        LOGGER.info("Shutting down the async job executor [{}] for engine {}", getClass().getName(), getJobServiceConfiguration().getEngineName());

        stopRunnables();
        shutdownAdditionalComponents();

        isActive = false;
    }

    protected void stopRunnables() {
        if (timerJobRunnable != null) {
            timerJobRunnable.stop();
        }
        if (asyncJobsDueRunnable != null) {
            asyncJobsDueRunnable.stop();
        }
        if (resetExpiredJobsRunnable != null) {
            resetExpiredJobsRunnable.stop();
        }

        timerJobRunnable = null;
        asyncJobsDueRunnable = null;
        resetExpiredJobsRunnable = null;
    }

    protected abstract void shutdownAdditionalComponents();

    /* getters and setters */

    @Override
    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    @Override
    public void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public boolean isAutoActivate() {
        return isAutoActivate;
    }

    @Override
    public void setAutoActivate(boolean isAutoActivate) {
        this.isAutoActivate = isAutoActivate;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    public boolean isMessageQueueMode() {
        return isMessageQueueMode;
    }

    public void setMessageQueueMode(boolean isMessageQueueMode) {
        this.isMessageQueueMode = isMessageQueueMode;
    }

    @Override
    public String getLockOwner() {
        return configuration.getLockOwner();
    }

    public void setLockOwner(String lockOwner) {
        configuration.setLockOwner(lockOwner);
    }

    @Override
    public int getTimerLockTimeInMillis() {
        return (int) configuration.getTimerLockTime().toMillis();
    }

    @Override
    public void setTimerLockTimeInMillis(int timerLockTimeInMillis) {
        configuration.setTimerLockTime(Duration.ofMillis(timerLockTimeInMillis));
    }

    @Override
    public int getAsyncJobLockTimeInMillis() {
        return (int) configuration.getAsyncJobLockTime().toMillis();
    }

    @Override
    public void setAsyncJobLockTimeInMillis(int asyncJobLockTimeInMillis) {
        configuration.setAsyncJobLockTime(Duration.ofMillis(asyncJobLockTimeInMillis));
    }

    public int getMoveTimerExecutorPoolSize() {
        return configuration.getMoveTimerExecutorPoolSize();
    }

    public void setMoveTimerExecutorPoolSize(int moveTimerExecutorPoolSize) {
        configuration.setMoveTimerExecutorPoolSize(moveTimerExecutorPoolSize);
    }

    @Override
    public int getMaxTimerJobsPerAcquisition() {
        return configuration.getMaxTimerJobsPerAcquisition();
    }

    @Override
    public void setMaxTimerJobsPerAcquisition(int maxTimerJobsPerAcquisition) {
        configuration.setMaxTimerJobsPerAcquisition(maxTimerJobsPerAcquisition);
    }

    @Override
    public int getMaxAsyncJobsDuePerAcquisition() {
        return configuration.getMaxAsyncJobsDuePerAcquisition();
    }

    @Override
    public void setMaxAsyncJobsDuePerAcquisition(int maxAsyncJobsDuePerAcquisition) {
        this.configuration.setMaxAsyncJobsDuePerAcquisition(maxAsyncJobsDuePerAcquisition);
    }

    @Override
    public int getDefaultTimerJobAcquireWaitTimeInMillis() {
        return (int) configuration.getDefaultTimerJobAcquireWaitTime().toMillis();
    }

    @Override
    public void setDefaultTimerJobAcquireWaitTimeInMillis(int defaultTimerJobAcquireWaitTimeInMillis) {
        configuration.setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(defaultTimerJobAcquireWaitTimeInMillis));
    }

    @Override
    public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
        return (int) configuration.getDefaultAsyncJobAcquireWaitTime().toMillis();
    }

    @Override
    public void setDefaultAsyncJobAcquireWaitTimeInMillis(int defaultAsyncJobAcquireWaitTimeInMillis) {
        configuration.setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(defaultAsyncJobAcquireWaitTimeInMillis));
    }

    public void setTimerJobRunnable(AcquireTimerJobsRunnable timerJobRunnable) {
        this.timerJobRunnable = timerJobRunnable;
    }

    @Override
    public int getDefaultQueueSizeFullWaitTimeInMillis() {
        return (int) configuration.getDefaultQueueSizeFullWaitTime().toMillis();
    }

    @Override
    public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTime) {
        configuration.setDefaultQueueSizeFullWaitTime(Duration.ofMillis(defaultQueueSizeFullWaitTime));
    }

    public void setAsyncJobsDueRunnable(AcquireAsyncJobsDueRunnable asyncJobsDueRunnable) {
        this.asyncJobsDueRunnable = asyncJobsDueRunnable;
    }

    public AcquireAsyncJobsDueLifecycleListener getAsyncJobsDueLifecycleListener() {
        return asyncJobsDueLifecycleListener;
    }

    public void setAsyncJobsDueLifecycleListener(AcquireAsyncJobsDueLifecycleListener asyncJobsDueLifecycleListener) {
        this.asyncJobsDueLifecycleListener = asyncJobsDueLifecycleListener;
    }

    public boolean isTimerRunnableNeeded() {
        return configuration.isTimerRunnableNeeded();
    }

    public void setTimerRunnableNeeded(boolean timerRunnableNeeded) {
        configuration.setTimerRunnableNeeded(timerRunnableNeeded);
    }

    public AcquireTimerLifecycleListener getTimerLifecycleListener() {
        return timerLifecycleListener;
    }

    public void setTimerLifecycleListener(AcquireTimerLifecycleListener timerLifecycleListener) {
        this.timerLifecycleListener = timerLifecycleListener;
    }

    public boolean isGlobalAcquireLockEnabled() {
        return configuration.isGlobalAcquireLockEnabled();
    }

    public void setGlobalAcquireLockEnabled(boolean globalAcquireLockEnabled) {
        configuration.setGlobalAcquireLockEnabled(globalAcquireLockEnabled);
    }

    public String getGlobalAcquireLockPrefix() {
        return configuration.getGlobalAcquireLockPrefix();
    }

    public void setGlobalAcquireLockPrefix(String globalAcquireLockPrefix) {
        configuration.setGlobalAcquireLockPrefix(globalAcquireLockPrefix);
    }

    public Duration getAsyncJobsGlobalLockWaitTime() {
        return configuration.getAsyncJobsGlobalLockWaitTime();
    }

    public void setAsyncJobsGlobalLockWaitTime(Duration asyncJobsGlobalLockWaitTime) {
        configuration.setAsyncJobsGlobalLockWaitTime(asyncJobsGlobalLockWaitTime);
    }

    public Duration getAsyncJobsGlobalLockPollRate() {
        return configuration.getAsyncJobsGlobalLockPollRate();
    }

    public void setAsyncJobsGlobalLockPollRate(Duration asyncJobsGlobalLockPollRate) {
        configuration.setAsyncJobsGlobalLockPollRate(asyncJobsGlobalLockPollRate);
    }

    public Duration getTimerLockWaitTime() {
        return configuration.getTimerLockWaitTime();
    }

    public void setTimerLockWaitTime(Duration timerLockWaitTime) {
        configuration.setTimerLockWaitTime(timerLockWaitTime);
    }

    public Duration getTimerLockPollRate() {
        return configuration.getTimerLockPollRate();
    }

    public void setTimerLockPollRate(Duration timerLockPollRate) {
        configuration.setTimerLockPollRate(timerLockPollRate);
    }

    public void setAcquireRunnableThreadName(String acquireRunnableThreadName) {
        configuration.setAcquireRunnableThreadName(acquireRunnableThreadName);
    }

    public void setJobEntityManager(JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {
        this.jobEntityManager = jobEntityManager;
    }
    
    public void setResetExpiredRunnableName(String resetExpiredRunnableName) {
        configuration.setResetExpiredRunnableName(resetExpiredRunnableName);
    }

    public void setResetExpiredJobsRunnable(ResetExpiredJobsRunnable resetExpiredJobsRunnable) {
        this.resetExpiredJobsRunnable = resetExpiredJobsRunnable;
    }

    @Override
    @Deprecated
    public int getRetryWaitTimeInMillis() {
        // No longer used
        return Integer.MAX_VALUE;
    }

    @Override
    @Deprecated
    public void setRetryWaitTimeInMillis(int retryWaitTimeInMillis) {
        // No longer used
    }

    @Override
    public int getResetExpiredJobsInterval() {
        return (int) configuration.getResetExpiredJobsInterval().toMillis();
    }

    @Override
    public void setResetExpiredJobsInterval(int resetExpiredJobsInterval) {
        configuration.setResetExpiredJobsInterval(Duration.ofMillis(resetExpiredJobsInterval));
    }

    @Override
    public int getResetExpiredJobsPageSize() {
        return configuration.getResetExpiredJobsPageSize();
    }

    @Override
    public void setResetExpiredJobsPageSize(int resetExpiredJobsPageSize) {
        configuration.setResetExpiredJobsPageSize(resetExpiredJobsPageSize);
    }

    public ExecuteAsyncRunnableFactory getExecuteAsyncRunnableFactory() {
        return executeAsyncRunnableFactory;
    }

    public void setExecuteAsyncRunnableFactory(ExecuteAsyncRunnableFactory executeAsyncRunnableFactory) {
        this.executeAsyncRunnableFactory = executeAsyncRunnableFactory;
    }

    public AsyncRunnableExecutionExceptionHandler getAsyncRunnableExecutionExceptionHandler() {
        return asyncRunnableExecutionExceptionHandler;
    }

    public void setAsyncRunnableExecutionExceptionHandler(AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler) {
        this.asyncRunnableExecutionExceptionHandler = asyncRunnableExecutionExceptionHandler;
    }

    public AcquireTimerJobsRunnable getTimerJobRunnable() {
        return timerJobRunnable;
    }

    public AcquireAsyncJobsDueRunnable getAsyncJobsDueRunnable() {
        return asyncJobsDueRunnable;
    }

    public ResetExpiredJobsRunnable getResetExpiredJobsRunnable() {
        return resetExpiredJobsRunnable;
    }

    public String getTenantId() {
        return configuration.getTenantId();
    }

    public void setTenantId(String tenantId) {
        configuration.setTenantId(tenantId);
    }

    public AsyncJobExecutorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(AsyncJobExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    public class AcquireTimerRunnableConfiguration implements AcquireJobsRunnableConfiguration {

        @Override
        public boolean isGlobalAcquireLockEnabled() {
            return configuration.isGlobalAcquireLockEnabled();
        }

        @Override
        public String getGlobalAcquireLockPrefix() {
            return configuration.getGlobalAcquireLockPrefix();
        }

        @Override
        public Duration getLockWaitTime() {
            return configuration.getTimerLockWaitTime();
        }

        @Override
        public Duration getLockPollRate() {
            return configuration.getTimerLockPollRate();
        }

        @Override
        public Duration getLockForceAcquireAfter() {
            return configuration.getTimerLockForceAcquireAfter();
        }
    }

    public class AcquireAsyncJobsDueRunnableConfiguration implements AcquireJobsRunnableConfiguration {

        @Override
        public boolean isGlobalAcquireLockEnabled() {
            return configuration.isGlobalAcquireLockEnabled();
        }

        @Override
        public String getGlobalAcquireLockPrefix() {
            return configuration.getGlobalAcquireLockPrefix();
        }

        @Override
        public Duration getLockWaitTime() {
            return configuration.getAsyncJobsGlobalLockWaitTime();
        }

        @Override
        public Duration getLockPollRate() {
            return configuration.getAsyncJobsGlobalLockPollRate();
        }

        @Override
        public Duration getLockForceAcquireAfter() {
            return configuration.getAsyncJobsGlobalLockForceAcquireAfter();
        }
    }
}
