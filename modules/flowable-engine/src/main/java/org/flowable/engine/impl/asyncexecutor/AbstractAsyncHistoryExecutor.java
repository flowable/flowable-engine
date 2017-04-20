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
package org.flowable.engine.impl.asyncexecutor;

import java.util.UUID;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.UnacquireOwnedHistoryJobsCmd;
import org.flowable.engine.runtime.HistoryJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class AbstractAsyncHistoryExecutor implements AsyncHistoryExecutor {

    private static Logger log = LoggerFactory.getLogger(AbstractAsyncHistoryExecutor.class);

    protected AcquireAsyncHistoryJobsDueRunnable asyncHistoryJobsDueRunnable;
    protected ResetExpiredHistoryJobsRunnable resetExpiredHistoryJobsRunnable;

    protected ExecuteAsyncHistoryRunnableFactory executeAsyncHistoryRunnableFactory;

    protected boolean isAutoActivate;
    protected boolean isActive;
    protected boolean isMessageQueueMode;

    protected int maxAsyncJobsDuePerAcquisition = 1;
    protected int defaultAsyncJobAcquireWaitTimeInMillis = 10 * 1000;
    protected int defaultQueueSizeFullWaitTime;

    protected String lockOwner = UUID.randomUUID().toString();
    protected int asyncJobLockTimeInMillis = 5 * 60 * 1000;
    protected int retryWaitTimeInMillis = 500;

    protected int resetExpiredJobsInterval = 60 * 1000;
    protected int resetExpiredJobsPageSize = 3;

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public boolean executeAsyncJob(final HistoryJob job) {
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
            throw new FlowableException("History async executor is not active");
        }
    }

    protected abstract boolean executeAsyncJob(final HistoryJob job, Runnable runnable);

    protected void unlockOwnedJobs() {
        processEngineConfiguration.getCommandExecutor().execute(new UnacquireOwnedHistoryJobsCmd(lockOwner, null));
    }

    protected Runnable createRunnableForJob(final HistoryJob job) {
        if (executeAsyncHistoryRunnableFactory == null) {
            return new ExecuteAsyncHistoryRunnable(job, processEngineConfiguration);
        } else {
            return executeAsyncHistoryRunnableFactory.createExecuteAsyncHistoryRunnable(job, processEngineConfiguration);
        }
    }

    /** Starts the async executor */
    public void start() {
        if (isActive) {
            return;
        }

        isActive = true;

        log.info("Starting up the async history job executor [{}].", getClass().getName());

        initializeRunnables();
        startAdditionalComponents();
    }

    protected void initializeRunnables() {
        if (resetExpiredHistoryJobsRunnable == null) {
            resetExpiredHistoryJobsRunnable = new ResetExpiredHistoryJobsRunnable(this);
        }

        if (!isMessageQueueMode && asyncHistoryJobsDueRunnable == null) {
            asyncHistoryJobsDueRunnable = new AcquireAsyncHistoryJobsDueRunnable(this);
        }
    }

    protected abstract void startAdditionalComponents();

    /** Shuts down the whole job executor */
    public synchronized void shutdown() {
        if (!isActive) {
            return;
        }
        log.info("Shutting down the async history job executor [{}].", getClass().getName());

        stopRunnables();
        shutdownAdditionalComponents();

        isActive = false;
    }

    protected void stopRunnables() {
        if (asyncHistoryJobsDueRunnable != null) {
            asyncHistoryJobsDueRunnable.stop();
        }
        if (resetExpiredHistoryJobsRunnable != null) {
            resetExpiredHistoryJobsRunnable.stop();
        }

        asyncHistoryJobsDueRunnable = null;
        resetExpiredHistoryJobsRunnable = null;
    }

    protected abstract void shutdownAdditionalComponents();

    /* getters and setters */

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public boolean isAutoActivate() {
        return isAutoActivate;
    }

    public void setAutoActivate(boolean isAutoActivate) {
        this.isAutoActivate = isAutoActivate;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isMessageQueueMode() {
        return isMessageQueueMode;
    }

    public void setMessageQueueMode(boolean isMessageQueueMode) {
        this.isMessageQueueMode = isMessageQueueMode;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public int getAsyncJobLockTimeInMillis() {
        return asyncJobLockTimeInMillis;
    }

    public void setAsyncJobLockTimeInMillis(int asyncJobLockTimeInMillis) {
        this.asyncJobLockTimeInMillis = asyncJobLockTimeInMillis;
    }

    public int getMaxAsyncJobsDuePerAcquisition() {
        return maxAsyncJobsDuePerAcquisition;
    }

    public void setMaxAsyncJobsDuePerAcquisition(int maxAsyncJobsDuePerAcquisition) {
        this.maxAsyncJobsDuePerAcquisition = maxAsyncJobsDuePerAcquisition;
    }

    public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
        return defaultAsyncJobAcquireWaitTimeInMillis;
    }

    public void setDefaultAsyncJobAcquireWaitTimeInMillis(int defaultAsyncJobAcquireWaitTimeInMillis) {
        this.defaultAsyncJobAcquireWaitTimeInMillis = defaultAsyncJobAcquireWaitTimeInMillis;
    }

    public int getDefaultQueueSizeFullWaitTimeInMillis() {
        return defaultQueueSizeFullWaitTime;
    }

    public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTime) {
        this.defaultQueueSizeFullWaitTime = defaultQueueSizeFullWaitTime;
    }

    public void setAsyncHistoryJobsDueRunnable(AcquireAsyncHistoryJobsDueRunnable asyncHistoryJobsDueRunnable) {
        this.asyncHistoryJobsDueRunnable = asyncHistoryJobsDueRunnable;
    }

    public void setResetExpiredHistoryJobsRunnable(ResetExpiredHistoryJobsRunnable resetExpiredHistoryJobsRunnable) {
        this.resetExpiredHistoryJobsRunnable = resetExpiredHistoryJobsRunnable;
    }

    public int getRetryWaitTimeInMillis() {
        return retryWaitTimeInMillis;
    }

    public void setRetryWaitTimeInMillis(int retryWaitTimeInMillis) {
        this.retryWaitTimeInMillis = retryWaitTimeInMillis;
    }

    public int getResetExpiredJobsInterval() {
        return resetExpiredJobsInterval;
    }

    public void setResetExpiredJobsInterval(int resetExpiredJobsInterval) {
        this.resetExpiredJobsInterval = resetExpiredJobsInterval;
    }

    public int getResetExpiredJobsPageSize() {
        return resetExpiredJobsPageSize;
    }

    public void setResetExpiredJobsPageSize(int resetExpiredJobsPageSize) {
        this.resetExpiredJobsPageSize = resetExpiredJobsPageSize;
    }

    public ExecuteAsyncHistoryRunnableFactory getExecuteAsyncHistoryRunnableFactory() {
        return executeAsyncHistoryRunnableFactory;
    }

    public void setExecuteAsyncHistoryRunnableFactory(ExecuteAsyncHistoryRunnableFactory executeAsyncHistoryRunnableFactory) {
        this.executeAsyncHistoryRunnableFactory = executeAsyncHistoryRunnableFactory;
    }

}
