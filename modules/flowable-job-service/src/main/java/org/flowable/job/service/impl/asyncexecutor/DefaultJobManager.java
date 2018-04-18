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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
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
import org.flowable.job.service.impl.persistence.entity.AbstractJobEntity;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayRef;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
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

    public DefaultJobManager() {
    }

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

    private void sendTimerScheduledEvent(TimerJobEntity timerJob) {
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                    FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_SCHEDULED, timerJob));
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
        }

        return suspendedJob;
    }

    @Override
    public AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job) {
        AbstractRuntimeJobEntity activatedJob = null;
        if (Job.JOB_TYPE_TIMER.equals(job.getJobType())) {
            activatedJob = createTimerJobFromOtherJob(job);
            jobServiceConfiguration.getTimerJobEntityManager().insert((TimerJobEntity) activatedJob);

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
        }

        return deadLetterJob;
    }

    @Override
    public JobEntity moveDeadLetterJobToExecutableJob(DeadLetterJobEntity deadLetterJobEntity, int retries) {
        if (deadLetterJobEntity == null) {
            throw new FlowableIllegalArgumentException("Null job provided");
        }

        JobEntity executableJob = createExecutableJobFromOtherJob(deadLetterJobEntity);
        executableJob.setRetries(retries);
        boolean insertSuccessful = jobServiceConfiguration.getJobEntityManager().insertJobEntity(executableJob);
        if (insertSuccessful) {
            jobServiceConfiguration.getDeadLetterJobEntityManager().delete(deadLetterJobEntity);
            triggerExecutorIfNeeded(executableJob);
            return executableJob;
        }
        return null;
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

        } else {
            // It could be a v5 job, so simply unlock it.
            jobServiceConfiguration.getJobEntityManager().resetExpiredJob(job.getId());
        }

    }

    @Override
    public void unacquireWithDecrementRetries(JobInfo job) {
        if (job instanceof HistoryJob) {
            HistoryJobEntity historyJobEntity = (HistoryJobEntity) job;

            if (historyJobEntity.getRetries() > 0) {
                HistoryJobEntity newHistoryJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().create();
                copyHistoryJobInfo(newHistoryJobEntity, historyJobEntity);
                newHistoryJobEntity.setId(null); // We want a new id to be assigned to this job
                newHistoryJobEntity.setLockExpirationTime(null);
                newHistoryJobEntity.setLockOwner(null);
                newHistoryJobEntity.setCreateTime(jobServiceConfiguration.getClock().getCurrentTime());

                newHistoryJobEntity.setRetries(newHistoryJobEntity.getRetries() - 1);
                jobServiceConfiguration.getHistoryJobEntityManager().insert(newHistoryJobEntity);
                jobServiceConfiguration.getHistoryJobEntityManager().deleteNoCascade(historyJobEntity);
            
            } else {
                jobServiceConfiguration.getHistoryJobEntityManager().delete(historyJobEntity);
            }

        } else {
            JobEntity jobEntity = (JobEntity) job;

            JobEntity newJobEntity = jobServiceConfiguration.getJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);

            if (newJobEntity.getRetries() > 0) {
                newJobEntity.setRetries(newJobEntity.getRetries() - 1);
                jobServiceConfiguration.getJobEntityManager().insert(newJobEntity);

            } else {
                DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromOtherJob(newJobEntity);
                jobServiceConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);
            }

            jobServiceConfiguration.getJobEntityManager().delete(jobEntity.getId());

            // We're not calling triggerExecutorIfNeeded here after the insert. The unacquire happened
            // for a reason (eg queue full or exclusive lock failure). No need to try it immediately again,
            // as the chance of failure will be high.

        }
    }

    protected void executeMessageJob(JobEntity jobEntity) {
        executeJobHandler(jobEntity);
        if (jobEntity.getId() != null) {
            CommandContextUtil.getJobEntityManager().delete(jobEntity);
        }
    }

    protected void executeHistoryJob(HistoryJobEntity historyJobEntity) {
        executeHistoryJobHandler(historyJobEntity);
        if (historyJobEntity.getId() != null) {
            CommandContextUtil.getHistoryJobEntityManager().delete(historyJobEntity);
        }
    }

    protected void executeTimerJob(JobEntity timerEntity) {
        TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration.getTimerJobEntityManager();

        VariableScope variableScope = jobServiceConfiguration.getInternalJobManager().resolveVariableScope(timerEntity);

        if (variableScope == null) {
            variableScope = NoExecutionVariableScope.getSharedInstance();
        }

        jobServiceConfiguration.getInternalJobManager().preTimerJobDelete(timerEntity, variableScope);

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
                jobServiceConfiguration.getInternalJobManager().preRepeatedTimerSchedule(newTimerJobEntity, variableScope);
                scheduleTimerJob(newTimerJobEntity);
            }
        }
    }
    
    protected void executeJobHandler(JobEntity jobEntity) {
        VariableScope variableScope = jobServiceConfiguration.getInternalJobManager().resolveVariableScope(jobEntity);

        Map<String, JobHandler> jobHandlers = jobServiceConfiguration.getJobHandlers();
        JobHandler jobHandler = jobHandlers.get(jobEntity.getJobHandlerType());
        jobHandler.execute(jobEntity, jobEntity.getJobHandlerConfiguration(), variableScope, getCommandContext());
    }

    protected void executeHistoryJobHandler(HistoryJobEntity historyJobEntity) {
        Map<String, HistoryJobHandler> jobHandlers = jobServiceConfiguration.getHistoryJobHandlers();
        HistoryJobHandler jobHandler = jobHandlers.get(historyJobEntity.getJobHandlerType());
        jobHandler.execute(historyJobEntity, historyJobEntity.getJobHandlerConfiguration(), getCommandContext());
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
        
        if (Context.getTransactionContext() != null) {
            JobAddedTransactionListener jobAddedTransactionListener = new JobAddedTransactionListener(job, getAsyncExecutor(),
                            CommandContextUtil.getJobServiceConfiguration().getCommandExecutor());
            Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, jobAddedTransactionListener);
        } else {
            AsyncJobAddedNotification jobAddedNotification = new AsyncJobAddedNotification(job, getAsyncExecutor());
            getCommandContext().addCloseListener(jobAddedNotification);
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
            businessCalendarName = (String) CommandContextUtil.getJobServiceConfiguration().getExpressionManager()
                    .createExpression(calendarName).getValue(variableScope);
        }
        return businessCalendarName;
    }

    @Override
    public HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity) {
        callHistoryJobProcessors(HistoryJobProcessorContext.Phase.BEFORE_CREATE, historyJobEntity);
        jobServiceConfiguration.getHistoryJobEntityManager().insert(historyJobEntity);
        return historyJobEntity;
    }

    protected void internalCreateAsyncJob(JobEntity jobEntity, boolean exclusive) {
        fillDefaultAsyncJobInfo(jobEntity, exclusive);
    }

    protected void internalCreateLockedAsyncJob(JobEntity jobEntity, boolean exclusive) {
        fillDefaultAsyncJobInfo(jobEntity, exclusive);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, getAsyncExecutor().getAsyncJobLockTimeInMillis());
        jobEntity.setLockExpirationTime(gregorianCalendar.getTime());
        jobEntity.setLockOwner(getAsyncExecutor().getLockOwner());
    }

    protected void fillDefaultAsyncJobInfo(JobEntity jobEntity, boolean exclusive) {
        jobEntity.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        jobEntity.setRevision(1);
        jobEntity.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());
        jobEntity.setExclusive(exclusive);
    }

    protected JobEntity createExecutableJobFromOtherJob(AbstractRuntimeJobEntity job) {
        JobEntity executableJob = jobServiceConfiguration.getJobEntityManager().create();
        copyJobInfo(executableJob, job);

        if (isAsyncExecutorActive()) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
            gregorianCalendar.add(Calendar.MILLISECOND, getAsyncExecutor().getTimerLockTimeInMillis());
            executableJob.setLockExpirationTime(gregorianCalendar.getTime());
            executableJob.setLockOwner(getAsyncExecutor().getLockOwner());
        }

        return executableJob;
    }

    protected TimerJobEntity createTimerJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        TimerJobEntity timerJob = jobServiceConfiguration.getTimerJobEntityManager().create();
        copyJobInfo(timerJob, otherJob);
        return timerJob;
    }

    protected SuspendedJobEntity createSuspendedJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        SuspendedJobEntity suspendedJob = jobServiceConfiguration.getSuspendedJobEntityManager().create();
        copyJobInfo(suspendedJob, otherJob);
        return suspendedJob;
    }

    protected DeadLetterJobEntity createDeadLetterJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        DeadLetterJobEntity deadLetterJob = jobServiceConfiguration.getDeadLetterJobEntityManager().create();
        copyJobInfo(deadLetterJob, otherJob);
        return deadLetterJob;
    }

    protected AbstractRuntimeJobEntity copyJobInfo(AbstractRuntimeJobEntity copyToJob, AbstractRuntimeJobEntity copyFromJob) {
        copyToJob.setDuedate(copyFromJob.getDuedate());
        copyToJob.setEndDate(copyFromJob.getEndDate());
        copyToJob.setExclusive(copyFromJob.isExclusive());
        copyToJob.setExecutionId(copyFromJob.getExecutionId());
        copyToJob.setId(copyFromJob.getId());
        copyToJob.setJobHandlerConfiguration(copyFromJob.getJobHandlerConfiguration());
        copyToJob.setCustomValues(copyFromJob.getCustomValues());
        copyToJob.setJobHandlerType(copyFromJob.getJobHandlerType());
        copyToJob.setJobType(copyFromJob.getJobType());
        copyToJob.setExceptionMessage(copyFromJob.getExceptionMessage());
        copyToJob.setExceptionStacktrace(copyFromJob.getExceptionStacktrace());
        copyToJob.setMaxIterations(copyFromJob.getMaxIterations());
        copyToJob.setProcessDefinitionId(copyFromJob.getProcessDefinitionId());
        copyToJob.setProcessInstanceId(copyFromJob.getProcessInstanceId());
        copyToJob.setScopeId(copyFromJob.getScopeId());
        copyToJob.setSubScopeId(copyFromJob.getSubScopeId());
        copyToJob.setScopeType(copyFromJob.getScopeType());
        copyToJob.setScopeDefinitionId(copyFromJob.getScopeDefinitionId());
        copyToJob.setRepeat(copyFromJob.getRepeat());
        copyToJob.setRetries(copyFromJob.getRetries());
        copyToJob.setRevision(copyFromJob.getRevision());
        copyToJob.setTenantId(copyFromJob.getTenantId());

        return copyToJob;
    }

    protected HistoryJobEntity copyHistoryJobInfo(HistoryJobEntity copyToJob, HistoryJobEntity copyFromJob) {
        copyToJob.setId(copyFromJob.getId());
        copyToJob.setJobHandlerConfiguration(copyFromJob.getJobHandlerConfiguration());
        if (copyFromJob.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            JobByteArrayRef configurationByteArrayRefCopy = copyFromJob.getAdvancedJobHandlerConfigurationByteArrayRef().copy();
            copyToJob.setAdvancedJobHandlerConfigurationByteArrayRef(configurationByteArrayRefCopy);
        }
        if (copyFromJob.getExceptionByteArrayRef() != null) {
            JobByteArrayRef exceptionByteArrayRefCopy = copyFromJob.getExceptionByteArrayRef();
            copyToJob.setExceptionByteArrayRef(exceptionByteArrayRefCopy);
        }
        if (copyFromJob.getCustomValuesByteArrayRef() != null) {
            JobByteArrayRef customValuesByteArrayRefCopy = copyFromJob.getCustomValuesByteArrayRef().copy();
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
        return jobServiceConfiguration.getAsyncExecutor() != null
                && jobServiceConfiguration.getAsyncExecutor().isActive();
    }

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected AsyncExecutor getAsyncExecutor() {
        return jobServiceConfiguration.getAsyncExecutor();
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
