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
import java.util.UUID;

/**
 * @author Filip Hrisafov
 */
public class AsyncJobExecutorConfiguration {

    /**
     * Whether the thread for acquiring async jobs will be started.
     * This can be used to boot up engine instances that still execute jobs originating from this instance itself,
     * but don't fetch new jobs themselves.
     */
    private boolean asyncJobAcquisitionEnabled = true;
    /**
     * Whether the thread for acquiring timer jobs will be started.
     */
    private boolean timerJobAcquisitionEnabled = true;
    /**
     * Whether the thread for resetting expired jobs will be started.
     */
    private boolean resetExpiredJobEnabled = true;

    /**
     * Whether to unlock jobs that are owned by this executor (have the same lockOwner) at startup or shutdown.
     */
    private boolean unlockOwnedJobs = true;

    /**
     * Whether runnable for acquiring  timer jobs should be enabled
     */
    private boolean timerRunnableNeeded = true; // default true for backwards compatibility (History Async executor came later)

    /**
     * The name of the thread in which async jobs will be acquired.
     */
    private String acquireRunnableThreadName;
    /**
     * The name of the thread in which expired jobs will be reset.
     */
    private String resetExpiredRunnableName;

    /**
     * How large should the thread pool for moving timer jobs be.
     */
    private int moveTimerExecutorPoolSize = 4;
    /**
     * How many timer jobs should be acquired in one acquisition.
     */
    private int maxTimerJobsPerAcquisition = 512;
    /**
     * How many async / history jobs should be acquired in one acquisition.
     */
    private int maxAsyncJobsDuePerAcquisition = 512;

    /**
     * The time the timer acquisition thread should wait before executing the next acquire logic.
     */
    private Duration defaultTimerJobAcquireWaitTime = Duration.ofSeconds(10);
    /**
     * The time the async job acquisition thread should wait before executing the next acquire logic.
     */
    private Duration defaultAsyncJobAcquireWaitTime = Duration.ofSeconds(10);
    /**
     * The time the acquisition thread should wait when the queue is full before executing the next acquire logic.
     */
    private Duration defaultQueueSizeFullWaitTime = Duration.ofSeconds(5);

    /**
     * The value that should be used when locking async / timer jobs.
     * <p>
     * When a job is acquired, it is locked so other async executors can't lock and execute it.
     * While doing this, the 'name' of the lock owner is written into a column of the job.
     * <p>
     * By default, a random UUID will be generated when the executor is created.
     * <p>
     * It is important that each async executor instance in a cluster of Flowable engines has a different name!
     */
    private String lockOwner = UUID.randomUUID().toString();

    /**
     * The amount of time a timer job is locked when acquired.
     * During this period of time, no other async executor will try to acquire and lock this job.
     */
    private Duration timerLockTime = Duration.ofHours(1);
    /**
     * The amount of time an async job is locked when acquired.
     * During this period of time, no other async executor will try to acquire and lock this job.
     */
    private Duration asyncJobLockTime = Duration.ofHours(1);

    /**
     * Whether global acquire lock should be used.
     */
    protected boolean globalAcquireLockEnabled;
    /**
     * The prefix that the runnable should use for the global acquire lock.
     * Setting a different prefix allows differentiating different engines / executors without them competing for the same lock.
     */
    protected String globalAcquireLockPrefix = "";

    /**
     * The amount of time the async job acquire thread should wait to acquire the global lock.
     */
    private Duration asyncJobsGlobalLockWaitTime = Duration.ofMinutes(1);
    /**
     * The poll rate of the async job acquire thread for checking if the global lock has been released.
     */
    private Duration asyncJobsGlobalLockPollRate = Duration.ofMillis(500);
    /**
     * The amount of time after the last global lock acquire time the lock will be forcefully acquired.
     * This means that if for some reason another node did not release the lock properly because it crashed
     * another node will be able to acquire the lock.
     */
    private Duration asyncJobsGlobalLockForceAcquireAfter = Duration.ofMinutes(10);
    /**
     * The amount of time the timer job acquire thread should wait to acquire the global lock.
     */
    private Duration timerLockWaitTime = Duration.ofMinutes(1);
    /**
     * The poll rate of the timer job acquire thread for checking if the global lock has been released.
     */
    private Duration timerLockPollRate = Duration.ofMillis(500);
    /**
     * The amount of time after the last global lock acquire time the lock will be forcefully acquired.
     * This means that if for some reason another node did not release the lock properly because it crashed
     * another node will be able to acquire the lock.
     */
    private Duration timerLockForceAcquireAfter = Duration.ofMinutes(10);

