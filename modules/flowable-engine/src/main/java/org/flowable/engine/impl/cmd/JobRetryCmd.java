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
package org.flowable.engine.impl.cmd;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.calendar.DurationHelper;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class JobRetryCmd implements Command<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRetryCmd.class.getName());

    protected String jobId;
    protected Throwable exception;

    public JobRetryCmd(String jobId, Throwable exception) {
        this.jobId = jobId;
        this.exception = exception;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        TimerJobService timerJobService = CommandContextUtil.getTimerJobService(commandContext);
        
        JobEntity job = jobService.findJobById(jobId);
        if (job == null) {
            return null;
        }

        ProcessEngineConfiguration processEngineConfig = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        ExecutionEntity executionEntity = fetchExecutionEntity(commandContext, job.getExecutionId());
        FlowElement currentFlowElement = executionEntity != null ? executionEntity.getCurrentFlowElement() : null;
        if (executionEntity != null) {
            executionEntity.setActive(false);
        }

        String failedJobRetryTimeCycleValue = null;
        if (currentFlowElement instanceof ServiceTask) {
            failedJobRetryTimeCycleValue = ((ServiceTask) currentFlowElement).getFailedJobRetryTimeCycleValue();
        }

        AbstractRuntimeJobEntity newJobEntity = null;
        if (currentFlowElement == null || failedJobRetryTimeCycleValue == null) {

            LOGGER.debug("activity or FailedJobRetryTimerCycleValue is null in job {}. Only decrementing retries.", jobId);

            if (job.getRetries() <= 1) {
                newJobEntity = jobService.moveJobToDeadLetterJob(job);
            } else {
                newJobEntity = timerJobService.moveJobToTimerJob(job);
            }

            newJobEntity.setRetries(job.getRetries() - 1);
            if (job.getDuedate() == null || JobEntity.JOB_TYPE_MESSAGE.equals(job.getJobType())) {
                // add wait time for failed async job
                newJobEntity.setDuedate(calculateDueDate(commandContext, processEngineConfig.getAsyncFailedJobWaitTime(), null));
            } else {
                // add default wait time for failed job
                newJobEntity.setDuedate(calculateDueDate(commandContext, processEngineConfig.getDefaultFailedJobWaitTime(), job.getDuedate()));
            }

        } else {
            try {
                DurationHelper durationHelper = new DurationHelper(failedJobRetryTimeCycleValue, processEngineConfig.getClock());
                int jobRetries = job.getRetries();
                if (job.getExceptionMessage() == null) {
                    // change default retries to the ones configured
                    jobRetries = durationHelper.getTimes();
                }

                if (jobRetries <= 1) {
                    newJobEntity = jobService.moveJobToDeadLetterJob(job);
                } else {
                    newJobEntity = timerJobService.moveJobToTimerJob(job);
                }

                newJobEntity.setDuedate(durationHelper.getDateAfter());

                if (job.getExceptionMessage() == null) { // is it the first exception
                    LOGGER.debug("Applying JobRetryStrategy '{}' the first time for job {} with {} retries", failedJobRetryTimeCycleValue, job.getId(), durationHelper.getTimes());

                } else {
                    LOGGER.debug("Decrementing retries of JobRetryStrategy '{}' for job {}", failedJobRetryTimeCycleValue, job.getId());
                }

                newJobEntity.setRetries(jobRetries - 1);

            } catch (Exception e) {
                throw new FlowableException("failedJobRetryTimeCycle has wrong format:" + failedJobRetryTimeCycleValue, exception);
            }
        }

        if (exception != null) {
            newJobEntity.setExceptionMessage(exception.getMessage());
            newJobEntity.setExceptionStacktrace(getExceptionStacktrace());
        }

        // Dispatch both an update and a retry-decrement event
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, newJobEntity));
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_RETRIES_DECREMENTED, newJobEntity));
        }

        return null;
    }

    protected Date calculateDueDate(CommandContext commandContext, int waitTimeInSeconds, Date oldDate) {
        Calendar newDateCal = new GregorianCalendar();
        if (oldDate != null) {
            newDateCal.setTime(oldDate);

        } else {
            newDateCal.setTime(CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime());
        }

        newDateCal.add(Calendar.SECOND, waitTimeInSeconds);
        return newDateCal.getTime();
    }

    protected String getExceptionStacktrace() {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    protected ExecutionEntity fetchExecutionEntity(CommandContext commandContext, String executionId) {
        if (executionId == null) {
            return null;
        }
        return CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
    }

}
