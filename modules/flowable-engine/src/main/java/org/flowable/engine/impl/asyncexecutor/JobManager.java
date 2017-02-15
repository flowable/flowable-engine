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
package org.flowable.engine.impl.asyncexecutor;

import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.AbstractJobEntity;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.runtime.Job;

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
    void execute(Job job);

    /**
     * Unacquires a job, meaning that this job was previously locked, and it is now freed to be acquired by other executor nodes.
     */
    void unacquire(Job job);

    /**
     * Creates an async job for the provided {@link ExecutionEntity}, so that it can be continued later in a background thread.
     */
    JobEntity createAsyncJob(ExecutionEntity execution, boolean exclusive);

    /**
     * Schedules and async job. If the {@link AsyncExecutor} is running, it can be executed immediately after the transaction. Otherwise it can be picked up by other executors.
     */
    void scheduleAsyncJob(JobEntity job);

    /**
     * Creates a {@link TimerJobEntity} based on the current {@link ExecutionEntity} and the configuration in the {@link TimerEventDefinition}.
     */
    TimerJobEntity createTimerJob(TimerEventDefinition timerEventDefinition, boolean interrupting, ExecutionEntity execution, String timerEventType, String jobHandlerConfiguration);

    /**
     * Schedules a timer, meaning it will be inserted in the datastore.
     */
    void scheduleTimerJob(TimerJobEntity timerJob);

    /**
     * Reschedules a timer by deleting the timer job and creating a new one with the specified definition.
     */
    TimerJobEntity rescheduleTimerJob(String timerJobId, TimerEventDefinition timerEventDefinition);

    /**
     * Moves a {@link TimerJobEntity} to become an async {@link JobEntity}.
     * 
     * This happens for example when the due date of a timer is reached, the timer entity then becomes a 'regular' async job that can be picked up by the {@link AsyncExecutor}.
     */
    JobEntity moveTimerJobToExecutableJob(TimerJobEntity timerJob);

    /**
     * Moves an {@link AbstractJobEntity} to become a {@link TimerJobEntity}.
     * 
     * This happens for example when an async job is executed and fails. It then becomes a timer, as it needs to be retried later.
     */
    TimerJobEntity moveJobToTimerJob(AbstractJobEntity job);

    /**
     * Moves an {@link AbstractJobEntity} to become a {@link SuspendedJobEntity}, such that the {@link AsyncExecutor} won't pick it up anymore for execution.
     */
    SuspendedJobEntity moveJobToSuspendedJob(AbstractJobEntity job);

    /**
     * Transforms a {@link SuspendedJobEntity} back to an {@link AbstractJobEntity} (i.e. to what it was originally). The job will now again be able to picked up by the {@link AsyncExecutor}.
     */
    AbstractJobEntity activateSuspendedJob(SuspendedJobEntity job);

    /**
     * Transforms an {@link AbstractJobEntity} to a {@link DeadLetterJobEntity}. This means that the job has been tried a configurable amount of times, but kept failing.
     */
    DeadLetterJobEntity moveJobToDeadLetterJob(AbstractJobEntity job);

    /**
     * Transforms a {@link DeadLetterJobEntity} to a {@link JobEntity}, thus making it executable again. Note that a 'retries' parameter needs to be passed, as the job got into the deadletter table
     * because of it failed and retries became 0.
     */
    JobEntity moveDeadLetterJobToExecutableJob(DeadLetterJobEntity deadLetterJobEntity, int retries);

    /**
     * The ProcessEngineConfiguration instance will be passed when the {@link ProcessEngine} is built.
     */
    void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration);

}
