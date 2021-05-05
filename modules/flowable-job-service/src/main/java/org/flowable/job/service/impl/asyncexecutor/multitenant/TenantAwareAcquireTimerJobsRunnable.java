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

import java.util.List;

import org.flowable.common.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerJobsRunnable;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerLifecycleListener;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Extends the default {@link AcquireTimerJobsRunnable} by setting the 'tenant' context before executing.
 * 
 * @author Joram Barrez
 */
public class TenantAwareAcquireTimerJobsRunnable extends AcquireTimerJobsRunnable {

    protected TenantInfoHolder tenantInfoHolder;
    protected String tenantId;

    public TenantAwareAcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, TenantInfoHolder tenantInfoHolder, String tenantId, int moveExecutorPoolSize) {
        this(asyncExecutor, tenantInfoHolder, tenantId, null, false, "", moveExecutorPoolSize);
    }

    public TenantAwareAcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, TenantInfoHolder tenantInfoHolder, String tenantId,
            AcquireTimerLifecycleListener lifecycleListener, boolean globalAcquireLockEnabled, String globalAcquireLockPrefix, int moveExecutorPoolSize) {

        super(asyncExecutor, asyncExecutor.getJobServiceConfiguration().getJobManager(), lifecycleListener,
            globalAcquireLockEnabled, globalAcquireLockPrefix, moveExecutorPoolSize);
        this.tenantInfoHolder = tenantInfoHolder;
        this.tenantId = tenantId;
    }

    protected ExecutorPerTenantAsyncExecutor getTenantAwareAsyncExecutor() {
        return (ExecutorPerTenantAsyncExecutor) asyncExecutor;
    }

    @Override
    public synchronized void run() {
        tenantInfoHolder.setCurrentTenantId(tenantId);
        super.run();
        tenantInfoHolder.clearCurrentTenantId();
    }

    @Override
    protected void executeMoveTimerJobsToExecutableJobs(List<TimerJobEntity> timerJobs) {
        try {
            tenantInfoHolder.setCurrentTenantId(tenantId);
            super.executeMoveTimerJobsToExecutableJobs(timerJobs);
        } finally {
            tenantInfoHolder.clearCurrentTenantId();
        }
    }
}
