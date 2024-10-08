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

import org.flowable.common.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.flowable.job.service.impl.asyncexecutor.JobExecutionObservationProvider;

/**
 * Extends the default {@link ExecuteAsyncRunnable} by setting the 'tenant' context before executing.
 * 
 * @author Joram Barrez
 */
public class TenantAwareExecuteAsyncRunnable extends ExecuteAsyncRunnable {

    protected TenantInfoHolder tenantInfoHolder;
    protected String tenantId;

    public TenantAwareExecuteAsyncRunnable(JobInfo job, JobServiceConfiguration jobServiceConfiguration, TenantInfoHolder tenantInfoHolder, String tenantId,
            JobExecutionObservationProvider jobExecutionObservationProvider) {
        super(job, jobServiceConfiguration, jobServiceConfiguration.getJobEntityManager(), null, jobExecutionObservationProvider);
        this.tenantInfoHolder = tenantInfoHolder;
        this.tenantId = tenantId;
    }

    @Override
    public void run() {
        tenantInfoHolder.setCurrentTenantId(tenantId);
        super.run();
        tenantInfoHolder.clearCurrentTenantId();
    }

}
