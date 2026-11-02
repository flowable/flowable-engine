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
package org.flowable.job.service.impl.asyncexecutor;

import static org.flowable.job.service.impl.util.JobProcessorUtil.callJobProcessors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.job.service.JobProcessorContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class TimerJobSchedulerImpl implements TimerJobScheduler {

    protected final JobServiceConfiguration jobServiceConfiguration;

    public TimerJobSchedulerImpl(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public void rescheduleTimerJobAfterExecution(JobEntity timerJob, VariableScope variableScope) {
        if (timerJob.getRepeat() != null) {
            TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration.getTimerJobEntityManager();
            TimerJobEntity newTimerJobEntity = timerJobEntityManager.createAndCalculateNextTimer(timerJob, variableScope);
            if (newTimerJobEntity != null) {
                if (jobServiceConfiguration.getInternalJobManager() != null) {
                    jobServiceConfiguration.getInternalJobManager().preRepeatedTimerSchedule(newTimerJobEntity, variableScope);
                }

                scheduleTimerJob(newTimerJobEntity);
            }
        }
    }

    @Override
    public void scheduleTimerJob(TimerJobEntity timerJob) {
        scheduleTimer(timerJob);
        sendTimerScheduledEvent(timerJob);
    }

    protected void scheduleTimer(TimerJobEntity timerJob) {
        if (timerJob == null) {
            throw new FlowableException("Empty timer job can not be scheduled");
        }
        callJobProcessors(jobServiceConfiguration, JobProcessorContext.Phase.BEFORE_CREATE, timerJob);
        jobServiceConfiguration.getTimerJobEntityManager().insert(timerJob);
    }

    protected void sendTimerScheduledEvent(TimerJobEntity timerJob) {
        FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(
                    FlowableEngineEventType.TIMER_SCHEDULED, timerJob), jobServiceConfiguration.getEngineName());
        }
    }
}
