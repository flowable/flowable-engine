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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.impl.cfg.TransactionState;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.calendar.BusinessCalendar;
import org.flowable.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.NoExecutionVariableScope;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncJobAddedNotification;
import org.flowable.engine.impl.jobexecutor.HistoryJobHandler;
import org.flowable.engine.impl.jobexecutor.JobAddedTransactionListener;
import org.flowable.engine.impl.jobexecutor.JobHandler;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.engine.runtime.HistoryJob;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJobManager implements JobManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobManager.class);

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultJobManager() {
    }

    public DefaultJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public JobEntity createAsyncJob(ExecutionEntity execution, boolean exclusive) {
        JobEntity jobEntity = null;
        // When the async executor is activated, the job is directly passed on to the async executor thread
        if (isAsyncExecutorActive()) {
            jobEntity = internalCreateLockedAsyncJob(execution, exclusive);

        } else {
            jobEntity = internalCreateAsyncJob(execution, exclusive);
        }

        return jobEntity;
    }

    @Override
    public void scheduleAsyncJob(JobEntity jobEntity) {
        processEngineConfiguration.getJobEntityManager().insert(jobEntity);
        triggerExecutorIfNeeded(jobEntity);
    }

    protected void triggerExecutorIfNeeded(JobEntity jobEntity) {
        // When the async executor is activated, the job is directly passed on to the async executor thread
        if (isAsyncExecutorActive()) {
            hintAsyncExecutor(jobEntity);
        }
    }

    @Override
    public TimerJobEntity createTimerJob(TimerEventDefinition timerEventDefinition, boolean interrupting,
            ExecutionEntity execution, String timerEventType, String jobHandlerConfiguration) {

        TimerJobEntity timerEntity = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, interrupting,
                execution, timerEventType, jobHandlerConfiguration);

        return timerEntity;
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
        processEngineConfiguration.getTimerJobEntityManager().insert(timerJob);
    }

    private void sendTimerScheduledEvent(TimerJobEntity timerJob) {
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_SCHEDULED, timerJob));
        }
    }

    public TimerJobEntity rescheduleTimerJob(String timerJobId, TimerEventDefinition timerEventDefinition) {
        TimerJobEntityManager jobManager = processEngineConfiguration.getTimerJobEntityManager();
        TimerJobEntity timerJob = jobManager.findById(timerJobId);
        if (timerJob != null) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(timerJob.getProcessDefinitionId());
            Event eventElement = (Event) bpmnModel.getFlowElement(TimerEventHandler.getActivityIdFromConfiguration(timerJob.getJobHandlerConfiguration()));
            boolean isInterruptingTimer = false;
            if (eventElement instanceof BoundaryEvent) {
                isInterruptingTimer = ((BoundaryEvent) eventElement).isCancelActivity();
            }

            ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().findById(timerJob.getExecutionId());
            TimerJobEntity rescheduledTimerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, isInterruptingTimer, execution,
                    timerJob.getJobHandlerType(), timerJob.getJobHandlerConfiguration());

            processEngineConfiguration.getTimerJobEntityManager().delete(timerJob);
            scheduleTimer(rescheduledTimerJob);

            if (CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createJobRescheduledEvent(FlowableEngineEventType.JOB_RESCHEDULED, rescheduledTimerJob, timerJob.getId()));
            }

            // job rescheduled event should occur before new timer scheduled event
            sendTimerScheduledEvent(rescheduledTimerJob);

            return rescheduledTimerJob;
        }
        return null;
    }

    @Override
    public JobEntity moveTimerJobToExecutableJob(TimerJobEntity timerJob) {
        if (timerJob == null) {
            throw new FlowableException("Empty timer job can not be scheduled");
        }

        JobEntity executableJob = createExecutableJobFromOtherJob(timerJob);
        boolean insertSuccessful = processEngineConfiguration.getJobEntityManager().insertJobEntity(executableJob);
        if (insertSuccessful) {
            processEngineConfiguration.getTimerJobEntityManager().delete(timerJob);
            triggerExecutorIfNeeded(executableJob);
            return executableJob;
        }
        return null;
    }

    @Override
    public TimerJobEntity moveJobToTimerJob(AbstractRuntimeJobEntity job) {
        TimerJobEntity timerJob = createTimerJobFromOtherJob(job);
        boolean insertSuccessful = processEngineConfiguration.getTimerJobEntityManager().insertTimerJobEntity(timerJob);
        if (insertSuccessful) {
            if (job instanceof JobEntity) {
                processEngineConfiguration.getJobEntityManager().delete((JobEntity) job);
            } else if (job instanceof SuspendedJobEntity) {
                processEngineConfiguration.getSuspendedJobEntityManager().delete((SuspendedJobEntity) job);
            }

            return timerJob;
        }
        return null;
    }

    @Override
    public SuspendedJobEntity moveJobToSuspendedJob(AbstractRuntimeJobEntity job) {
        SuspendedJobEntity suspendedJob = createSuspendedJobFromOtherJob(job);
        processEngineConfiguration.getSuspendedJobEntityManager().insert(suspendedJob);
        if (job instanceof TimerJobEntity) {
            processEngineConfiguration.getTimerJobEntityManager().delete((TimerJobEntity) job);

        } else if (job instanceof JobEntity) {
            processEngineConfiguration.getJobEntityManager().delete((JobEntity) job);
        }

        return suspendedJob;
    }

    @Override
    public AbstractRuntimeJobEntity activateSuspendedJob(SuspendedJobEntity job) {
        AbstractRuntimeJobEntity activatedJob = null;
        if (Job.JOB_TYPE_TIMER.equals(job.getJobType())) {
            activatedJob = createTimerJobFromOtherJob(job);
            processEngineConfiguration.getTimerJobEntityManager().insert((TimerJobEntity) activatedJob);

        } else {
            activatedJob = createExecutableJobFromOtherJob(job);
            JobEntity jobEntity = (JobEntity) activatedJob;
            processEngineConfiguration.getJobEntityManager().insert(jobEntity);
            triggerExecutorIfNeeded(jobEntity);
        }

        processEngineConfiguration.getSuspendedJobEntityManager().delete(job);
        return activatedJob;
    }

    @Override
    public DeadLetterJobEntity moveJobToDeadLetterJob(AbstractRuntimeJobEntity job) {
        DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromOtherJob(job);
        processEngineConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);
        if (job instanceof TimerJobEntity) {
            processEngineConfiguration.getTimerJobEntityManager().delete((TimerJobEntity) job);

        } else if (job instanceof JobEntity) {
            processEngineConfiguration.getJobEntityManager().delete((JobEntity) job);
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
        boolean insertSuccessful = processEngineConfiguration.getJobEntityManager().insertJobEntity(executableJob);
        if (insertSuccessful) {
            processEngineConfiguration.getDeadLetterJobEntityManager().delete(deadLetterJobEntity);
            triggerExecutorIfNeeded(executableJob);
            return executableJob;
        }
        return null;
    }

    @Override
    public void execute(JobInfo job) {
        if (job instanceof HistoryJobEntity) {
            executeHistoryJob((HistoryJobEntity) job);
        } else if (job instanceof JobEntity) {
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

            HistoryJobEntity newJobEntity = processEngineConfiguration.getHistoryJobEntityManager().create();
            copyHistoryJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            processEngineConfiguration.getHistoryJobEntityManager().insert(newJobEntity);
            processEngineConfiguration.getHistoryJobEntityManager().deleteNoCascade(jobEntity);
            
        } else if (job instanceof JobEntity) {
            
            // Deleting the old job and inserting it again with another id,
            // will avoid that the job is immediately is picked up again (for example
            // when doing lots of exclusive jobs for the same process instance)
            
            JobEntity jobEntity = (JobEntity) job;

            JobEntity newJobEntity = processEngineConfiguration.getJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            processEngineConfiguration.getJobEntityManager().insert(newJobEntity);
            processEngineConfiguration.getJobEntityManager().delete(jobEntity.getId());

            // We're not calling triggerExecutorIfNeeded here after the insert. The unacquire happened
            // for a reason (eg queue full or exclusive lock failure). No need to try it immediately again,
            // as the chance of failure will be high.

        } else {
            // It could be a v5 job, so simply unlock it.
            processEngineConfiguration.getJobEntityManager().resetExpiredJob(job.getId());
        }

    }
    
    @Override
    public void unacquireWithDecrementRetries(JobInfo job) {
        if (job instanceof HistoryJob) {
            HistoryJobEntity historyJobEntity = (HistoryJobEntity) job;
            
            if (historyJobEntity.getRetries() > 0) {
                HistoryJobEntity newHistoryJobEntity = processEngineConfiguration.getHistoryJobEntityManager().create();
                copyHistoryJobInfo(newHistoryJobEntity, historyJobEntity);
                newHistoryJobEntity.setId(null); // We want a new id to be assigned to this job
                newHistoryJobEntity.setLockExpirationTime(null);
                newHistoryJobEntity.setLockOwner(null);
                newHistoryJobEntity.setCreateTime(processEngineConfiguration.getClock().getCurrentTime());
            
                newHistoryJobEntity.setRetries(newHistoryJobEntity.getRetries() - 1);
                processEngineConfiguration.getHistoryJobEntityManager().insert(newHistoryJobEntity);
                
            }
            
            processEngineConfiguration.getHistoryJobEntityManager().deleteNoCascade(historyJobEntity);
            
        } else {
            JobEntity jobEntity = (JobEntity) job;
            
            JobEntity newJobEntity = processEngineConfiguration.getJobEntityManager().create();
            copyJobInfo(newJobEntity, jobEntity);
            newJobEntity.setId(null); // We want a new id to be assigned to this job
            newJobEntity.setLockExpirationTime(null);
            newJobEntity.setLockOwner(null);
            
            if (newJobEntity.getRetries() > 0) {
                newJobEntity.setRetries(newJobEntity.getRetries() - 1);
                processEngineConfiguration.getJobEntityManager().insert(newJobEntity);
                
            } else {
                DeadLetterJobEntity deadLetterJob = createDeadLetterJobFromOtherJob(newJobEntity);
                processEngineConfiguration.getDeadLetterJobEntityManager().insert(deadLetterJob);
            }
            
            processEngineConfiguration.getJobEntityManager().delete(jobEntity.getId());
    
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
        TimerJobEntityManager timerJobEntityManager = processEngineConfiguration.getTimerJobEntityManager();

        VariableScope variableScope = null;
        if (timerEntity.getExecutionId() != null) {
            variableScope = getExecutionEntityManager().findById(timerEntity.getExecutionId());
        }

        if (variableScope == null) {
            variableScope = NoExecutionVariableScope.getSharedInstance();
        }

        // set endDate if it was set to the definition
        restoreExtraData(timerEntity, variableScope);

        if (timerEntity.getDuedate() != null && !isValidTime(timerEntity, timerEntity.getDuedate(), variableScope)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Timer {} fired. but the dueDate is after the endDate.  Deleting timer.", timerEntity.getId());
            }
            processEngineConfiguration.getJobEntityManager().delete(timerEntity);
            return;
        }

        executeJobHandler(timerEntity);
        processEngineConfiguration.getJobEntityManager().delete(timerEntity);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Timer {} fired. Deleting timer.", timerEntity.getId());
        }

        if (timerEntity.getRepeat() != null) {
            TimerJobEntity newTimerJobEntity = timerJobEntityManager.createAndCalculateNextTimer(timerEntity, variableScope);
            if (newTimerJobEntity != null) {
                scheduleTimerJob(newTimerJobEntity);
            }
        }
    }

    protected void executeJobHandler(JobEntity jobEntity) {
        ExecutionEntity execution = null;
        if (jobEntity.getExecutionId() != null) {
            execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
        }

        Map<String, JobHandler> jobHandlers = processEngineConfiguration.getJobHandlers();
        JobHandler jobHandler = jobHandlers.get(jobEntity.getJobHandlerType());
        jobHandler.execute(jobEntity, jobEntity.getJobHandlerConfiguration(), execution, getCommandContext());
    }
    
    protected void executeHistoryJobHandler(HistoryJobEntity historyJobEntity) {
        Map<String, HistoryJobHandler> jobHandlers = processEngineConfiguration.getHistoryJobHandlers();
        HistoryJobHandler jobHandler = jobHandlers.get(historyJobEntity.getJobHandlerType());
        jobHandler.execute(historyJobEntity, historyJobEntity.getJobHandlerConfiguration(), getCommandContext());
    }

    protected void restoreExtraData(JobEntity timerEntity, VariableScope variableScope) {
        String activityId = timerEntity.getJobHandlerConfiguration();

        if (timerEntity.getJobHandlerType().equalsIgnoreCase(TimerStartEventJobHandler.TYPE) ||
                timerEntity.getJobHandlerType().equalsIgnoreCase(TriggerTimerEventJobHandler.TYPE)) {

            activityId = TimerEventHandler.getActivityIdFromConfiguration(timerEntity.getJobHandlerConfiguration());
            String endDateExpressionString = TimerEventHandler.getEndDateFromConfiguration(timerEntity.getJobHandlerConfiguration());

            if (endDateExpressionString != null) {
                Expression endDateExpression = processEngineConfiguration.getExpressionManager().createExpression(endDateExpressionString);

                String endDateString = null;

                BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(
                        getBusinessCalendarName(TimerEventHandler.geCalendarNameFromConfiguration(timerEntity.getJobHandlerConfiguration()), variableScope));

                if (endDateExpression != null) {
                    Object endDateValue = endDateExpression.getValue(variableScope);
                    if (endDateValue instanceof String) {
                        endDateString = (String) endDateValue;
                    } else if (endDateValue instanceof Date) {
                        timerEntity.setEndDate((Date) endDateValue);
                    } else {
                        throw new FlowableException("Timer '" + ((ExecutionEntity) variableScope).getActivityId()
                                + "' was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
                    }

                    if (timerEntity.getEndDate() == null) {
                        timerEntity.setEndDate(businessCalendar.resolveEndDate(endDateString));
                    }
                }
            }
        }

        int maxIterations = 1;
        if (timerEntity.getProcessDefinitionId() != null) {
            org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(timerEntity.getProcessDefinitionId());
            maxIterations = getMaxIterations(process, activityId);
            if (maxIterations <= 1) {
                maxIterations = getMaxIterations(process, activityId);
            }
        }
        timerEntity.setMaxIterations(maxIterations);
    }

    protected int getMaxIterations(org.flowable.bpmn.model.Process process, String activityId) {
        FlowElement flowElement = process.getFlowElement(activityId, true);
        if (flowElement != null) {
            if (flowElement instanceof Event) {

                Event event = (Event) flowElement;
                List<EventDefinition> eventDefinitions = event.getEventDefinitions();

                if (eventDefinitions != null) {

                    for (EventDefinition eventDefinition : eventDefinitions) {
                        if (eventDefinition instanceof TimerEventDefinition) {
                            TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
                            if (timerEventDefinition.getTimeCycle() != null) {
                                return calculateMaxIterationsValue(timerEventDefinition.getTimeCycle());
                            }
                        }
                    }

                }

            }
        }
        return -1;
    }

    protected int calculateMaxIterationsValue(String originalExpression) {
        int times = Integer.MAX_VALUE;
        List<String> expression = Arrays.asList(originalExpression.split("/"));
        if (expression.size() > 1 && expression.get(0).startsWith("R")) {
            times = Integer.MAX_VALUE;
            if (expression.get(0).length() > 1) {
                times = Integer.parseInt(expression.get(0).substring(1));
            }
        }
        
        return times;
    }

    protected boolean isValidTime(JobEntity timerEntity, Date newTimerDate, VariableScope variableScope) {
        BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(
                getBusinessCalendarName(TimerEventHandler.geCalendarNameFromConfiguration(timerEntity.getJobHandlerConfiguration()), variableScope));
        return businessCalendar.validateDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations(), timerEntity.getEndDate(), newTimerDate);
    }

    protected String getBusinessCalendarName(String calendarName, VariableScope variableScope) {
        String businessCalendarName = CycleBusinessCalendar.NAME;
        if (StringUtils.isNotEmpty(calendarName)) {
            businessCalendarName = (String) CommandContextUtil.getProcessEngineConfiguration().getExpressionManager()
                    .createExpression(calendarName).getValue(variableScope);
        }
        return businessCalendarName;
    }

    protected void hintAsyncExecutor(JobEntity job) {
        if (Context.getTransactionContext() != null) {
            JobAddedTransactionListener jobAddedTransactionListener = new JobAddedTransactionListener(job, getAsyncExecutor());
            Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, jobAddedTransactionListener);
        } else {
            AsyncJobAddedNotification jobAddedNotification = new AsyncJobAddedNotification(job, getAsyncExecutor());
            getCommandContext().addCloseListener(jobAddedNotification);
        }
    }
    
    @Override
    public HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity) {
        processEngineConfiguration.getHistoryJobEntityManager().insert(historyJobEntity);
        return historyJobEntity;
    }
    
    protected JobEntity internalCreateAsyncJob(ExecutionEntity execution, boolean exclusive) {
        JobEntity asyncJob = processEngineConfiguration.getJobEntityManager().create();
        fillDefaultAsyncJobInfo(asyncJob, execution, exclusive);
        return asyncJob;
    }

    protected JobEntity internalCreateLockedAsyncJob(ExecutionEntity execution, boolean exclusive) {
        JobEntity asyncJob = processEngineConfiguration.getJobEntityManager().create();
        fillDefaultAsyncJobInfo(asyncJob, execution, exclusive);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(processEngineConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, getAsyncExecutor().getAsyncJobLockTimeInMillis());
        asyncJob.setLockExpirationTime(gregorianCalendar.getTime());
        asyncJob.setLockOwner(getAsyncExecutor().getLockOwner());

        return asyncJob;
    }

    protected void fillDefaultAsyncJobInfo(JobEntity jobEntity, ExecutionEntity execution, boolean exclusive) {
        jobEntity.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        jobEntity.setRevision(1);
        jobEntity.setRetries(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        jobEntity.setExecutionId(execution.getId());
        jobEntity.setProcessInstanceId(execution.getProcessInstanceId());
        jobEntity.setProcessDefinitionId(execution.getProcessDefinitionId());
        jobEntity.setExclusive(exclusive);
        jobEntity.setJobHandlerType(AsyncContinuationJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            jobEntity.setTenantId(execution.getTenantId());
        }
    }

    protected JobEntity createExecutableJobFromOtherJob(AbstractRuntimeJobEntity job) {
        JobEntity executableJob = processEngineConfiguration.getJobEntityManager().create();
        copyJobInfo(executableJob, job);

        if (isAsyncExecutorActive()) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(processEngineConfiguration.getClock().getCurrentTime());
            gregorianCalendar.add(Calendar.MILLISECOND, getAsyncExecutor().getTimerLockTimeInMillis());
            executableJob.setLockExpirationTime(gregorianCalendar.getTime());
            executableJob.setLockOwner(getAsyncExecutor().getLockOwner());
        }

        return executableJob;
    }

    protected TimerJobEntity createTimerJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        TimerJobEntity timerJob = processEngineConfiguration.getTimerJobEntityManager().create();
        copyJobInfo(timerJob, otherJob);
        return timerJob;
    }

    protected SuspendedJobEntity createSuspendedJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        SuspendedJobEntity suspendedJob = processEngineConfiguration.getSuspendedJobEntityManager().create();
        copyJobInfo(suspendedJob, otherJob);
        return suspendedJob;
    }

    protected DeadLetterJobEntity createDeadLetterJobFromOtherJob(AbstractRuntimeJobEntity otherJob) {
        DeadLetterJobEntity deadLetterJob = processEngineConfiguration.getDeadLetterJobEntityManager().create();
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
        copyToJob.setJobHandlerType(copyFromJob.getJobHandlerType());
        copyToJob.setJobType(copyFromJob.getJobType());
        copyToJob.setExceptionMessage(copyFromJob.getExceptionMessage());
        copyToJob.setExceptionStacktrace(copyFromJob.getExceptionStacktrace());
        copyToJob.setMaxIterations(copyFromJob.getMaxIterations());
        copyToJob.setProcessDefinitionId(copyFromJob.getProcessDefinitionId());
        copyToJob.setProcessInstanceId(copyFromJob.getProcessInstanceId());
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
            ByteArrayRef configurationByteArrayRefCopy = copyFromJob.getAdvancedJobHandlerConfigurationByteArrayRef().copy();
            copyToJob.setAdvancedJobHandlerConfigurationByteArrayRef(configurationByteArrayRefCopy);
        }
        if (copyFromJob.getExceptionByteArrayRef() != null) {
            ByteArrayRef exceptionByteArrayRefCopy = copyFromJob.getExceptionByteArrayRef();
            copyToJob.setExceptionByteArrayRef(exceptionByteArrayRefCopy);
        }
        copyToJob.setJobHandlerType(copyFromJob.getJobHandlerType());
        copyToJob.setExceptionMessage(copyFromJob.getExceptionMessage());
        copyToJob.setExceptionStacktrace(copyFromJob.getExceptionStacktrace());
        copyToJob.setRetries(copyFromJob.getRetries());
        copyToJob.setRevision(copyFromJob.getRevision());
        copyToJob.setTenantId(copyFromJob.getTenantId());

        return copyToJob;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    protected boolean isAsyncExecutorActive() {
        return processEngineConfiguration.getAsyncExecutor() != null
                && processEngineConfiguration.getAsyncExecutor().isActive();
    }
    
    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected AsyncExecutor getAsyncExecutor() {
        return processEngineConfiguration.getAsyncExecutor();
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return processEngineConfiguration.getExecutionEntityManager();
    }
}
