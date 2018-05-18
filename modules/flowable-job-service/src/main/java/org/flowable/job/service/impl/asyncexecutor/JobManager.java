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

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Contains methods that are not tied to any specific job type (async, timer, suspended or deadletter), but which are generally applicable or are about going from one type to another.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface JobManager {

    /**
     * Execute a job, which means that the logic (async logic, timer that fires, etc) is executed, typically by a background thread of an executor.
     */
    void execute(JobInfo job);
    
    /**
     * Unacquires a job, meaning that this job was previously locked, and it is now freed to be acquired by other executor nodes.
     */
    void unacquire(JobInfo job);
    
    /**
     * Unacquires a job, meaning that this job was previously locked, and it is now freed to be acquired by other executor nodes.
     */
    void unacquireWithDecrementRetries(JobInfo job);
    
    /**
     * Creates an async job so that it can be continued later in a background thread.
     */
    void createAsyncJob(JobEntity jobEntity, boolean exclusive);

    /**
     * Schedules and async job. If the {@link AsyncExecutor} is running, it can be executed immediately after the transaction. Otherwise it can be picked up by other executors.
     */
    void scheduleAsyncJob(JobEntity job);

    /**
     * Schedules a timer, meaning it will be inserted in the datastore.
     */
    void scheduleTimerJob(TimerJobEntity timerJob);
    
    /**
     * Get the business calendar name of the job configuration
     */
    String getBusinessCalendarName(JobEntity timerEntity, VariableScope variableScope);

    /**
     * Moves a {@link TimerJobEntity} to become an async {@link JobEntity}.
     * 
     * This happens for example when the due date of a timer is reached, the timer entity then becomes a 'regular' async job that can be picked up by the {@link AsyncExecutor}.
     */
    JobEntity moveTimerJobToExecutableJob(TimerJobEntity timerJob);

    /**
     * Moves an {@link AbstractRuntimeJobEntity} to become a {@link TimerJobEntity}.
     * 
     * This happens for example when an async job is executed and fails. It then becomes a timer, as it needs to be retried later.
     */
    TimerJobEntity moveJobToTimerJob(AbstractRuntimeJobEntity job);

    /**
     * Moves an {@link AbstractRuntimeJobEntity} to become a {@link SuspendedJobEntity}, such that the {@link AsyncExecutor} won't pick it up anymore for execution.
     */
    SuspendedJobEntity moveJobToSuspendedJob(AbstractRuntimeJobEntity job);

    /**
     * Transforms a {@link SuspendedJobEntity} back to an {@link AbstractRuntimeJobEntity} (i.e. to what it was originally). The job will now again be able to picked up by the {@link AsyncExecutor}.
     */
    AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job);

    /**
     * Transforms an {@link AbstractRuntimeJobEntity} to a {@link DeadLetterJobEntity}. This means that the job has been tried a configurable amount of times, but kept failing.
     */
    DeadLetterJobEntity moveJobToDeadLetterJob(AbstractRuntimeJobEntity job);

    /**
     * Transforms a {@link DeadLetterJobEntity} to a {@link JobEntity}, thus making it executable again. Note that a 'retries' parameter needs to be passed, as the job got into the deadletter table
     * because of it failed and retries became 0.
     */
    JobEntity moveDeadLetterJobToExecutableJob(DeadLetterJobEntity deadLetterJobEntity, int retries);
    
    /**
     * schedules a {@link HistoryJobEntity}, meaning it will be scheduled (inserted in the database/put on a queue/...) to be executed at a later point in time.
     */
    HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity);

    /**
     * The ProcessEngineConfiguration instance will be passed when the ProcessEngine is built.
     */
    void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration);
    
    /**
     * Create an executable job from another job
     */
    JobEntity createExecutableJobFromOtherJob(AbstractRuntimeJobEntity job);

    /**
     * Create a timer job from another job
     */
    TimerJobEntity createTimerJobFromOtherJob(AbstractRuntimeJobEntity otherJob);

    /**
     * Create a suspended job from another job
     */
    SuspendedJobEntity createSuspendedJobFromOtherJob(AbstractRuntimeJobEntity otherJob);

    /**
     * Create a dead letter job from another job
     */
    DeadLetterJobEntity createDeadLetterJobFromOtherJob(AbstractRuntimeJobEntity otherJob);

    /**
     * Copy job info from one job to the other
     */
    AbstractRuntimeJobEntity copyJobInfo(AbstractRuntimeJobEntity copyToJob, AbstractRuntimeJobEntity copyFromJob);
}
