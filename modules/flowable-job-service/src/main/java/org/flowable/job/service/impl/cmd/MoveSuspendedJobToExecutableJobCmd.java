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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

import java.io.Serializable;

/**
 * @author martin.grofcik
 */
public class MoveSuspendedJobToExecutableJobCmd implements Command<Job>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String jobId;

    public MoveSuspendedJobToExecutableJobCmd(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public Job execute(CommandContext commandContext) {

        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId and job is null");
        }

        SuspendedJobEntity job = CommandContextUtil.getSuspendedJobEntityManager(commandContext).findById(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId);
        }
        return CommandContextUtil.getJobServiceConfiguration().getJobService().activateSuspendedJob(job);
    }

    public String getJobId() {
        return jobId;
    }

}
