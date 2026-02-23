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
package org.flowable.engine.impl.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.DefinitionVariableContainer;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DurationBusinessCalendar;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.joda.JodaDeprecationLogger;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.joda.time.DateTime;

/**
 * @author Joram Barrez
 */
public class TimerUtil {

    /**
     * The event definition on which the timer is based.
     * <p>
     * Takes in a {@link ProcessDefinition} for expression evaluation at deployment time (eg Timer start event).
     */
    public static TimerJobEntity createTimerEntityForTimerEventDefinition(TimerEventDefinition timerEventDefinition,
            FlowElement currentFlowElement, boolean isInterruptingTimer, ProcessDefinition processDefinition,
            String jobHandlerType, String jobHandlerConfig) {

        DefinitionVariableContainer definitionVariableContainer = new DefinitionVariableContainer(processDefinition.getId(),
                processDefinition.getKey(), processDefinition.getDeploymentId(), ScopeTypes.BPMN, processDefinition.getTenantId());

        TimerJobEntity timer = createTimerEntity(timerEventDefinition, currentFlowElement, isInterruptingTimer,
                definitionVariableContainer, jobHandlerType, jobHandlerConfig);

        if (timer != null) {
            timer.setProcessDefinitionId(processDefinition.getId());

            if (processDefinition.getTenantId() != null) {
                timer.setTenantId(processDefinition.getTenantId());
            }
        }

        return timer;
    }

    /**
     * The event definition on which the timer is based.
     * <p>
     * Takes in an optional execution, if missing the {@link NoExecutionVariableScope} will be used (eg Timer start event)
     */
    public static TimerJobEntity createTimerEntityForTimerEventDefinition(TimerEventDefinition timerEventDefinition,
            FlowElement currentFlowElement, boolean isInterruptingTimer,
            ExecutionEntity executionEntity, String jobHandlerType, String jobHandlerConfig) {

        VariableContainer variableContainer = executionEntity;
        if (variableContainer == null) {
            variableContainer = NoExecutionVariableScope.getSharedInstance();
        }

        TimerJobEntity timer = createTimerEntity(timerEventDefinition, currentFlowElement, isInterruptingTimer,
                variableContainer, jobHandlerType, jobHandlerConfig);

        if (timer != null && executionEntity != null) {
            timer.setExecutionId(executionEntity.getId());
            timer.setProcessDefinitionId(executionEntity.getProcessDefinitionId());
            timer.setProcessInstanceId(executionEntity.getProcessInstanceId());
            timer.setElementId(executionEntity.getCurrentFlowElement().getId());
            timer.setElementName(executionEntity.getCurrentFlowElement().getName());

            if (executionEntity.getTenantId() != null) {
                timer.setTenantId(executionEntity.getTenantId());
            }
        }

        return timer;
    }

