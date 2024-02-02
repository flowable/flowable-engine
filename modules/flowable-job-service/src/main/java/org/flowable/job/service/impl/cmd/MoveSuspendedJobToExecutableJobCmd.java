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

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;

/**
 * @author martin.grofcik
 */
public class MoveSuspendedJobToExecutableJobCmd implements Command<Job>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String jobId;
    protected JobServiceConfiguration jobServiceConfiguration;

    public MoveSuspendedJobToExecutableJobCmd(String jobId, JobServiceConfiguration jobServiceConfiguration) {
        this.jobId = jobId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Job execute(CommandContext commandContext) {

        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId and job is null");
        }

        SuspendedJobEntity job = jobServiceConfiguration.getSuspendedJobEntityManager().findById(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId);
        }
        return jobServiceConfiguration.getJobService().activateSuspendedJob(job);
    }

    public String getJobId() {
        return jobId;
    }

}