    /**
     * The time the reset expired jobs thread should wait before executing the next reset logic.
     * Expired jobs are jobs that were locked (a lock owner + time was written by some executor, but the job was never completed).
     * During such a check, jobs that are expired are again made available, meaning the lock owner and lock time will be removed.
     * Other executors will now be able to pick it up.
     * A job is deemed expired if the current time has passed the lock time.
     */
    private Duration resetExpiredJobsInterval = Duration.ofMinutes(1);
    /**
     * The amount of expired jobs that should be rest in one cycle.
     */
    private int resetExpiredJobsPageSize = 3;

    /**
     * The id of the tenant that the async executor should use when unlocking jobs.
     */
    private String tenantId;

    public boolean isAsyncJobAcquisitionEnabled() {
        return asyncJobAcquisitionEnabled;
    }

    public void setAsyncJobAcquisitionEnabled(boolean asyncJobAcquisitionEnabled) {
        this.asyncJobAcquisitionEnabled = asyncJobAcquisitionEnabled;
    }

    public boolean isTimerJobAcquisitionEnabled() {
        return timerJobAcquisitionEnabled;
    }

    public void setTimerJobAcquisitionEnabled(boolean timerJobAcquisitionEnabled) {
        this.timerJobAcquisitionEnabled = timerJobAcquisitionEnabled;
    }

    public boolean isResetExpiredJobEnabled() {
        return resetExpiredJobEnabled;
    }

    public void setResetExpiredJobEnabled(boolean resetExpiredJobEnabled) {
        this.resetExpiredJobEnabled = resetExpiredJobEnabled;
    }

    public boolean isUnlockOwnedJobs() {
        return unlockOwnedJobs;
    }

    public void setUnlockOwnedJobs(boolean unlockOwnedJobs) {
        this.unlockOwnedJobs = unlockOwnedJobs;
    }

    public boolean isTimerRunnableNeeded() {
        return timerRunnableNeeded;
    }

    public void setTimerRunnableNeeded(boolean timerRunnableNeeded) {
        this.timerRunnableNeeded = timerRunnableNeeded;
    }

    public String getAcquireRunnableThreadName() {
        return acquireRunnableThreadName;
    }

    public void setAcquireRunnableThreadName(String acquireRunnableThreadName) {
        this.acquireRunnableThreadName = acquireRunnableThreadName;
    }

    public String getResetExpiredRunnableName() {
        return resetExpiredRunnableName;
    }

    public void setResetExpiredRunnableName(String resetExpiredRunnableName) {
        this.resetExpiredRunnableName = resetExpiredRunnableName;
    }

    public int getMoveTimerExecutorPoolSize() {
        return moveTimerExecutorPoolSize;
    }

    public void setMoveTimerExecutorPoolSize(int moveTimerExecutorPoolSize) {
        this.moveTimerExecutorPoolSize = moveTimerExecutorPoolSize;
    }

    public int getMaxTimerJobsPerAcquisition() {
        return maxTimerJobsPerAcquisition;
    }

    public void setMaxTimerJobsPerAcquisition(int maxTimerJobsPerAcquisition) {
        this.maxTimerJobsPerAcquisition = maxTimerJobsPerAcquisition;
    }

    public int getMaxAsyncJobsDuePerAcquisition() {
        return maxAsyncJobsDuePerAcquisition;
    }

    public void setMaxAsyncJobsDuePerAcquisition(int maxAsyncJobsDuePerAcquisition) {
        this.maxAsyncJobsDuePerAcquisition = maxAsyncJobsDuePerAcquisition;
    }

    public Duration getDefaultTimerJobAcquireWaitTime() {
        return defaultTimerJobAcquireWaitTime;
    }

    public void setDefaultTimerJobAcquireWaitTime(Duration defaultTimerJobAcquireWaitTime) {
        this.defaultTimerJobAcquireWaitTime = defaultTimerJobAcquireWaitTime;
    }

    // This is for backwards compatibility so property exists like it used to exist
    @Deprecated
    public void setDefaultTimerJobAcquireWaitTimeInMillis(int defaultTimerJobAcquireWaitTimeInMillis) {
        this.defaultTimerJobAcquireWaitTime = Duration.ofMillis(defaultTimerJobAcquireWaitTimeInMillis);
    }

