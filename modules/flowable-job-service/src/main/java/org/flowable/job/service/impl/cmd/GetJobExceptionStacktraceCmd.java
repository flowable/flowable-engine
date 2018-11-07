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
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class GetJobExceptionStacktraceCmd implements Command<String>, Serializable {

    private static final long serialVersionUID = 1L;
    private String jobId;
    protected JobType jobType;

    public GetJobExceptionStacktraceCmd(String jobId, JobType jobType) {
        this.jobId = jobId;
        this.jobType = jobType;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("jobId is null");
        }

        AbstractRuntimeJobEntity job = null;
        switch (jobType) {
        case ASYNC:
            job = CommandContextUtil.getJobEntityManager(commandContext).findById(jobId);
            break;
        case TIMER:
            job = CommandContextUtil.getTimerJobEntityManager(commandContext).findById(jobId);
            break;
        case SUSPENDED:
            job = CommandContextUtil.getSuspendedJobEntityManager(commandContext).findById(jobId);
            break;
        case DEADLETTER:
            job = CommandContextUtil.getDeadLetterJobEntityManager(commandContext).findById(jobId);
            break;
        }

        if (job == null) {
            throw new FlowableObjectNotFoundException("No job found with id " + jobId, Job.class);
        }

        return job.getExceptionStacktrace();
    }

}