    private static TimerJobEntity createTimerEntity(TimerEventDefinition timerEventDefinition,
            FlowElement currentFlowElement, boolean isInterruptingTimer, VariableContainer variableContainer,
            String jobHandlerType, String jobHandlerConfig) {

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();

        String businessCalendarRef = null;
        Expression expression = null;
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (StringUtils.isNotEmpty(timerEventDefinition.getTimeDate())) {

            businessCalendarRef = DueDateBusinessCalendar.NAME;
            expression = expressionManager.createExpression(timerEventDefinition.getTimeDate());

        } else if (StringUtils.isNotEmpty(timerEventDefinition.getTimeCycle())) {

            businessCalendarRef = CycleBusinessCalendar.NAME;
            expression = expressionManager.createExpression(timerEventDefinition.getTimeCycle());

        } else if (StringUtils.isNotEmpty(timerEventDefinition.getTimeDuration())) {

            businessCalendarRef = DurationBusinessCalendar.NAME;
            expression = expressionManager.createExpression(timerEventDefinition.getTimeDuration());
        }

        if (StringUtils.isNotEmpty(timerEventDefinition.getCalendarName())) {
            businessCalendarRef = timerEventDefinition.getCalendarName();
            Expression businessCalendarExpression = expressionManager.createExpression(businessCalendarRef);
            businessCalendarRef = businessCalendarExpression.getValue(variableContainer).toString();
        }

        if (expression == null) {
            throw new FlowableException(
                    "Timer needs configuration (either timeDate, timeCycle or timeDuration is needed) (" + timerEventDefinition.getId() + ")");
        }

        BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(businessCalendarRef);

        String dueDateString = null;
        Date duedate = null;

        Object dueDateValue = expression.getValue(variableContainer);
        if (dueDateValue instanceof String) {
            dueDateString = (String) dueDateValue;

        } else if (dueDateValue instanceof Date) {
            duedate = (Date) dueDateValue;

        } else if (dueDateValue instanceof DateTime) {
            JodaDeprecationLogger.LOGGER.warn(
                    "Using Joda-Time DateTime has been deprecated and will be removed in a future version. Timer event listener expression {} in {} resolved to a Joda-Time DateTime. ",
                    expression.getExpressionText(), variableContainer);
            // JodaTime support
            duedate = ((DateTime) dueDateValue).toDate();

        } else if (dueDateValue instanceof Duration) {
            dueDateString = ((Duration) dueDateValue).toString();

        } else if (dueDateValue instanceof Instant) {
            duedate = Date.from((Instant) dueDateValue);

        } else if (dueDateValue instanceof LocalDate) {
            duedate = Date.from(((LocalDate) dueDateValue).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        } else if (dueDateValue instanceof LocalDateTime) {
            duedate = Date.from(((LocalDateTime) dueDateValue).atZone(ZoneId.systemDefault()).toInstant());

        } else if (dueDateValue != null) {
            throw new FlowableException(
                    "Timer for " + variableContainer + " was not configured with a valid duration/time, either hand in a java.util.Date, java.time.LocalDate, java.time.LocalDateTime or a java.time.Instant or a org.joda.time.DateTime or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
        }

        if (duedate == null && dueDateString != null) {
            duedate = businessCalendar.resolveDuedate(dueDateString);
        }

        TimerJobEntity timer = null;
        if (duedate != null) {

            timer = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService().createTimerJob();
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            timer.setRevision(1);
            timer.setJobHandlerType(jobHandlerType);
            timer.setJobHandlerConfiguration(jobHandlerConfig);
            timer.setExclusive(true);
            timer.setRetries(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
            timer.setDuedate(duedate);

            String jobCategoryElementText = resolveJobCategoryText(currentFlowElement);
            if (jobCategoryElementText != null) {
                Expression categoryExpression = processEngineConfiguration.getExpressionManager().createExpression(jobCategoryElementText);
                Object categoryValue = categoryExpression.getValue(variableContainer);
                if (categoryValue != null) {
                    timer.setCategory(categoryValue.toString());
                }
            }

        } else {
            StringBuilder sb = new StringBuilder("Due date could not be determined for timer job ").append(dueDateString);
            sb.append(" for ").append(variableContainer);
            throw new FlowableException(sb.toString());
        }

        if (StringUtils.isNotEmpty(timerEventDefinition.getTimeCycle())) {
            // See ACT-1427: A boundary timer with a cancelActivity='true', doesn't need to repeat itself
            boolean repeat = !isInterruptingTimer;

            // ACT-1951: intermediate catching timer events shouldn't repeat according to spec
            if (currentFlowElement instanceof IntermediateCatchEvent) {
                repeat = false;
            }

            if (repeat) {
                String prepared = prepareRepeat(dueDateString);
                timer.setRepeat(prepared);
            }
        }



        return timer;
    }

    private static String resolveJobCategoryText(FlowElement flowElement) {
        List<ExtensionElement> jobCategoryElements = flowElement.getExtensionElements().get("jobCategory");
        if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
            ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
            if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                return jobCategoryElement.getElementText();
            }
        }
        return null;
    }

    public static TimerJobEntity rescheduleTimerJob(String timerJobId, TimerEventDefinition timerEventDefinition) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
        TimerJobEntity timerJob = timerJobService.findTimerJobById(timerJobId);
        if (timerJob != null) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(timerJob.getProcessDefinitionId());
            Event eventElement = (Event) bpmnModel.getFlowElement(TimerEventHandler.getActivityIdFromConfiguration(timerJob.getJobHandlerConfiguration()));
            boolean isInterruptingTimer = false;
            if (eventElement instanceof BoundaryEvent) {
                isInterruptingTimer = ((BoundaryEvent) eventElement).isCancelActivity();
            }

            ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().findById(timerJob.getExecutionId());
            TimerJobEntity rescheduledTimerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition,
                    eventElement, isInterruptingTimer, execution,
                    timerJob.getJobHandlerType(), timerJob.getJobHandlerConfiguration());

            timerJobService.deleteTimerJob(timerJob);
            timerJobService.insertTimerJob(rescheduledTimerJob);

            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createJobRescheduledEvent(FlowableEngineEventType.JOB_RESCHEDULED,
                        rescheduledTimerJob, timerJob.getId()), processEngineConfiguration.getEngineCfgKey());

             // job rescheduled event should occur before new timer scheduled event
                eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_SCHEDULED, rescheduledTimerJob),
                                processEngineConfiguration.getEngineCfgKey());
            }

            return rescheduledTimerJob;
        }
        return null;
    }

    public static String prepareRepeat(String dueDate) {
        if (dueDate.startsWith("R") && dueDate.split("/").length == 2) {
            Clock clock = CommandContextUtil.getProcessEngineConfiguration().getClock();
            return dueDate.replace("/", "/" + clock.getCurrentTime().toInstant().toString() + "/");
        }
        return dueDate;
    }

}