    public Duration getDefaultAsyncJobAcquireWaitTime() {
        return defaultAsyncJobAcquireWaitTime;
    }

    public void setDefaultAsyncJobAcquireWaitTime(Duration defaultAsyncJobAcquireWaitTime) {
        this.defaultAsyncJobAcquireWaitTime = defaultAsyncJobAcquireWaitTime;
    }

    // This is for backwards compatibility so property exists like it used to exist
    @Deprecated
    public void setDefaultAsyncJobAcquireWaitTimeInMillis(int defaultAsyncJobAcquireWaitTimeInMillis) {
        this.defaultAsyncJobAcquireWaitTime = Duration.ofMillis(defaultAsyncJobAcquireWaitTimeInMillis);
    }

    public Duration getDefaultQueueSizeFullWaitTime() {
        return defaultQueueSizeFullWaitTime;
    }

    public void setDefaultQueueSizeFullWaitTime(Duration defaultQueueSizeFullWaitTime) {
        this.defaultQueueSizeFullWaitTime = defaultQueueSizeFullWaitTime;
    }

    // This is for backwards compatibility so property exists like it used to exist
    @Deprecated
    public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTimeInMillis) {
        this.defaultQueueSizeFullWaitTime = Duration.ofMillis(defaultQueueSizeFullWaitTimeInMillis);
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public Duration getTimerLockTime() {
        return timerLockTime;
    }

    public void setTimerLockTime(Duration timerLockTime) {
        this.timerLockTime = timerLockTime;
    }

    // This is for backwards compatibility so property exists like it used to exist
    @Deprecated
    public void setTimerLockTimeInMillis(int timerLockTimeInMillis) {
        this.timerLockTime = Duration.ofMillis(timerLockTimeInMillis);
    }

    public Duration getAsyncJobLockTime() {
        return asyncJobLockTime;
    }

    public void setAsyncJobLockTime(Duration asyncJobLockTime) {
        this.asyncJobLockTime = asyncJobLockTime;
    }

    // This is for backwards compatibility so property exists like it used to exist
    @Deprecated
    public void setAsyncJobLockTimeInMillis(int asyncJobLockTimeInMillis) {
        this.asyncJobLockTime = Duration.ofMillis(asyncJobLockTimeInMillis);
    }

    public boolean isGlobalAcquireLockEnabled() {
        return globalAcquireLockEnabled;
    }

    public void setGlobalAcquireLockEnabled(boolean globalAcquireLockEnabled) {
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
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
    }

    public Duration getAsyncJobsGlobalLockPollRate() {
        return asyncJobsGlobalLockPollRate;
    }

    public void setAsyncJobsGlobalLockPollRate(Duration asyncJobsGlobalLockPollRate) {
        this.asyncJobsGlobalLockPollRate = asyncJobsGlobalLockPollRate;
    }

    public Duration getAsyncJobsGlobalLockForceAcquireAfter() {
        return asyncJobsGlobalLockForceAcquireAfter;
    }

    public void setAsyncJobsGlobalLockForceAcquireAfter(Duration asyncJobsGlobalLockForceAcquireAfter) {
        this.asyncJobsGlobalLockForceAcquireAfter = asyncJobsGlobalLockForceAcquireAfter;
    }

    public Duration getTimerLockWaitTime() {
        return timerLockWaitTime;
    }

    public void setTimerLockWaitTime(Duration timerLockWaitTime) {
        this.timerLockWaitTime = timerLockWaitTime;
    }

    public Duration getTimerLockPollRate() {
        return timerLockPollRate;
    }

    public void setTimerLockPollRate(Duration timerLockPollRate) {
        this.timerLockPollRate = timerLockPollRate;
    }

    public Duration getTimerLockForceAcquireAfter() {
        return timerLockForceAcquireAfter;
    }

    public void setTimerLockForceAcquireAfter(Duration timerLockForceAcquireAfter) {
        this.timerLockForceAcquireAfter = timerLockForceAcquireAfter;
    }

    public Duration getResetExpiredJobsInterval() {
        return resetExpiredJobsInterval;
    }

    public void setResetExpiredJobsInterval(Duration resetExpiredJobsInterval) {
        this.resetExpiredJobsInterval = resetExpiredJobsInterval;
    }

    public int getResetExpiredJobsPageSize() {
        return resetExpiredJobsPageSize;
    }

    public void setResetExpiredJobsPageSize(int resetExpiredJobsPageSize) {
        this.resetExpiredJobsPageSize = resetExpiredJobsPageSize;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
