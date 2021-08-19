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
import java.util.UUID;

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

    private String tenantId;
    
    protected boolean timerRunnableNeeded = true; // default true for backwards compatibility (History Async executor came later)
    protected AcquireTimerJobsRunnable timerJobRunnable;
    protected AcquireTimerLifecycleListener timerLifecycleListener;
    protected String acquireRunnableThreadName;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected AcquireAsyncJobsDueRunnable asyncJobsDueRunnable;
    protected AcquireAsyncJobsDueLifecycleListener asyncJobsDueLifecycleListener;
    protected String resetExpiredRunnableName;
    protected ResetExpiredJobsRunnable resetExpiredJobsRunnable;

    protected ExecuteAsyncRunnableFactory executeAsyncRunnableFactory;
    
    protected AsyncRunnableExecutionExceptionHandler asyncRunnableExecutionExceptionHandler;

    protected boolean isAutoActivate;
    protected boolean isActive;
    protected boolean isMessageQueueMode;

    protected int moveTimerExecutorPoolSize = 4;
    protected int maxTimerJobsPerAcquisition = 512;
    protected int maxAsyncJobsDuePerAcquisition = 512;
    protected int defaultTimerJobAcquireWaitTimeInMillis = 10 * 1000;
    protected int defaultAsyncJobAcquireWaitTimeInMillis = 10 * 1000;
    protected int defaultQueueSizeFullWaitTime = 5 * 1000;

    protected String lockOwner = UUID.randomUUID().toString();
    protected int timerLockTimeInMillis = 60 * 60 * 1000;
    protected int asyncJobLockTimeInMillis = 60 * 60 * 1000;
    protected int retryWaitTimeInMillis = 500;

    protected boolean globalAcquireLockEnabled;
    // The runnable can be running for different engines/executors.
    // Setting a different prefix allows to differentiate without them competing for the same lock
    protected String globalAcquireLockPrefix = "";
    protected Duration asyncJobsGlobalLockWaitTime = Duration.ofMinutes(1);
    protected Duration asyncJobsGlobalLockPollRate = Duration.ofMillis(500);
    protected Duration asyncJobsGlobalLockForceAcquireAfter = Duration.ofMinutes(10);
    protected Duration timerLockWaitTime = Duration.ofMinutes(1);
    protected Duration timerLockPollRate = Duration.ofMillis(500);
    protected Duration timerLockForceAcquireAfter = Duration.ofMinutes(10);

    protected int resetExpiredJobsInterval = 60 * 1000;
    protected int resetExpiredJobsPageSize = 3;

    // Job queue used when async executor is not yet started and jobs are already added.
    // This is mainly used for testing purpose.
    protected LinkedList<JobInfo> temporaryJobQueue = new LinkedList<>();

    protected JobServiceConfiguration jobServiceConfiguration;

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
        jobServiceConfiguration.getCommandExecutor().execute(new UnacquireOwnedJobsCmd(lockOwner, tenantId, jobServiceConfiguration));
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

        LOGGER.info("Starting up the async job executor [{}].", getClass().getName());

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
        if (timerRunnableNeeded && timerJobRunnable == null) {
            timerJobRunnable = new AcquireTimerJobsRunnable(this, jobServiceConfiguration.getJobManager(),
                timerLifecycleListener, globalAcquireLockEnabled, globalAcquireLockPrefix, moveTimerExecutorPoolSize);

            timerJobRunnable.setLockWaitTime(timerLockWaitTime);
            timerJobRunnable.setLockPollRate(timerLockPollRate);
            timerJobRunnable.setLockForceAcquireAfter(timerLockForceAcquireAfter);
        }

        JobInfoEntityManager<? extends JobInfoEntity> jobEntityManagerToUse = jobEntityManager != null
                ? jobEntityManager : jobServiceConfiguration.getJobEntityManager();

        if (resetExpiredJobsRunnable == null) {
            String resetRunnableName = resetExpiredRunnableName != null ?
                    resetExpiredRunnableName : "flowable-" + getJobServiceConfiguration().getEngineName() + "-reset-expired-jobs";
            resetExpiredJobsRunnable = createResetExpiredJobsRunnable(resetRunnableName);
        }

        if (!isMessageQueueMode && asyncJobsDueRunnable == null) {
            String acquireJobsRunnableName = acquireRunnableThreadName != null ?
                    acquireRunnableThreadName : "flowable-" + getJobServiceConfiguration().getEngineName() + "-acquire-async-jobs";
            asyncJobsDueRunnable = new AcquireAsyncJobsDueRunnable(acquireJobsRunnableName, this, jobEntityManagerToUse,
                asyncJobsDueLifecycleListener, globalAcquireLockEnabled, globalAcquireLockPrefix);

            asyncJobsDueRunnable.setLockWaitTime(asyncJobsGlobalLockWaitTime);
            asyncJobsDueRunnable.setLockPollRate(asyncJobsGlobalLockPollRate);
            asyncJobsDueRunnable.setLockForceAcquireAfter(asyncJobsGlobalLockForceAcquireAfter);
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
        LOGGER.info("Shutting down the async job executor [{}].", getClass().getName());

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
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    @Override
    public int getTimerLockTimeInMillis() {
        return timerLockTimeInMillis;
    }

    @Override
    public void setTimerLockTimeInMillis(int timerLockTimeInMillis) {
        this.timerLockTimeInMillis = timerLockTimeInMillis;
    }

    @Override
    public int getAsyncJobLockTimeInMillis() {
        return asyncJobLockTimeInMillis;
    }

    @Override
    public void setAsyncJobLockTimeInMillis(int asyncJobLockTimeInMillis) {
        this.asyncJobLockTimeInMillis = asyncJobLockTimeInMillis;
    }

    public int getMoveTimerExecutorPoolSize() {
        return moveTimerExecutorPoolSize;
    }

    public void setMoveTimerExecutorPoolSize(int moveTimerExecutorPoolSize) {
        this.moveTimerExecutorPoolSize = moveTimerExecutorPoolSize;
    }

    @Override
    public int getMaxTimerJobsPerAcquisition() {
        return maxTimerJobsPerAcquisition;
    }

    @Override
    public void setMaxTimerJobsPerAcquisition(int maxTimerJobsPerAcquisition) {
        this.maxTimerJobsPerAcquisition = maxTimerJobsPerAcquisition;
    }

    @Override
    public int getMaxAsyncJobsDuePerAcquisition() {
        return maxAsyncJobsDuePerAcquisition;
    }

    @Override
    public void setMaxAsyncJobsDuePerAcquisition(int maxAsyncJobsDuePerAcquisition) {
        this.maxAsyncJobsDuePerAcquisition = maxAsyncJobsDuePerAcquisition;
    }

    @Override
    public int getDefaultTimerJobAcquireWaitTimeInMillis() {
        return defaultTimerJobAcquireWaitTimeInMillis;
    }

    @Override
    public void setDefaultTimerJobAcquireWaitTimeInMillis(int defaultTimerJobAcquireWaitTimeInMillis) {
        this.defaultTimerJobAcquireWaitTimeInMillis = defaultTimerJobAcquireWaitTimeInMillis;
    }

    @Override
    public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
        return defaultAsyncJobAcquireWaitTimeInMillis;
    }

    @Override
    public void setDefaultAsyncJobAcquireWaitTimeInMillis(int defaultAsyncJobAcquireWaitTimeInMillis) {
        this.defaultAsyncJobAcquireWaitTimeInMillis = defaultAsyncJobAcquireWaitTimeInMillis;
    }

    public void setTimerJobRunnable(AcquireTimerJobsRunnable timerJobRunnable) {
        this.timerJobRunnable = timerJobRunnable;
    }

    @Override
    public int getDefaultQueueSizeFullWaitTimeInMillis() {
        return defaultQueueSizeFullWaitTime;
    }

    @Override
    public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTime) {
        this.defaultQueueSizeFullWaitTime = defaultQueueSizeFullWaitTime;
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
        return timerRunnableNeeded;
    }

    public void setTimerRunnableNeeded(boolean timerRunnableNeeded) {
        this.timerRunnableNeeded = timerRunnableNeeded;
    }

    public AcquireTimerLifecycleListener getTimerLifecycleListener() {
        return timerLifecycleListener;
    }

    public void setTimerLifecycleListener(AcquireTimerLifecycleListener timerLifecycleListener) {
        this.timerLifecycleListener = timerLifecycleListener;
    }

    public boolean isGlobalAcquireLockEnabled() {
        return globalAcquireLockEnabled;
    }

    public void setGlobalAcquireLockEnabled(boolean globalAcquireLockEnabled) {
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
        if (timerJobRunnable != null) {
            timerJobRunnable.setGlobalAcquireLockEnabled(globalAcquireLockEnabled);
        }

        if (asyncJobsDueRunnable != null) {
            asyncJobsDueRunnable.setGlobalAcquireLockEnabled(globalAcquireLockEnabled);
        }
    }

    public String getGlobalAcquireLockPrefix() {
        return globalAcquireLockPrefix;
    }

    public void setGlobalAcquireLockPrefix(String globalAcquireLockPrefix) {
        this.globalAcquireLockPrefix = globalAcquireLockPrefix;
    }

    public Duration getAsyncJobsGlobalLockWaitTime() {
        return asyncJobsGlobalLockWaitTime;
    }

    public void setAsyncJobsGlobalLockWaitTime(Duration asyncJobsGlobalLockWaitTime) {
        this.asyncJobsGlobalLockWaitTime = asyncJobsGlobalLockWaitTime;
        if (asyncJobsDueRunnable != null) {
            asyncJobsDueRunnable.setLockWaitTime(asyncJobsGlobalLockWaitTime);
        }
    }

    public Duration getAsyncJobsGlobalLockPollRate() {
        return asyncJobsGlobalLockPollRate;
    }

    public void setAsyncJobsGlobalLockPollRate(Duration asyncJobsGlobalLockPollRate) {
        this.asyncJobsGlobalLockPollRate = asyncJobsGlobalLockPollRate;
        if (asyncJobsDueRunnable != null) {
            asyncJobsDueRunnable.setLockPollRate(asyncJobsGlobalLockPollRate);
        }
    }

    public Duration getAsyncJobsGlobalLockForceAcquireAfter() {
        return asyncJobsGlobalLockForceAcquireAfter;
    }

    public void setAsyncJobsGlobalLockForceAcquireAfter(Duration asyncJobsGlobalLockForceAcquireAfter) {
        this.asyncJobsGlobalLockForceAcquireAfter = asyncJobsGlobalLockForceAcquireAfter;
        if (asyncJobsDueRunnable != null) {
            asyncJobsDueRunnable.setLockForceAcquireAfter(asyncJobsGlobalLockForceAcquireAfter);
        }
    }

    public Duration getTimerLockWaitTime() {
        return timerLockWaitTime;
    }

    public void setTimerLockWaitTime(Duration timerLockWaitTime) {
        this.timerLockWaitTime = timerLockWaitTime;
        if (timerJobRunnable != null) {
            timerJobRunnable.setLockWaitTime(timerLockWaitTime);
        }
    }

    public Duration getTimerLockPollRate() {
        return timerLockPollRate;
    }

    public void setTimerLockPollRate(Duration timerLockPollRate) {
        this.timerLockPollRate = timerLockPollRate;
        if (timerJobRunnable != null) {
            timerJobRunnable.setLockPollRate(timerLockPollRate);
        }
    }

    public Duration getTimerLockForceAcquireAfter() {
        return timerLockForceAcquireAfter;
    }

    public void setTimerLockForceAcquireAfter(Duration timerLockForceAcquireAfter) {
        this.timerLockForceAcquireAfter = timerLockForceAcquireAfter;
        if (timerJobRunnable != null) {
            timerJobRunnable.setLockForceAcquireAfter(timerLockForceAcquireAfter);
        }
    }

    public void setAcquireRunnableThreadName(String acquireRunnableThreadName) {
        this.acquireRunnableThreadName = acquireRunnableThreadName;
    }

    public void setJobEntityManager(JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {
        this.jobEntityManager = jobEntityManager;
    }
    
    public void setResetExpiredRunnableName(String resetExpiredRunnableName) {
        this.resetExpiredRunnableName = resetExpiredRunnableName;
    }

    public void setResetExpiredJobsRunnable(ResetExpiredJobsRunnable resetExpiredJobsRunnable) {
        this.resetExpiredJobsRunnable = resetExpiredJobsRunnable;
    }

    @Override
    public int getRetryWaitTimeInMillis() {
        return retryWaitTimeInMillis;
    }

    @Override
    public void setRetryWaitTimeInMillis(int retryWaitTimeInMillis) {
        this.retryWaitTimeInMillis = retryWaitTimeInMillis;
    }

    @Override
    public int getResetExpiredJobsInterval() {
        return resetExpiredJobsInterval;
    }

    @Override
    public void setResetExpiredJobsInterval(int resetExpiredJobsInterval) {
        this.resetExpiredJobsInterval = resetExpiredJobsInterval;
    }

    @Override
    public int getResetExpiredJobsPageSize() {
        return resetExpiredJobsPageSize;
    }

    @Override
    public void setResetExpiredJobsPageSize(int resetExpiredJobsPageSize) {
        this.resetExpiredJobsPageSize = resetExpiredJobsPageSize;
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
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
}
