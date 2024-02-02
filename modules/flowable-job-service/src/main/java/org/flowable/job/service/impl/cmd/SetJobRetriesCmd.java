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
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Falko Menge
 */
public class SetJobRetriesCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected JobServiceConfiguration jobServiceConfiguration;

    protected final String jobId;
    protected final int retries;

    public SetJobRetriesCmd(String jobId, int retries, JobServiceConfiguration jobServiceConfiguration) {
        if (jobId == null || jobId.length() < 1) {
            throw new FlowableIllegalArgumentException("The job id is mandatory, but '" + jobId + "' has been provided.");
        }
        if (retries < 0) {
            throw new FlowableIllegalArgumentException("The number of job retries must be a non-negative Integer, but '" + retries + "' has been provided.");
        }
        this.jobId = jobId;
        this.retries = retries;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        JobEntity job = jobServiceConfiguration.getJobEntityManager().findById(jobId);
        if (job != null) {

            job.setRetries(retries);

            FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, job),
                        jobServiceConfiguration.getEngineName());
            }
        } else {
            throw new FlowableObjectNotFoundException("No job found with id '" + jobId + "'.", Job.class);
        }
        return null;
    }
}
