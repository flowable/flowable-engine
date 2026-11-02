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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;

public class UnacquireExternalWorkerJobCmd implements Command<Void> {

    protected final String jobId;
    protected final String workerId;
    protected final JobServiceConfiguration jobServiceConfiguration;

    public UnacquireExternalWorkerJobCmd(String jobId, String workerId, JobServiceConfiguration jobServiceConfiguration) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (StringUtils.isEmpty(jobId)) {
            throw new FlowableIllegalArgumentException("job id must not be empty");
        }

        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("worker id must not be empty");
        }

        ExternalWorkerJobEntityManager externalWorkerJobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();

        ExternalWorkerJobEntity jobEntity = externalWorkerJobEntityManager.findById(jobId);
        if (jobEntity == null) {
            throw new FlowableException("Could not find job for id " + jobId);
        }
        
        if (!jobEntity.getLockOwner().equals(workerId)) {
            throw new FlowableException(jobEntity + " is locked with a different worker id");
        }

        jobEntity.setLockExpirationTime(null);
        jobEntity.setLockOwner(null);
        externalWorkerJobEntityManager.update(jobEntity);
        if (jobEntity.isExclusive()) {
            new UnlockExclusiveJobCmd(jobEntity, jobServiceConfiguration).execute(commandContext);
        }
        
        return null;
    }
}
