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
package org.flowable.engine;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.management.TablePage;
import org.flowable.common.engine.api.management.TablePageQuery;
import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Service for admin and maintenance operations on the process engine.
 * 
 * These operations will typically not be used in a workflow driven application, but are used in for example the operational console.
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 */
public interface ManagementService {

    /**
     * Get the mapping containing {table name, row count} entries of the database schema.
     */
    Map<String, Long> getTableCount();

    /**
     * Gets the table name (including any configured prefix) for an entity like Task, Execution or the like.
     */
    String getTableName(Class<?> entityClass);

    /**
     * Gets the metadata (column names, column types, etc.) of a certain table. Returns null when no table exists with the given name.
     */
    TableMetaData getTableMetaData(String tableName);

    /**
     * Creates a {@link TablePageQuery} that can be used to fetch {@link TablePage} containing specific sections of table row data.
     */
    TablePageQuery createTablePageQuery();

    /**
     * Returns a new JobQuery implementation, that can be used to dynamically query the jobs.
     */
    JobQuery createJobQuery();

    /**
     * Returns a new TimerJobQuery implementation, that can be used to dynamically query the timer jobs.
     */
    TimerJobQuery createTimerJobQuery();

    /**
     * Returns a new SuspendedJobQuery implementation, that can be used to dynamically query the suspended jobs.
     */
    SuspendedJobQuery createSuspendedJobQuery();

    /**
     * Returns a new DeadLetterJobQuery implementation, that can be used to dynamically query the dead letter jobs.
     */
    DeadLetterJobQuery createDeadLetterJobQuery();
    
    /**
     * Returns a new HistoryJobQuery implementation, that can be used to dynamically query the history jobs.
     */
    HistoryJobQuery createHistoryJobQuery();

    /**
     * Forced synchronous execution of a job (eg. for administration or testing) The job will be executed, even if the process definition and/or the process instance is in suspended state.
     * 
     * @param jobId
     *            id of the job to execute, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void executeJob(String jobId);

    /**
     * Moves a timer job to the executable job table (eg. for administration or testing). The timer job will be moved, even if the process definition and/or the process instance is in suspended state.
     * 
     * @param jobId
     *            id of the timer job to move, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveTimerToExecutableJob(String jobId);

    /**
     * Moves a job to the dead letter job table (eg. for administration or testing). The job will be moved, even if the process definition and/or the process instance has retries left.
     * 
     * @param jobId
     *            id of the job to move, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveJobToDeadLetterJob(String jobId);

    /**
     * Moves a job that is in the dead letter job table back to be an executable job, and resetting the retries (as the retries was 0 when it was put into the dead letter job table).
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
     * Moves a suspendend job from the suspended letter job table back to be an executable job. The retries are untouched.
     * 
     * @param jobId
     *            id of the job to move, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    Job moveSuspendedJobToExecutableJob(String jobId);

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
     * Delete the history job with the provided id.
     * 
     * @param jobId
     *            id of the history job to delete, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when there is no job with the given id.
     */
    void deleteHistoryJob(String jobId);

    /**
     * Sets the number of retries that a job has left.
     * 
     * Whenever the JobExecutor fails to execute a job, this value is decremented. When it hits zero, the job is supposed to be dead and not retried again. In that case, this method can be used to
     * increase the number of retries.
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
     * Whenever the JobExecutor fails to execute a timer job, this value is decremented. When it hits zero, the job is supposed to be dead and not retried again. In that case, this method can be used
     * to increase the number of retries.
     * 
     * @param jobId
     *            id of the timer job to modify, cannot be null.
     * @param retries
     *            number of retries.
     */
    void setTimerJobRetries(String jobId, int retries);

    /**
     * Reschedule a timer job with a time date.
     * 
     * @param jobId
     *            id of the timer job to reschedule, cannot be null.
     * @param timeDate
     *            A fixed date in ISO 8601 format, when job will be fired
     */
    Job rescheduleTimeDateJob(String jobId, String timeDate);

