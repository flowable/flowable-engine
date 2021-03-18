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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.HistoryJobProcessor;
import org.flowable.job.service.HistoryJobProcessorContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobProcessor;
import org.flowable.job.service.JobProcessorContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.HistoryJobProcessorContextImpl;
import org.flowable.job.service.impl.JobProcessorContextImpl;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.TriggerAsyncHistoryExecutorTransactionListener;
import org.flowable.job.service.impl.persistence.entity.AbstractJobEntity;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJobManager implements JobManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobManager.class);

    public static final String CYCLE_TYPE = "cycle";

    protected JobServiceConfiguration jobServiceConfiguration;

    public DefaultJobManager(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public void createAsyncJob(JobEntity jobEntity, boolean exclusive) {
        // When the async executor is activated, the job is directly passed on to the async executor thread
        if (isAsyncExecutorActive()) {
            internalCreateLockedAsyncJob(jobEntity, exclusive);

        } else {
            internalCreateAsyncJob(jobEntity, exclusive);
        }
    }

    @Override
    public void scheduleAsyncJob(JobEntity jobEntity) {
        callJobProcessors(JobProcessorContext.Phase.BEFORE_CREATE, jobEntity);
        jobServiceConfiguration.getJobEntityManager().insert(jobEntity);
        triggerExecutorIfNeeded(jobEntity);
    }

    protected void triggerExecutorIfNeeded(JobEntity jobEntity) {
        // When the async executor is activated, the job is directly passed on to the async executor thread
        if (isAsyncExecutorActive()) {
            if (StringUtils.isNotEmpty(jobEntity.getCategory())) {
                if (jobServiceConfiguration.getEnabledJobCategories() != null && 
                        !jobServiceConfiguration.getEnabledJobCategories().contains(jobEntity.getCategory())) {
                    
                    return;
                }
            }
            
            hintAsyncExecutor(jobEntity);
        }
    }

    @Override
    public void scheduleTimerJob(TimerJobEntity timerJob) {
        scheduleTimer(timerJob);
        sendTimerScheduledEvent(timerJob);
    }

    private void scheduleTimer(TimerJobEntity timerJob) {
        if (timerJob == null) {
            throw new FlowableException("Empty timer job can not be scheduled");
        }
        callJobProcessors(JobProcessorContext.Phase.BEFORE_CREATE, timerJob);
        jobServiceConfiguration.getTimerJobEntityManager().insert(timerJob);
    }

    protected void sendTimerScheduledEvent(TimerJobEntity timerJob) {
        FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(
                    FlowableEngineEventType.TIMER_SCHEDULED, timerJob), jobServiceConfiguration.getEngineName());
        }
    }

    @Override
    public JobEntity moveTimerJobToExecutableJob(TimerJobEntity timerJob) {
        if (timerJob == null) {
            throw new FlowableException("Empty timer job can not be scheduled");
        }

        JobEntity executableJob = createExecutableJobFromOtherJob(timerJob);
        boolean insertSuccessful = jobServiceConfiguration.getJobEntityManager().insertJobEntity(executableJob);
        if (insertSuccessful) {
            jobServiceConfiguration.getTimerJobEntityManager().delete(timerJob);
            triggerExecutorIfNeeded(executableJob);
            return executableJob;
        }
        return null;
    }

    @Override
    public void bulkMoveTimerJobsToExecutableJobs(List<TimerJobEntity> timerJobEntities) {

        if (timerJobEntities == null || timerJobEntities.isEmpty()) {
            throw new FlowableException("Empty timer jobs collection can not be scheduled");
        }

        // Only hint when there is enough capacity remaining in the job queue
        boolean remainingCapacitySufficient = isAsyncExecutorRemainingCapacitySufficient(timerJobEntities.size());

        for (TimerJobEntity timerJobEntity : timerJobEntities) {
            JobEntity executableJob = createExecutableJobFromOtherJob(timerJobEntity, remainingCapacitySufficient);

            boolean insertSuccessful = jobServiceConfiguration.getJobEntityManager().insertJobEntity(executableJob);
            if (insertSuccessful && remainingCapacitySufficient) {
                triggerExecutorIfNeeded(executableJob);
            }
        }

        jobServiceConfiguration.getTimerJobEntityManager().bulkDeleteTimerJobsWithoutRevisionCheck(timerJobEntities);
    }

    @Override
    public JobEntity moveExternalWorkerJobToExecutableJob(ExternalWorkerJobEntity externalWorkerJob) {
        if (externalWorkerJob == null) {
            throw new FlowableException("Empty external worker job can not be scheduled");
        }

        JobEntity executableJob = createExecutableJobFromOtherJob(externalWorkerJob);
        // This job should now become a regular async job
        fillDefaultAsyncJobInfo(executableJob, executableJob.isExclusive());
        boolean insertSuccessful = jobServiceConfiguration.getJobEntityManager().insertJobEntity(executableJob);
        if (insertSuccessful) {
            jobServiceConfiguration.getExternalWorkerJobEntityManager().delete(externalWorkerJob);
            triggerExecutorIfNeeded(executableJob);
            return executableJob;
        }
        return null;
    }

    @Override
    public TimerJobEntity moveJobToTimerJob(AbstractRuntimeJobEntity job) {
        TimerJobEntity timerJob = createTimerJobFromOtherJob(job);
        boolean insertSuccessful = jobServiceConfiguration.getTimerJobEntityManager().insertTimerJobEntity(timerJob);
        if (insertSuccessful) {
            if (job instanceof JobEntity) {
                jobServiceConfiguration.getJobEntityManager().delete((JobEntity) job);
            } else if (job instanceof SuspendedJobEntity) {
                jobServiceConfiguration.getSuspendedJobEntityManager().delete((SuspendedJobEntity) job);
            }

            return timerJob;
        }
        return null;
    }

    @Override
    public SuspendedJobEntity moveJobToSuspendedJob(AbstractRuntimeJobEntity job) {
        SuspendedJobEntity suspendedJob = createSuspendedJobFromOtherJob(job);
        jobServiceConfiguration.getSuspendedJobEntityManager().insert(suspendedJob);
        if (job instanceof TimerJobEntity) {
            jobServiceConfiguration.getTimerJobEntityManager().delete((TimerJobEntity) job);

        } else if (job instanceof JobEntity) {
            jobServiceConfiguration.getJobEntityManager().delete((JobEntity) job);
        } else if (job instanceof ExternalWorkerJobEntity) {
            jobServiceConfiguration.getExternalWorkerJobEntityManager().delete((ExternalWorkerJobEntity) job);
        }

        return suspendedJob;
    }

    @Override
    public AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job) {
        AbstractRuntimeJobEntity activatedJob = null;
        if (Job.JOB_TYPE_TIMER.equals(job.getJobType())) {
            activatedJob = createTimerJobFromOtherJob(job);
            jobServiceConfiguration.getTimerJobEntityManager().insert((TimerJobEntity) activatedJob);

        } else if (Job.JOB_TYPE_EXTERNAL_WORKER.equals(job.getJobType())) {
            activatedJob = createExternalWorkerJobFromOtherJob(job);
            jobServiceConfiguration.getExternalWorkerJobEntityManager().insert((ExternalWorkerJobEntity) activatedJob);

        } else {
            activatedJob = createExecutableJobFromOtherJob(job);
            JobEntity jobEntity = (JobEntity) activatedJob;
            jobServiceConfiguration.getJobEntityManager().insert(jobEntity);
            triggerExecutorIfNeeded(jobEntity);
        }

        jobServiceConfiguration.getSuspendedJobEntityManager().delete(job);
        return activatedJob;
    }

    @Override
    public DeadLetterJobEntity moveJobToDeadLetterJob(AbstractRuntimeJobEntity job) {
        DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromOtherJob(job);
        jobServiceConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);
        if (job instanceof TimerJobEntity) {
            jobServiceConfiguration.getTimerJobEntityManager().delete((TimerJobEntity) job);

        } else if (job instanceof JobEntity) {
            jobServiceConfiguration.getJobEntityManager().delete((JobEntity) job);
        } else if (job instanceof ExternalWorkerJobEntity) {
            jobServiceConfiguration.getExternalWorkerJobEntityManager().delete((ExternalWorkerJobEntity) job);
        } else {
            throw new FlowableIllegalArgumentException("Cannot move the job to deadletter: the job is not a timer, async job or external worker job");
        }

        return deadLetterJob;
    }

    protected void sendMoveToDeadletterEvent(JobInfo job) {
        FlowableEventDispatcher eventDispatcher = jobServiceConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(
                FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER, job), jobServiceConfiguration.getEngineName());
        }
    }

    @Override
    public Job moveDeadLetterJobToExecutableJob(DeadLetterJobEntity deadLetterJobEntity, int retries) {
        if (deadLetterJobEntity == null) {
            throw new FlowableIllegalArgumentException("Null job provided");
        }

        if (HistoryJobEntity.HISTORY_JOB_TYPE.equals(deadLetterJobEntity.getJobType())) {
            throw new FlowableIllegalArgumentException("Cannot move a history job to an executable job");
        }

        if (Job.JOB_TYPE_EXTERNAL_WORKER.equals(deadLetterJobEntity.getJobType())) {
            ExternalWorkerJobEntity externalWorkerJob = createExternalWorkerJobFromOtherJob(deadLetterJobEntity);
            externalWorkerJob.setRetries(retries);
            boolean insertSuccessful = jobServiceConfiguration.getExternalWorkerJobEntityManager().insertExternalWorkerJobEntity(externalWorkerJob);
            if (insertSuccessful) {
                jobServiceConfiguration.getDeadLetterJobEntityManager().delete(deadLetterJobEntity);
                return externalWorkerJob;
            }
        } else {
            JobEntity executableJob = createExecutableJobFromOtherJob(deadLetterJobEntity);
            executableJob.setRetries(retries);
            boolean insertSuccessful = jobServiceConfiguration.getJobEntityManager().insertJobEntity(executableJob);
            if (insertSuccessful) {
                jobServiceConfiguration.getDeadLetterJobEntityManager().delete(deadLetterJobEntity);
                triggerExecutorIfNeeded(executableJob);
                return executableJob;
            }
        }

        return null;
    }

    @Override
    public HistoryJobEntity moveDeadLetterJobToHistoryJob(DeadLetterJobEntity deadLetterJobEntity, int retries) {
        if (deadLetterJobEntity == null) {
            throw new FlowableIllegalArgumentException("Null job provided");
        }

        if (!HistoryJobEntity.HISTORY_JOB_TYPE.equals(deadLetterJobEntity.getJobType())) {
            throw new FlowableIllegalArgumentException("Can only move a history job to a history job");
        }

        HistoryJobEntityManager historyJobEntityManager = jobServiceConfiguration.getHistoryJobEntityManager();
        HistoryJobEntity historyJobEntity = historyJobEntityManager.create();
        copyHistoryJobProperties(historyJobEntity, deadLetterJobEntity);

        historyJobEntity.setRetries(retries);
        historyJobEntity.setJobHandlerConfiguration(null); // special case: the deadletter jobConfiguration had the history json bytearray as reference in the configuration

        // Need to copy the bytes, because the delete of the deadLetterJobEntity will delete the byte array too
        // (which is needed when the deadLetterJob gets removed through the API service, so the byte array deletion can't be removed from there)
        ByteArrayEntity byteArrayEntity = getCommandContext().getEngineConfigurations().get(jobServiceConfiguration.getEngineName())
            .getByteArrayEntityManager().findById(deadLetterJobEntity.getJobHandlerConfiguration());
        historyJobEntity.setAdvancedJobHandlerConfigurationBytes(byteArrayEntity.getBytes());

        historyJobEntityManager.insert(historyJobEntity);
        jobServiceConfiguration.getDeadLetterJobEntityManager().delete(deadLetterJobEntity);

        return historyJobEntity;
    }

    @Override
    public void execute(JobInfo job) {
        if (job instanceof HistoryJobEntity) {
            callHistoryJobProcessors(HistoryJobProcessorContext.Phase.BEFORE_EXECUTE, (HistoryJobEntity) job);
            executeHistoryJob((HistoryJobEntity) job);
        } else if (job instanceof JobEntity) {
            callJobProcessors(JobProcessorContext.Phase.BEFORE_EXECUTE, (JobEntity) job);
            if (Job.JOB_TYPE_MESSAGE.equals(((Job) job).getJobType())) {
                executeMessageJob((JobEntity) job);
            } else if (Job.JOB_TYPE_TIMER.equals(((Job) job).getJobType())) {
                executeTimerJob((JobEntity) job);
            }

        } else {
            throw new FlowableException("Only jobs with type JobEntity are supported to be executed");
        }
    }

    @Override
    public void unacquire(JobInfo job) {

        if (job instanceof HistoryJob) {

            HistoryJobEntity jobEntity = (HistoryJobEntity) job;

            HistoryJobEntity newJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().create();
            copyHistoryJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            jobServiceConfiguration.getHistoryJobEntityManager().insert(newJobEntity);
            jobServiceConfiguration.getHistoryJobEntityManager().deleteNoCascade(jobEntity);

        } else if (job instanceof JobEntity) {

            // Deleting the old job and inserting it again with another id,
            // will avoid that the job is immediately is picked up again (for example
            // when doing lots of exclusive jobs for the same process instance)

            JobEntity jobEntity = (JobEntity) job;

            JobEntity newJobEntity = jobServiceConfiguration.getJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            jobServiceConfiguration.getJobEntityManager().insert(newJobEntity);
            jobServiceConfiguration.getJobEntityManager().delete(jobEntity.getId());

            // We're not calling triggerExecutorIfNeeded here after the insert. The unacquire happened
            // for a reason (eg queue full or exclusive lock failure). No need to try it immediately again,
            // as the chance of failure will be high.

        } else if (job instanceof ExternalWorkerJobEntity) {
            ExternalWorkerJobEntity jobEntity = (ExternalWorkerJobEntity) job;

            ExternalWorkerJobEntity newJobEntity = jobServiceConfiguration.getExternalWorkerJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            jobServiceConfiguration.getExternalWorkerJobEntityManager().insert(newJobEntity);
            jobServiceConfiguration.getExternalWorkerJobEntityManager().delete(jobEntity.getId());
        } else if (job instanceof TimerJobEntity) {
            jobServiceConfiguration.getTimerJobEntityManager().resetExpiredJob(job.getId());
        } else {
            if (job != null) {
                // It could be a v5 job, so simply unlock it.
                jobServiceConfiguration.getJobEntityManager().resetExpiredJob(job.getId());
            } else {
                throw new FlowableException("Programmatic error: null job passed");
            }
        }

    }

    @Override
    public void unacquireWithDecrementRetries(JobInfo job, Throwable exception) {
        if (job instanceof HistoryJob) {
            HistoryJobEntity historyJobEntity = (HistoryJobEntity) job;

            HistoryJobEntity newHistoryJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().create();
            copyHistoryJobInfo(newHistoryJobEntity, historyJobEntity);

            newHistoryJobEntity.setId(null); // We want a new id to be assigned to this job
            newHistoryJobEntity.setLockExpirationTime(null);
            newHistoryJobEntity.setLockOwner(null);
            newHistoryJobEntity.setCreateTime(jobServiceConfiguration.getClock().getCurrentTime());

            if (exception != null) {
                newHistoryJobEntity.setExceptionMessage(exception.getMessage());
                newHistoryJobEntity.setExceptionStacktrace(getExceptionStacktrace(exception));
            }

            if (historyJobEntity.getRetries() > 0) {
                newHistoryJobEntity.setRetries(newHistoryJobEntity.getRetries() - 1);
                jobServiceConfiguration.getHistoryJobEntityManager().insert(newHistoryJobEntity);

            } else {
                DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromHistoryJob(newHistoryJobEntity);

                if (exception != null) {
                    deadLetterJob.setExceptionMessage(exception.getMessage());

                    // Is copied from original HistoryJobEntity before and needs to be reset (as a new byteRef needs to be created)
                    if (deadLetterJob.getExceptionByteArrayRef() != null) {
                        deadLetterJob.getExceptionByteArrayRef().delete(jobServiceConfiguration.getEngineName());
                        deadLetterJob.setExceptionByteArrayRef(null);
                    }
                    deadLetterJob.setExceptionStacktrace(getExceptionStacktrace(exception));
                }

                jobServiceConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);

            }

            jobServiceConfiguration.getHistoryJobEntityManager().deleteNoCascade(historyJobEntity); // no cascade -> the bytearray ref is reused for either the new history job or the deadletter job

        } else {
            JobEntity jobEntity = (JobEntity) job;

            JobEntity newJobEntity = jobServiceConfiguration.getJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);

            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);

            if (exception != null) {
                newJobEntity.setExceptionMessage(exception.getMessage());
                newJobEntity.setExceptionStacktrace(getExceptionStacktrace(exception));
            }

            if (newJobEntity.getRetries() > 0) {
                newJobEntity.setRetries(newJobEntity.getRetries() - 1);
                jobServiceConfiguration.getJobEntityManager().insert(newJobEntity);

            } else {
                DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromOtherJob(newJobEntity);

                if (exception != null) {
                    deadLetterJob.setExceptionMessage(exception.getMessage());
                    deadLetterJob.setExceptionStacktrace(getExceptionStacktrace(exception));
                }

                jobServiceConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);

            }

            jobServiceConfiguration.getJobEntityManager().delete(jobEntity.getId());

            // We're not calling triggerExecutorIfNeeded here after the insert. The unacquire happened
            // for a reason (eg queue full or exclusive lock failure). No need to try it immediately again,
            // as the chance of failure will be high.

        }
    }

    protected String getExceptionStacktrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    protected void executeMessageJob(JobEntity jobEntity) {
        executeJobHandler(jobEntity);
        if (jobEntity.getId() != null) {
            jobServiceConfiguration.getJobEntityManager().delete(jobEntity);
        }
    }

    protected void executeHistoryJob(HistoryJobEntity historyJobEntity) {
        executeHistoryJobHandler(historyJobEntity);
        if (historyJobEntity.getId() != null) {
            jobServiceConfiguration.getHistoryJobEntityManager().delete(historyJobEntity);
        }
    }

    protected void executeTimerJob(JobEntity timerEntity) {
        TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration.getTimerJobEntityManager();

        VariableScope variableScope = null;
        if (jobServiceConfiguration.getInternalJobManager() != null) {
            variableScope = jobServiceConfiguration.getInternalJobManager().resolveVariableScope(timerEntity);
        }

        if (variableScope == null) {
            variableScope = NoExecutionVariableScope.getSharedInstance();
        }

        if (jobServiceConfiguration.getInternalJobManager() != null) {
            jobServiceConfiguration.getInternalJobManager().preTimerJobDelete(timerEntity, variableScope);
        }

        if (timerEntity.getDuedate() != null && !isValidTime(timerEntity, timerEntity.getDuedate(), variableScope)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Timer {} fired. but the dueDate is after the endDate.  Deleting timer.", timerEntity.getId());
            }
            jobServiceConfiguration.getJobEntityManager().delete(timerEntity);
            return;
        }

        executeJobHandler(timerEntity);
        jobServiceConfiguration.getJobEntityManager().delete(timerEntity);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Timer {} fired. Deleting timer.", timerEntity.getId());
        }

        if (timerEntity.getRepeat() != null) {
            TimerJobEntity newTimerJobEntity = timerJobEntityManager.createAndCalculateNextTimer(timerEntity, variableScope);
            if (newTimerJobEntity != null) {
                if (jobServiceConfiguration.getInternalJobManager() != null) {
                    jobServiceConfiguration.getInternalJobManager().preRepeatedTimerSchedule(newTimerJobEntity, variableScope);
                }
                
                scheduleTimerJob(newTimerJobEntity);
            }
        }
    }
    
    protected void executeJobHandler(JobEntity jobEntity) {
        VariableScope variableScope = null;
        if (jobServiceConfiguration.getInternalJobManager() != null) {
            variableScope = jobServiceConfiguration.getInternalJobManager().resolveVariableScope(jobEntity);
        }
        
        if (variableScope == null) {
            variableScope = NoExecutionVariableScope.getSharedInstance();
        }

        Map<String, JobHandler> jobHandlers = jobServiceConfiguration.getJobHandlers();
        if (jobEntity.getJobHandlerType() != null) {
            
            if (jobHandlers != null) {
                JobHandler jobHandler = jobHandlers.get(jobEntity.getJobHandlerType());
                if (jobHandler != null) {
                    jobHandler.execute(jobEntity, jobEntity.getJobHandlerConfiguration(), variableScope, getCommandContext());
                } else {
                    throw new FlowableException("No job handler registered for type " + jobEntity.getJobHandlerType() + 
                                    " in job config for engine: " + jobServiceConfiguration.getEngineName());
                }
                
            } else {
                throw new FlowableException("No job handler registered for type " + jobEntity.getJobHandlerType() +
                                " in job config for engine: " + jobServiceConfiguration.getEngineName());
            }
            
        } else {
            throw new FlowableException("Job has no job handler type in job config for engine: " + jobServiceConfiguration.getEngineName());
        }
    }

    protected void executeHistoryJobHandler(HistoryJobEntity historyJobEntity) {
        Map<String, HistoryJobHandler> jobHandlers = jobServiceConfiguration.getHistoryJobHandlers();
        if (historyJobEntity.getJobHandlerType() != null) {
            if (jobHandlers != null) {
                HistoryJobHandler jobHandler = jobHandlers.get(historyJobEntity.getJobHandlerType());
                if (jobHandler != null) {
                    jobHandler.execute(historyJobEntity, historyJobEntity.getJobHandlerConfiguration(), getCommandContext(), jobServiceConfiguration);
                } else {
                    throw new FlowableException("No history job handler registered for type " + historyJobEntity.getJobHandlerType() +
                                    " in job config for engine: " + jobServiceConfiguration.getEngineName());
                }
                
            } else {
                throw new FlowableException("No history job handler registered for type " + historyJobEntity.getJobHandlerType() + 
                                " in job config for engine: " + jobServiceConfiguration.getEngineName());
            }
            
        } else {
            throw new FlowableException("Async history job has no job handler type in job config for engine: " + jobServiceConfiguration.getEngineName());
        }
    }

    protected boolean isValidTime(JobEntity timerEntity, Date newTimerDate, VariableScope variableScope) {
        BusinessCalendar businessCalendar = jobServiceConfiguration.getBusinessCalendarManager().getBusinessCalendar(
                getBusinessCalendarName(timerEntity, variableScope));
        return businessCalendar.validateDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations(), timerEntity.getEndDate(), newTimerDate);
    }

    protected void hintAsyncExecutor(JobEntity job) {
        // Verify that correct properties have been set when the async executor will be hinted
        if (job.getLockOwner() == null || job.getLockExpirationTime() == null) {
            createAsyncJob(job, job.isExclusive());
        }
        createHintListeners(getAsyncExecutor(), job);
    }

    protected void createHintListeners(AsyncExecutor asyncExecutor, JobInfoEntity job) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        if (Context.getTransactionContext() != null) {
            JobAddedTransactionListener jobAddedTransactionListener = new JobAddedTransactionListener(job, asyncExecutor,
                    jobServiceConfiguration.getCommandExecutor());
            Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, jobAddedTransactionListener);
            
        } else {
            AsyncJobAddedNotification jobAddedNotification = new AsyncJobAddedNotification(job, asyncExecutor);
            commandContext.addCloseListener(jobAddedNotification);
            
        }
    }

    @Override
    public String getBusinessCalendarName(JobEntity timerEntity, VariableScope variableScope) {
        String calendarValue = null;
        if (StringUtils.isNotEmpty(timerEntity.getJobHandlerConfiguration())) {
            try {
                JsonNode jobConfigNode = jobServiceConfiguration.getObjectMapper().readTree(timerEntity.getJobHandlerConfiguration());
                JsonNode calendarNameNode = jobConfigNode.get("calendarName");
                if (calendarNameNode != null && !calendarNameNode.isNull()) {
                    calendarValue = calendarNameNode.asText();
                }

            } catch (Exception e) {
                // ignore JSON exception
            }
        }

        return getBusinessCalendarName(calendarValue, variableScope);
    }

    protected String getBusinessCalendarName(String calendarName, VariableScope variableScope) {
        String businessCalendarName = CYCLE_TYPE;
        if (StringUtils.isNotEmpty(calendarName)) {
            businessCalendarName = (String) jobServiceConfiguration.getExpressionManager()
                    .createExpression(calendarName).getValue(variableScope);
        }
        return businessCalendarName;
    }

    @Override
    public HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity) {
        callHistoryJobProcessors(HistoryJobProcessorContext.Phase.BEFORE_CREATE, historyJobEntity);
        jobServiceConfiguration.getHistoryJobEntityManager().insert(historyJobEntity);
        triggerAsyncHistoryExecutorIfNeeded(historyJobEntity);
        return historyJobEntity;
    }
    
    protected void triggerAsyncHistoryExecutorIfNeeded(HistoryJobEntity historyJobEntity) {
        if (isAsyncHistoryExecutorActive()) {
            hintAsyncHistoryExecutor(historyJobEntity);
        }
    }

    protected void hintAsyncHistoryExecutor(HistoryJobEntity historyJobEntity) {
        if (historyJobEntity.getLockOwner() == null || historyJobEntity.getLockExpirationTime() == null) {
            setLockTimeAndOwner(getAsyncHistoryExecutor(), historyJobEntity);
        }
        createAsyncHistoryHintListeners(historyJobEntity);
    }

    protected void createAsyncHistoryHintListeners(HistoryJobEntity historyJobEntity) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
        if (asyncHistorySession != null) {
            TransactionContext transactionContext = asyncHistorySession.getTransactionContext();
            if (transactionContext != null) {
                transactionContext.addTransactionListener(TransactionState.COMMITTED, new TriggerAsyncHistoryExecutorTransactionListener(
                        jobServiceConfiguration, historyJobEntity)); 
            }
        }
    }
    
    protected void internalCreateAsyncJob(JobEntity jobEntity, boolean exclusive) {
        fillDefaultAsyncJobInfo(jobEntity, exclusive);
    }

    protected void internalCreateLockedAsyncJob(JobEntity jobEntity, boolean exclusive) {
        fillDefaultAsyncJobInfo(jobEntity, exclusive);
        
        if (StringUtils.isNotEmpty(jobEntity.getCategory())) {
            if (jobServiceConfiguration.getEnabledJobCategories() != null && 
                    !jobServiceConfiguration.getEnabledJobCategories().contains(jobEntity.getCategory())) {
                
                return;
            }
        }
        
        setLockTimeAndOwner(getAsyncExecutor(), jobEntity);
    }

    protected void setLockTimeAndOwner(AsyncExecutor asyncExecutor , JobInfoEntity jobInfoEntity) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, asyncExecutor.getAsyncJobLockTimeInMillis());
        jobInfoEntity.setLockExpirationTime(gregorianCalendar.getTime());
        jobInfoEntity.setLockOwner(asyncExecutor.getLockOwner());
    }

    protected void fillDefaultAsyncJobInfo(JobEntity jobEntity, boolean exclusive) {
        jobEntity.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        jobEntity.setRevision(1);
        jobEntity.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());
        jobEntity.setExclusive(exclusive);
    }

    @Override
    public JobEntity createExecutableJobFromOtherJob(AbstractRuntimeJobEntity job) {
       return createExecutableJobFromOtherJob(job, isAsyncExecutorActive());
    }

    protected JobEntity createExecutableJobFromOtherJob(AbstractRuntimeJobEntity job, boolean lockJob) {
        JobEntity executableJob = jobServiceConfiguration.getJobEntityManager().create();
        copyJobInfo(executableJob, job);

        if (lockJob) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
            gregorianCalendar.add(Calendar.MILLISECOND, getAsyncExecutor().getAsyncJobLockTimeInMillis());
            executableJob.setLockExpirationTime(gregorianCalendar.getTime());
            executableJob.setLockOwner(getAsyncExecutor().getLockOwner());
        }

        return executableJob;
    }

    @Override
    public TimerJobEntity createTimerJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        TimerJobEntity timerJob = jobServiceConfiguration.getTimerJobEntityManager().create();
        copyJobInfo(timerJob, otherJob);
        return timerJob;
    }

    @Override
    public SuspendedJobEntity createSuspendedJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        SuspendedJobEntity suspendedJob = jobServiceConfiguration.getSuspendedJobEntityManager().create();
        copyJobInfo(suspendedJob, otherJob);
        return suspendedJob;
    }

    @Override
    public DeadLetterJobEntity createDeadLetterJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        DeadLetterJobEntity deadLetterJob = jobServiceConfiguration.getDeadLetterJobEntityManager().create();
        copyJobInfo(deadLetterJob, otherJob);
        sendMoveToDeadletterEvent(otherJob);
        return deadLetterJob;
    }

    @Override
    public DeadLetterJobEntity createDeadLetterJobFromHistoryJob(HistoryJobEntity historyJobEntity) {
        DeadLetterJobEntity deadLetterJob = jobServiceConfiguration.getDeadLetterJobEntityManager().create();
        deadLetterJob.setJobType(HistoryJob.HISTORY_JOB_TYPE);
        copyHistoryJobProperties(deadLetterJob, historyJobEntity);

        // History jobs don't use the configuration field. Deadletter jobs don't have an advanced configuration column.
        // To work around that, the id of the byte ref (of the advanced config) is copied to the configuration field.
        // The id will later be taken from the configuration field when moving back to a history job.
        if (historyJobEntity.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            deadLetterJob.setJobHandlerConfiguration(historyJobEntity.getAdvancedJobHandlerConfigurationByteArrayRef().getId());
        }

        sendMoveToDeadletterEvent(historyJobEntity);

        return deadLetterJob;
    }

    @Override
    public ExternalWorkerJobEntity createExternalWorkerJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        ExternalWorkerJobEntity externalWorkerJob = jobServiceConfiguration.getExternalWorkerJobEntityManager().create();
        copyJobInfo(externalWorkerJob, otherJob);
        return externalWorkerJob;
    }

    @Override
    public AbstractRuntimeJobEntity copyJobInfo(AbstractRuntimeJobEntity copyToJob, AbstractRuntimeJobEntity copyFromJob) {
        copyToJob.setDuedate(copyFromJob.getDuedate());
        copyToJob.setEndDate(copyFromJob.getEndDate());
        copyToJob.setExclusive(copyFromJob.isExclusive());
        copyToJob.setExecutionId(copyFromJob.getExecutionId());
        copyToJob.setId(copyFromJob.getId());
        copyToJob.setProcessDefinitionId(copyFromJob.getProcessDefinitionId());
        copyToJob.setElementId(copyFromJob.getElementId());
        copyToJob.setElementName(copyFromJob.getElementName());
        copyToJob.setProcessInstanceId(copyFromJob.getProcessInstanceId());
        copyToJob.setScopeId(copyFromJob.getScopeId());
        copyToJob.setSubScopeId(copyFromJob.getSubScopeId());
        copyToJob.setScopeType(copyFromJob.getScopeType());
        copyToJob.setScopeDefinitionId(copyFromJob.getScopeDefinitionId());
        copyToJob.setJobHandlerConfiguration(copyFromJob.getJobHandlerConfiguration());
        copyToJob.setCustomValues(copyFromJob.getCustomValues());
        copyToJob.setJobHandlerType(copyFromJob.getJobHandlerType());
        copyToJob.setCategory(copyFromJob.getCategory());
        copyToJob.setJobType(copyFromJob.getJobType());
        copyToJob.setExceptionMessage(copyFromJob.getExceptionMessage());
        copyToJob.setExceptionStacktrace(copyFromJob.getExceptionStacktrace());
        copyToJob.setMaxIterations(copyFromJob.getMaxIterations());
        copyToJob.setRepeat(copyFromJob.getRepeat());
        copyToJob.setRetries(copyFromJob.getRetries());
        copyToJob.setRevision(copyFromJob.getRevision());
        copyToJob.setTenantId(copyFromJob.getTenantId());

        if (copyFromJob.getCorrelationId() != null) {
            copyToJob.setCorrelationId(copyFromJob.getCorrelationId());
        } else {
            copyToJob.setCorrelationId(jobServiceConfiguration.getIdGenerator().getNextId());
        }

        return copyToJob;
    }

    protected HistoryJobEntity copyHistoryJobInfo(HistoryJobEntity copyToJob, HistoryJobEntity copyFromJob) {
        copyHistoryJobProperties(copyToJob, copyFromJob);
        if (copyFromJob.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            ByteArrayRef configurationByteArrayRefCopy = copyFromJob.getAdvancedJobHandlerConfigurationByteArrayRef().copy();
            copyToJob.setAdvancedJobHandlerConfigurationByteArrayRef(configurationByteArrayRefCopy);
        }
        return copyToJob;
    }

    protected AbstractJobEntity copyHistoryJobProperties(AbstractJobEntity copyToJob, AbstractJobEntity copyFromJob) {
        copyToJob.setId(copyFromJob.getId());
        copyToJob.setScopeType(copyFromJob.getScopeType());
        copyToJob.setCreateTime(copyFromJob.getCreateTime());
        copyToJob.setJobHandlerConfiguration(copyFromJob.getJobHandlerConfiguration());
        if (copyFromJob.getExceptionByteArrayRef() != null) {
            ByteArrayRef exceptionByteArrayRefCopy = copyFromJob.getExceptionByteArrayRef();
            copyToJob.setExceptionByteArrayRef(exceptionByteArrayRefCopy);
        }
        if (copyFromJob.getCustomValuesByteArrayRef() != null) {
            ByteArrayRef customValuesByteArrayRefCopy = copyFromJob.getCustomValuesByteArrayRef().copy();
            copyToJob.setCustomValuesByteArrayRef(customValuesByteArrayRefCopy);
        }
        copyToJob.setJobHandlerType(copyFromJob.getJobHandlerType());
        copyToJob.setExceptionMessage(copyFromJob.getExceptionMessage());
        copyToJob.setExceptionStacktrace(copyFromJob.getExceptionStacktrace());
        copyToJob.setCustomValues(copyFromJob.getCustomValues());
        copyToJob.setRetries(copyFromJob.getRetries());
        copyToJob.setRevision(copyFromJob.getRevision());
        copyToJob.setTenantId(copyFromJob.getTenantId());

        return copyToJob;
    }

    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    @Override
    public void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    protected boolean isAsyncExecutorActive() {
        return isExecutorActive(jobServiceConfiguration.getAsyncExecutor());
    }

    protected boolean isAsyncExecutorRemainingCapacitySufficient(int neededCapacity) {
        return getAsyncExecutor().isActive() && getAsyncExecutor().getTaskExecutor().getRemainingCapacity() >= neededCapacity;
    }
    
    protected boolean isAsyncHistoryExecutorActive() {
        return isExecutorActive(jobServiceConfiguration.getAsyncHistoryExecutor());
    }
    
    protected boolean isExecutorActive(AsyncExecutor asyncExecutor) {
        return asyncExecutor != null && asyncExecutor.isActive();
    }

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected AsyncExecutor getAsyncExecutor() {
        return jobServiceConfiguration.getAsyncExecutor();
    }
    
    protected AsyncExecutor getAsyncHistoryExecutor() {
        return jobServiceConfiguration.getAsyncHistoryExecutor();
    }

    protected void callJobProcessors(JobProcessorContext.Phase processorType, AbstractJobEntity abstractJobEntity) {
        if (jobServiceConfiguration.getJobProcessors() != null) {
            JobProcessorContextImpl jobProcessorContext = new JobProcessorContextImpl(processorType, abstractJobEntity);
            for (JobProcessor jobProcessor : jobServiceConfiguration.getJobProcessors()) {
                jobProcessor.process(jobProcessorContext);
            }
        }
    }

    protected void callHistoryJobProcessors(HistoryJobProcessorContext.Phase processorType, HistoryJobEntity historyJobEntity) {
        if (jobServiceConfiguration.getHistoryJobProcessors() != null) {
            HistoryJobProcessorContextImpl historyJobProcessorContext = new HistoryJobProcessorContextImpl(processorType, historyJobEntity);
            for (HistoryJobProcessor historyJobProcessor : jobServiceConfiguration.getHistoryJobProcessors()) {
                historyJobProcessor.process(historyJobProcessorContext);
            }
        }
    }

}
