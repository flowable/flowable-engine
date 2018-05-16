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
package org.flowable.cmmn.api;

import java.util.Collection;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;

/**
 * @author Joram Barrez
 */
public interface CmmnManagementService {

    /**
     * Returns a map containing {tableName, rowCount} values.
     */
    Map<String, Long> getTableCounts();
    
    /**
     * Returns all relational database tables of the engine. 
     */
    Collection<String> getTableNames();
    
    /**
     * Returns a new JobQuery implementation, that can be used to query the jobs.
     */
    JobQuery createJobQuery();

    /**
     * Returns a new TimerJobQuery implementation, that can be used to query the timer jobs.
     */
    TimerJobQuery createTimerJobQuery();

    /**
     * Returns a new SuspendedJobQuery implementation, that can be used to query the suspended jobs.
     */
    SuspendedJobQuery createSuspendedJobQuery();

    /**
     * Returns a new DeadLetterJobQuery implementation, that can be used to query the dead letter jobs.
     */
    DeadLetterJobQuery createDeadLetterJobQuery();
    
    /**
     * Forced synchronous execution of a job (eg. for administration or testing).
     * The job will be executed, even if the case instance is suspended.
     * 
     * @param jobId
     *            id of the job to execute, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void executeJob(String jobId);

    /**
     * Moves a timer job to the executable job table (eg. for administration or testing). 
     * The timer job will be moved, even if the case instance is suspended.
     * 
     * @param jobId
     *            id of the timer job to move, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveTimerToExecutableJob(String jobId);

    /**
     * Moves a job to the dead letter job table (eg. for administration or testing). 
     * The job will be moved, even if the case instance is suspended or there are retries left.
     * 
     * @param jobId
     *            id of the job to move, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveJobToDeadLetterJob(String jobId);

    /**
     * Moves a job that is in the dead letter job table back to be an executable job, 
     * and resetting the retries (as the retries were probably 0 when it was put into the dead letter job table).
     * 
     * @param jobId
     *            id of the job to move, cannot be null.
     * @param retries
     *            the number of retries (value greater than 0) which will be set on the job.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveDeadLetterJobToExecutableJob(String jobId, int retries);

    /**
     * Delete the job with the provided id.
     * 
     * @param jobId
     *            id of the job to delete, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void deleteJob(String jobId);

    /**
     * Delete the timer job with the provided id.
     * 
     * @param jobId
     *            id of the timer job to delete, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void deleteTimerJob(String jobId);
    
    /**
     * Delete the suspended job with the provided id.
     * 
     * @param jobId
     *            id of the suspended job to delete, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void deleteSuspendedJob(String jobId);

    /**
     * Delete the dead letter job with the provided id.
     * 
     * @param jobId
     *            id of the dead letter job to delete, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void deleteDeadLetterJob(String jobId);
    
    /**
     * Sets the number of retries that a job has left.
     * 
     * Whenever the JobExecutor fails to execute a job, this value is decremented. 
     * When it hits zero, the job is supposed to be dead and not retried again. 
     * In that case, this method can be used to increase the number of retries.
     * 
     * @param jobId
     *            id of the job to modify, cannot be null.
     * @param retries
     *            number of retries.
     */
    void setJobRetries(String jobId, int retries);

    /**
     * Sets the number of retries that a timer job has left.
     * 
     * Whenever the JobExecutor fails to execute a timer job, this value is decremented. 
     * When it hits zero, the job is supposed to be dead and not retried again. 
     * In that case, this method can be used to increase the number of retries.
     * 
     * @param jobId
     *            id of the timer job to modify, cannot be null.
     * @param retries
     *            number of retries.
     */
    void setTimerJobRetries(String jobId, int retries);

    /**
     * Returns the full stacktrace of the exception that occurs when the job with the given id was last executed. 
     * Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the timer job with the given id was last executed. 
     * Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getTimerJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the suspended with the given id was last executed. 
     * Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getSuspendedJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the deadletter job with the given id was last executed. 
     * Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getDeadLetterJobExceptionStacktrace(String jobId);
    
    /**
     * Returns a new HistoryJobQuery implementation, that can be used to dynamically query the history jobs.
     */
    HistoryJobQuery createHistoryJobQuery();

}
