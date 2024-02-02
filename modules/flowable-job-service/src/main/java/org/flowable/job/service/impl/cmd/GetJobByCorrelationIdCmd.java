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
import org.flowable.job.service.JobServiceConfiguration;

/**
 * @author Filip Hrisafov
 */
public class GetJobByCorrelationIdCmd implements Command<Job> {
    
    protected JobServiceConfiguration jobServiceConfiguration;

    protected String correlationId;

    public GetJobByCorrelationIdCmd(String correlationId, JobServiceConfiguration jobServiceConfiguration) {
        this.correlationId = correlationId;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Job execute(CommandContext commandContext) {
        if (correlationId == null) {
            throw new FlowableIllegalArgumentException("correlationId is null");
        }

        Job job = jobServiceConfiguration.getDeadLetterJobEntityManager().findJobByCorrelationId(correlationId);
        if (job != null) {
            return job;
        }

        job = jobServiceConfiguration.getExternalWorkerJobEntityManager().findJobByCorrelationId(correlationId);
        if (job != null) {
            return job;
        }

        job = jobServiceConfiguration.getTimerJobEntityManager().findJobByCorrelationId(correlationId);
        if (job != null) {
            return job;
        }

        job = jobServiceConfiguration.getSuspendedJobEntityManager().findJobByCorrelationId(correlationId);
        if (job != null) {
            return job;
        }

        return jobServiceConfiguration.getJobEntityManager().findJobByCorrelationId(correlationId);
    }
}