    /**
     * Reschedule a timer job with a time duration.
     * 
     * @param jobId
     *            id of the timer job to reschedule, cannot be null.
     * @param timeDuration
     *            How long the timer should run before it is fired in ISO 8601 format. For example, PT10D means the timer will run for 10 days before it fires.
     */
    Job rescheduleTimeDurationJob(String jobId, String timeDuration);

    /**
     * Reschedule a timer job with a time cycle.
     * 
     * @param jobId
     *            id of the timer job to reschedule, cannot be null.
     * @param timeCycle
     *            Specifies a repeating interval at which the timer will fire in ISO 8601 format. For example R3/PT10H means the timer will fire three timers in intervals of 10 hours.
     */
    Job rescheduleTimeCycleJob(String jobId, String timeCycle);

    /**
     * Reschedule a timer job.
     * 
     * @param jobId
     *            id of the timer job to reschedule, cannot be null.
     * @param timeDate
     *            A fixed date in ISO 8601 format, when job will be fired
     * @param timeDuration
     *            How long the timer should run before it is fired in ISO 8601 format. For example, PT10D means the timer will run for 10 days before it fires.
     * @param timeCycle
     *            Specifies a repeating interval at which the timer will fire in ISO 8601 format. For example R3/PT10H means the timer will fire three timers in intervals of 10 hours.
     * @param endDate
     *            The date at which the application will stop creating additional jobs. The value should be provided in ISO8601 format. For example "2015-02-25T16:42:11+00:00".
     * @param calendarName
     *            The name of a business calendar defined in the process engine configuration. If null the default business calendars is used.
     */
    Job rescheduleTimerJob(String jobId, String timeDate, String timeDuration, String timeCycle, String endDate, String calendarName);

    /**
     * Returns the full stacktrace of the exception that occurs when the job with the given id was last executed. Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the {@link TimerJobEntity} with the given id was last executed. Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getTimerJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the {@link SuspendedJobEntity} with the given id was last executed. Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getSuspendedJobExceptionStacktrace(String jobId);

    /**
     * Returns the full stacktrace of the exception that occurs when the {@link DeadLetterJobEntity} with the given id was last executed. Returns null when the job has no exception stacktrace.
     * 
     * @param jobId
     *            id of the job, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no job exists with the given id.
     */
    String getDeadLetterJobExceptionStacktrace(String jobId);

    /** get the list of properties. */
    Map<String, String> getProperties();

    /**
     * programmatic schema update on a given connection returning feedback about what happened
     */
    String databaseSchemaUpgrade(Connection connection, String catalog, String schema);

    /**
     * Executes a given command with the default {@link CommandConfig}.
     * 
     * @param command
     *            the command, cannot be null.
     * @return the result of command execution
     */
    <T> T executeCommand(Command<T> command);

    /**
     * Executes a given command with the specified {@link CommandConfig}.
     * 
     * @param config
     *            the command execution configuration, cannot be null.
     * @param command
     *            the command, cannot be null.
     * @return the result of command execution
     */
    <T> T executeCommand(CommandConfig config, Command<T> command);

    /**
     * Executes the sql contained in the {@link CustomSqlExecution} parameter.
     */
    <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution);

    /**
     * Returns a list of event log entries, describing everything the engine has processed. Note that the event logging must specifically must be enabled in the process engine configuration.
     * 
     * Passing null as arguments will effectively fetch ALL event log entries. Be careful, as this list might be huge!
     */
    List<EventLogEntry> getEventLogEntries(Long startLogNr, Long pageSize);

    /**
     * Returns a list of event log entries for a specific process instance id. Note that the event logging must specifically must be enabled in the process engine configuration.
     * 
     * Passing null as arguments will effectively fetch ALL event log entries. Be careful, as this list might be huge!
     */
    List<EventLogEntry> getEventLogEntriesByProcessInstanceId(String processInstanceId);

    /**
     * Delete a EventLogEntry. Typically only used in testing, as deleting log entries defeats the whole purpose of keeping a log.
     */
    void deleteEventLogEntry(long logNr);

}
