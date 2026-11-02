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

package org.flowable.job.service.impl.asyncexecutor.multitenant;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AbstractAsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AsyncExecutor} that has one {@link AsyncExecutor} per tenant. So each tenant has its own acquiring threads and it's own threadpool for executing jobs.
 * 
 * @author Joram Barrez
 */
public class ExecutorPerTenantAsyncExecutor implements TenantAwareAsyncExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorPerTenantAsyncExecutor.class);

    protected TenantInfoHolder tenantInfoHolder;
    protected TenantAwareAsyncExecutorFactory tenantAwareAyncExecutorFactory;

    protected AsyncExecutor nullTenantIdAsyncExecutor; // a concurrent hashmap doesn't allow for null keys, hence why it needs to be stored separately.
    protected Map<String, AsyncExecutor> tenantExecutors = new ConcurrentHashMap<>();

    protected JobServiceConfiguration jobServiceConfiguration;
    protected boolean active;
    protected boolean autoActivate;

    public ExecutorPerTenantAsyncExecutor(TenantInfoHolder tenantInfoHolder) {
        this(tenantInfoHolder, null);
    }

    public ExecutorPerTenantAsyncExecutor(TenantInfoHolder tenantInfoHolder, TenantAwareAsyncExecutorFactory tenantAwareAyncExecutorFactory) {
        this.tenantInfoHolder = tenantInfoHolder;
        this.tenantAwareAyncExecutorFactory = tenantAwareAyncExecutorFactory;
    }

    @Override
    public Set<String> getTenantIds() {
        return tenantExecutors.keySet();
    }

    @Override
    public void addTenantAsyncExecutor(String tenantId, boolean startExecutor) {
        AsyncExecutor tenantExecutor = null;

        if (tenantAwareAyncExecutorFactory == null) {
            DefaultAsyncJobExecutor defaultAsyncJobExecutor = new DefaultAsyncJobExecutor();
            defaultAsyncJobExecutor.setTenantId(tenantId);
            tenantExecutor = defaultAsyncJobExecutor;
        } else {
            tenantExecutor = tenantAwareAyncExecutorFactory.createAsyncExecutor(tenantId);
        }

        tenantExecutor.setJobServiceConfiguration(jobServiceConfiguration);

        if (tenantExecutor instanceof AbstractAsyncExecutor defaultAsyncJobExecutor) {
            defaultAsyncJobExecutor.setAsyncJobsDueRunnable(new TenantAwareAcquireAsyncJobsDueRunnable(defaultAsyncJobExecutor, tenantInfoHolder, tenantId));
            defaultAsyncJobExecutor.setTimerJobRunnable(new TenantAwareAcquireTimerJobsRunnable(defaultAsyncJobExecutor, tenantInfoHolder, tenantId, defaultAsyncJobExecutor.getMoveTimerExecutorPoolSize()));
            defaultAsyncJobExecutor.setExecuteAsyncRunnableFactory(new TenantAwareExecuteAsyncRunnableFactory(tenantInfoHolder, tenantId, defaultAsyncJobExecutor.getJobExecutionObservationProvider()));
            defaultAsyncJobExecutor.setResetExpiredJobsRunnable(new TenantAwareResetExpiredJobsRunnable(defaultAsyncJobExecutor, tenantInfoHolder, tenantId));
        }

        if (tenantId != null) {
            tenantExecutors.put(tenantId, tenantExecutor);
        } else {
            nullTenantIdAsyncExecutor = tenantExecutor;
        }

        if (startExecutor) {
            startTenantExecutor(tenantId);
        }
    }
    
    @Override
    public AsyncExecutor getTenantAsyncExecutor(String tenantId) {
        if (tenantId != null) {
            return tenantExecutors.get(tenantId);
        }
        return nullTenantIdAsyncExecutor;
    }

    @Override
    public void removeTenantAsyncExecutor(String tenantId) {
        shutdownTenantExecutor(tenantId);

        if (tenantId != null) {
            tenantExecutors.remove(tenantId);
        } else {
            nullTenantIdAsyncExecutor = null;
        }
    }

    protected AsyncExecutor determineAsyncExecutor() {
        String currentTenantId = tenantInfoHolder.getCurrentTenantId();
        if (currentTenantId != null) {
            return tenantExecutors.get(currentTenantId);
        }
        return nullTenantIdAsyncExecutor;
    }

    @Override
    public boolean executeAsyncJob(JobInfo job) {
        return determineAsyncExecutor().executeAsyncJob(job);
    }

    @Override
    public void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setJobServiceConfiguration(jobServiceConfiguration);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setJobServiceConfiguration(jobServiceConfiguration);
        }
    }

    @Override
    public JobServiceConfiguration getJobServiceConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAutoActivate() {
        return autoActivate;
    }

    @Override
    public void setAutoActivate(boolean isAutoActivate) {
        autoActivate = isAutoActivate;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void start() {
        for (String tenantId : tenantExecutors.keySet()) {
            startTenantExecutor(tenantId);
        }
        if (nullTenantIdAsyncExecutor != null) {
            startTenantExecutor(null);
        }
        active = true;
    }

    protected void startTenantExecutor(String tenantId) {
        tenantInfoHolder.setCurrentTenantId(tenantId);
        if (tenantId != null) {
            tenantExecutors.get(tenantId).start();
        } else if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.start();
        }
        tenantInfoHolder.clearCurrentTenantId();
    }

    @Override
    public synchronized void shutdown() {
        for (String tenantId : tenantExecutors.keySet()) {
            shutdownTenantExecutor(tenantId);
        }
        if (nullTenantIdAsyncExecutor != null) {
            shutdownTenantExecutor(null);
        }
        active = false;
    }

    protected void shutdownTenantExecutor(String tenantId) {
        LOGGER.info("Shutting down async executor for tenant {}", tenantId);
        tenantInfoHolder.setCurrentTenantId(tenantId);
        if (tenantId != null) {
            tenantExecutors.get(tenantId).shutdown();
        } else if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.shutdown();
        }
        tenantInfoHolder.clearCurrentTenantId();
    }

    @Override
    public String getLockOwner() {
        return determineAsyncExecutor().getLockOwner();
    }

    @Override
    public int getTimerLockTimeInMillis() {
        return determineAsyncExecutor().getTimerLockTimeInMillis();
    }

    @Override
    public void setTimerLockTimeInMillis(int lockTimeInMillis) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setTimerLockTimeInMillis(lockTimeInMillis);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setTimerLockTimeInMillis(lockTimeInMillis);
        }
    }

    @Override
    public int getAsyncJobLockTimeInMillis() {
        return determineAsyncExecutor().getAsyncJobLockTimeInMillis();
    }

    @Override
    public void setAsyncJobLockTimeInMillis(int lockTimeInMillis) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setAsyncJobLockTimeInMillis(lockTimeInMillis);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setAsyncJobLockTimeInMillis(lockTimeInMillis);
        }
    }

    @Override
    public int getDefaultTimerJobAcquireWaitTimeInMillis() {
        return determineAsyncExecutor().getDefaultTimerJobAcquireWaitTimeInMillis();
    }

    @Override
    public void setDefaultTimerJobAcquireWaitTimeInMillis(int waitTimeInMillis) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(waitTimeInMillis);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(waitTimeInMillis);
        }
    }

    @Override
    public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
        return determineAsyncExecutor().getDefaultAsyncJobAcquireWaitTimeInMillis();
    }

    @Override
    public void setDefaultAsyncJobAcquireWaitTimeInMillis(int waitTimeInMillis) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(waitTimeInMillis);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(waitTimeInMillis);
        }
    }

    @Override
    public int getDefaultQueueSizeFullWaitTimeInMillis() {
        return determineAsyncExecutor().getDefaultQueueSizeFullWaitTimeInMillis();
    }

    @Override
    public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTimeInMillis) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(defaultQueueSizeFullWaitTimeInMillis);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(defaultQueueSizeFullWaitTimeInMillis);
        }
    }

    @Override
    public int getMaxAsyncJobsDuePerAcquisition() {
        return determineAsyncExecutor().getMaxAsyncJobsDuePerAcquisition();
    }

    @Override
    public void setMaxAsyncJobsDuePerAcquisition(int maxJobs) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setMaxAsyncJobsDuePerAcquisition(maxJobs);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setMaxAsyncJobsDuePerAcquisition(maxJobs);
        }
    }

    @Override
    public int getMaxTimerJobsPerAcquisition() {
        return determineAsyncExecutor().getMaxTimerJobsPerAcquisition();
    }

    @Override
    public void setMaxTimerJobsPerAcquisition(int maxJobs) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setMaxTimerJobsPerAcquisition(maxJobs);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setMaxTimerJobsPerAcquisition(maxJobs);
        }
    }

    @Override
    public int getResetExpiredJobsInterval() {
        return determineAsyncExecutor().getResetExpiredJobsInterval();
    }

    @Override
    public void setResetExpiredJobsInterval(int resetExpiredJobsInterval) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setResetExpiredJobsInterval(resetExpiredJobsInterval);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setResetExpiredJobsInterval(resetExpiredJobsInterval);
        }
    }

    @Override
    public int getResetExpiredJobsPageSize() {
        return determineAsyncExecutor().getResetExpiredJobsPageSize();
    }

    @Override
    public void setResetExpiredJobsPageSize(int resetExpiredJobsPageSize) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            asyncExecutor.setResetExpiredJobsPageSize(resetExpiredJobsPageSize);
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setResetExpiredJobsPageSize(resetExpiredJobsPageSize);
        }
    }

    @Override
    public AsyncTaskExecutor getTaskExecutor() {
        AsyncExecutor asyncExecutor = determineAsyncExecutor();
        if (asyncExecutor == null) {
            return null;
        }
        return asyncExecutor.getTaskExecutor();
    }

    @Override
    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
            if (asyncExecutor.getTaskExecutor() == null) {
                asyncExecutor.setTaskExecutor(taskExecutor);
            }
        }
        if (nullTenantIdAsyncExecutor != null) {
            nullTenantIdAsyncExecutor.setTaskExecutor(taskExecutor);
        }
    }
}
