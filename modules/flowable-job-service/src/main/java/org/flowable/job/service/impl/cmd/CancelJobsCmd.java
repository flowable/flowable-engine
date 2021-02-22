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
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Send job cancelled event and delete job
 * 
 * @author Tom Baeyens
 */
public class CancelJobsCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected JobServiceConfiguration jobServiceConfiguration;
    
    protected List<String> jobIds;

    public CancelJobsCmd(List<String> jobIds, JobServiceConfiguration jobServiceConfiguration) {
        this.jobIds = jobIds;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    public CancelJobsCmd(String jobId, JobServiceConfiguration jobServiceConfiguration) {
        this.jobIds = new ArrayList<>();
        jobIds.add(jobId);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        JobEntity jobToDelete = null;
        for (String jobId : jobIds) {
            jobToDelete = jobServiceConfiguration.getJobEntityManager().findById(jobId);

            FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
            if (jobToDelete != null) {
                // When given job doesn't exist, ignore
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete),
                            jobServiceConfiguration.getEngineName());
                }

                jobServiceConfiguration.getJobEntityManager().delete(jobToDelete);

            } else {
                TimerJobEntity timerJobToDelete = jobServiceConfiguration.getTimerJobEntityManager().findById(jobId);

                if (timerJobToDelete != null) {
                    // When given job doesn't exist, ignore
                    if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                        eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, timerJobToDelete),
                                jobServiceConfiguration.getEngineName());
                    }

                    jobServiceConfiguration.getTimerJobEntityManager().delete(timerJobToDelete);
                }
            }
        }
        return null;
    }
}
