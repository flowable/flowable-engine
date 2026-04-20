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
package org.flowable.job.service.impl.cmd;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;

public class UnacquireAllExternalWorkerJobsForWorkerCmd implements Command<Void> {

    protected final String workerId;
    protected final String tenantId;
    protected final JobServiceConfiguration jobServiceConfiguration;

    public UnacquireAllExternalWorkerJobsForWorkerCmd(String workerId, String tenantId, JobServiceConfiguration jobServiceConfiguration) {
        this.workerId = workerId;
        this.tenantId = tenantId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("worker id must not be empty");
        }

        ExternalWorkerJobEntityManager externalWorkerJobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();

        List<ExternalWorkerJobEntity> jobEntities = externalWorkerJobEntityManager.findJobsByWorkerId(workerId);
        
        if (!jobEntities.isEmpty()) {
            if (StringUtils.isNotEmpty(tenantId)) {
                for (ExternalWorkerJobEntity externalWorkerJob : jobEntities) {
                    if (!tenantId.equals(externalWorkerJob.getTenantId())) {
                        throw new FlowableIllegalArgumentException("provided worker id has external worker jobs from different tenant.");
                    }
                }
            }
            
            for (ExternalWorkerJobEntity externalWorkerJob : jobEntities) {
                if (externalWorkerJob.isExclusive()) {
                    new UnlockExclusiveJobCmd(externalWorkerJob, jobServiceConfiguration).execute(commandContext);
                }
            }
            
            externalWorkerJobEntityManager.bulkUpdateJobLockWithoutRevisionCheck(jobEntities, null, null);
        }
        
        return null;
    }
}
