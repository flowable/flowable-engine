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

import java.util.concurrent.RejectedExecutionException;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class DefaultAsyncJobExecutor extends AbstractAsyncExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAsyncJobExecutor.class);

    /**
     * Thread responsible for async job acquisition.
     */
    protected Thread asyncJobAcquisitionThread;

    /**
     * Thread responsible for timer job acquisition.
     */
    protected Thread timerJobAcquisitionThread;

    /**
     * Thread responsible for resetting the expired jobs.
     */
    protected Thread resetExpiredJobThread;

    /**
     * The async task executor used for job execution.
     */
    protected AsyncTaskExecutor taskExecutor;
    protected boolean shutdownTaskExecutor;

    public DefaultAsyncJobExecutor() {
        super();
    }

    public DefaultAsyncJobExecutor(AsyncJobExecutorConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected boolean executeAsyncJob(final JobInfo job, Runnable runnable) {
        try {
            taskExecutor.execute(runnable);
            return true;

        } catch (RejectedExecutionException e) {
            sendRejectedEvent(job);
            unacquireJobAfterRejection(job);

            // Job queue full, returning false so (if wanted) the acquiring can be throttled
            return false;
        }
    }

    protected void sendRejectedEvent(JobInfo job) {
        FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(
                FlowableEngineEventType.JOB_REJECTED, job), jobServiceConfiguration.getEngineName());
        }
    }

    protected void unacquireJobAfterRejection(final JobInfo job) {

        // When a RejectedExecutionException is caught, this means that the
        // queue for holding the jobs that are to be executed is full and can't store more.
        // The job is now 'unlocked', meaning that the lock owner/time is set to null,
        // so other executors can pick the job up (or this async executor, the next time the
        // acquire query is executed.

        CommandConfig commandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
        jobServiceConfiguration.getCommandExecutor().execute(commandConfig, new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                jobServiceConfiguration.getJobManager().unacquire(job);
                return null;
            }
        });
    }

    @Override
    protected void startAdditionalComponents() {
        if (configuration.isUnlockOwnedJobs()) {
            unlockOwnedJobs();
        }

        if (!isMessageQueueMode) {
            initAsyncJobExecutionThreadPool();
            startJobAcquisitionThread();
        }

        if (configuration.isTimerRunnableNeeded()) {
            startTimerAcquisitionThread();
        }
        startResetExpiredJobsThread();
    }

    @Override
    protected void shutdownAdditionalComponents() {
        stopResetExpiredJobsThread();
        stopTimerAcquisitionThread();
        stopJobAcquisitionThread();
        stopExecutingAsyncJobs();

        if (configuration.isUnlockOwnedJobs()) {
            unlockOwnedJobs();
        }

    }

    @Override
    protected ResetExpiredJobsRunnable createResetExpiredJobsRunnable(String resetRunnableName) {
        return new ResetExpiredJobsRunnable(resetRunnableName, this,
                jobServiceConfiguration.getJobEntityManager(),
                jobServiceConfiguration.getTimerJobEntityManager(),
                jobServiceConfiguration.getExternalWorkerJobEntityManager()
        );
    }

    protected void initAsyncJobExecutionThreadPool() {
        if (taskExecutor == null) {
            // This is for backwards compatibility
            // If there is no task executor then use the Default one and start it immediately.
            DefaultAsyncTaskExecutor defaultAsyncTaskExecutor = new DefaultAsyncTaskExecutor();
            defaultAsyncTaskExecutor.start();
            this.taskExecutor = defaultAsyncTaskExecutor;
            this.shutdownTaskExecutor = true;
        }
    }

    protected void stopExecutingAsyncJobs() {
        if (taskExecutor != null && shutdownTaskExecutor) {
            taskExecutor.shutdown();
            taskExecutor = null;
        }
    }

    /** Starts the acquisition thread */
    protected void startJobAcquisitionThread() {
        if (configuration.isAsyncJobAcquisitionEnabled()) {
            if (asyncJobAcquisitionThread == null) {
                asyncJobAcquisitionThread = new Thread(asyncJobsDueRunnable);
            }
            asyncJobAcquisitionThread.start();
        }
    }

    protected void startTimerAcquisitionThread() {
        if (configuration.isTimerJobAcquisitionEnabled()) {
            if (timerJobAcquisitionThread == null) {
                timerJobAcquisitionThread = new Thread(timerJobRunnable);
            }
            timerJobAcquisitionThread.start();
        }
    }

    /** Stops the acquisition thread */
    protected void stopJobAcquisitionThread() {
        if (asyncJobAcquisitionThread != null) {
            try {
                asyncJobAcquisitionThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the async job acquisition thread to terminate", e);
            }
            asyncJobAcquisitionThread = null;
        }
    }

    protected void stopTimerAcquisitionThread() {
        if (timerJobAcquisitionThread != null) {
            try {
                timerJobAcquisitionThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the timer job acquisition thread to terminate", e);
            }
            timerJobAcquisitionThread = null;
        }
    }

    /** Starts the reset expired jobs thread */
    protected void startResetExpiredJobsThread() {
        if (configuration.isResetExpiredJobEnabled()) {
            if (resetExpiredJobThread == null) {
                resetExpiredJobThread = new Thread(resetExpiredJobsRunnable);
            }
            resetExpiredJobThread.start();
        }
    }

    /** Stops the reset expired jobs thread */
    protected void stopResetExpiredJobsThread() {
        if (resetExpiredJobThread != null) {
            try {
                resetExpiredJobThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the reset expired jobs thread to terminate", e);
            }

            resetExpiredJobThread = null;
        }
    }

    public boolean isAsyncJobAcquisitionEnabled() {
        return configuration.isAsyncJobAcquisitionEnabled();
    }

    public void setAsyncJobAcquisitionEnabled(boolean isAsyncJobAcquisitionEnabled) {
        configuration.setAsyncJobAcquisitionEnabled(isAsyncJobAcquisitionEnabled);
    }

    public boolean isTimerJobAcquisitionEnabled() {
        return configuration.isTimerJobAcquisitionEnabled();
    }

    public void setTimerJobAcquisitionEnabled(boolean isTimerJobAcquisitionEnabled) {
        configuration.setTimerJobAcquisitionEnabled(isTimerJobAcquisitionEnabled);
    }

    public boolean isResetExpiredJobEnabled() {
        return configuration.isResetExpiredJobEnabled();
    }

    public void setResetExpiredJobEnabled(boolean isResetExpiredJobEnabled) {
        configuration.setResetExpiredJobEnabled(isResetExpiredJobEnabled);
    }

    public Thread getTimerJobAcquisitionThread() {
        return timerJobAcquisitionThread;
    }

    public void setTimerJobAcquisitionThread(Thread timerJobAcquisitionThread) {
        this.timerJobAcquisitionThread = timerJobAcquisitionThread;
    }

    public Thread getAsyncJobAcquisitionThread() {
        return asyncJobAcquisitionThread;
    }

    public void setAsyncJobAcquisitionThread(Thread asyncJobAcquisitionThread) {
        this.asyncJobAcquisitionThread = asyncJobAcquisitionThread;
    }

    public Thread getResetExpiredJobThread() {
        return resetExpiredJobThread;
    }

    public void setResetExpiredJobThread(Thread resetExpiredJobThread) {
        this.resetExpiredJobThread = resetExpiredJobThread;
    }

    public boolean isUnlockOwnedJobs() {
        return configuration.isUnlockOwnedJobs();
    }

    public void setUnlockOwnedJobs(boolean unlockOwnedJobs) {
        configuration.setUnlockOwnedJobs(unlockOwnedJobs);
    }

    @Override
    public AsyncTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @Override
    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }
}
