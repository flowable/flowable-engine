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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.job.TriggerTimerEventJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.joda.JodaDeprecationLogger;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DateUtil;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.joda.time.DateTime;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN Timer Event Listener.
 * 
 * @author Joram Barrez
 */
public class TimerEventListenerActivityBehaviour extends CoreCmmnActivityBehavior implements PlanItemActivityBehavior {

    protected TimerEventListener timerEventListener;

    public TimerEventListenerActivityBehaviour(TimerEventListener timerEventListener) {
        this.timerEventListener = timerEventListener;
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if ((PlanItemTransition.CREATE.equals(transition) && StringUtils.isEmpty(timerEventListener.getAvailableConditionExpression()))
                || PlanItemTransition.INITIATE.equals(transition)) {
            
            handleCreateTransition(commandContext, (PlanItemInstanceEntity) planItemInstance);

        } else if (PlanItemTransition.DISMISS.equals(transition)
                || PlanItemTransition.TERMINATE.equals(transition)
                || PlanItemTransition.EXIT.equals(transition)) {
            
            removeTimerJob(commandContext, (PlanItemInstanceEntity) planItemInstance);
        }
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
    }
    
    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        execute(planItemInstance);
    }

    protected void handleCreateTransition(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {

        if (timerJobForPlanItemInstanceExists(commandContext, planItemInstance)) {
            return;
        }

        Object timerValue = resolveTimerExpression(commandContext, planItemInstance);

        Date timerDueDate = null;
        boolean isRepeating = false;
        if (timerValue != null) {
            if (timerValue instanceof Date) {
                timerDueDate = (Date) timerValue;

            } else if (timerValue instanceof DateTime timerDateTime) {
                JodaDeprecationLogger.LOGGER.warn(
                        "Using Joda-Time DateTime has been deprecated and will be removed in a future version. Timer event listener expression {} in {} resolved to a Joda-Time DateTime. ",
                        timerEventListener.getTimerExpression(), planItemInstance);
                timerDueDate = timerDateTime.toDate();

            } else if (timerValue instanceof String timerString) {

                BusinessCalendarManager businessCalendarManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getBusinessCalendarManager();
                if (isDurationString(timerString)) {
                    timerDueDate = businessCalendarManager.getBusinessCalendar(DueDateBusinessCalendar.NAME).resolveDuedate(timerString);

                } else if (isRepetitionString(timerString)) {
                    timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(timerString);
                    isRepeating = true;

                } else {

                    // Try to parse as ISO8601 first
                    try {
                        timerDueDate = DateUtil.parseDate(timerString);
                    } catch (Exception e) { }

                    // Try to parse as cron expression
                    try {
                        timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(timerString);
                        isRepeating = true;

                    } catch (Exception pe) { }

                }

            } else if (timerValue instanceof Instant) {
                timerDueDate = Date.from((Instant) timerValue);

            } else if (timerValue instanceof LocalDate) {
                timerDueDate = Date.from(((LocalDate) timerValue).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            } else if (timerValue instanceof LocalDateTime) {
                timerDueDate = Date.from(((LocalDateTime) timerValue).atZone(ZoneId.systemDefault()).toInstant());

            }
        }

        if (timerDueDate == null) {
            throw new FlowableException("Timer expression '" + timerEventListener.getTimerExpression() + "' did not resolve to java.util.Date, org.joda.time.DateTime, "
                    + "java.time.Instant, java.time.LocalDate, java.time.LocalDateTime or "
                    + "an ISO8601 date/duration/repetition string or a cron expression for " + planItemInstance);
        }

        scheduleTimerJob(commandContext, planItemInstance, timerValue, timerDueDate, isRepeating);
    }

    protected boolean timerJobForPlanItemInstanceExists(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {

        // For the same plan item, only one timer job can ever be active at any given time.
        // Since the DefaultJobManager creates a new timer job on repeat, we need to make sure
        // we're not creating duplicate timers on the create or initiate transition (which does need to happen on the first repeat).
        //
        // The alternative implementation would be to move the repeating timer creation to the onStateTransition on occur,
        // but this would also require similar logic to look up the previous timer job, as the previous repeat value is needed to calculate the next.

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        List<TimerJobEntity> jobsByScopeIdAndSubScopeId = cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager()
                .findJobsByScopeIdAndSubScopeId(planItemInstance.getCaseInstanceId(), planItemInstance.getId());
        return jobsByScopeIdAndSubScopeId != null && !jobsByScopeIdAndSubScopeId.isEmpty();
    }

    protected void scheduleTimerJob(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            Object timerValue, Date timerDueDate, boolean isRepeating) {
        
        if (timerDueDate != null) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
            TimerJobEntity timer = jobServiceConfiguration.getTimerJobService().createTimerJob();
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            timer.setJobHandlerType(TriggerTimerEventJobHandler.TYPE);
            timer.setExclusive(true);
            timer.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());
            timer.setDuedate(timerDueDate);
            timer.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            timer.setScopeId(planItemInstanceEntity.getCaseInstanceId());
            timer.setSubScopeId(planItemInstanceEntity.getId());
            
            PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
            List<ExtensionElement> jobCategoryElements = planItemDefinition.getExtensionElements().get("jobCategory");
            if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
                ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
                if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                    Expression categoryExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(jobCategoryElement.getElementText());
                    Object categoryValue = categoryExpression.getValue(planItemInstanceEntity);
                    if (categoryValue != null) {
                        timer.setCategory(categoryValue.toString());
                    }
                }
            }
            
            timer.setScopeType(ScopeTypes.CMMN);
            timer.setElementId(timerEventListener.getId());
            timer.setElementName(timerEventListener.getName());
            timer.setTenantId(planItemInstanceEntity.getTenantId());
            
            if (isRepeating && timerValue instanceof String) {
                timer.setRepeat(prepareRepeat((String) timerValue, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock()));
            }
            
            jobServiceConfiguration.getTimerJobService().scheduleTimerJob(timer);
        }
    }

    protected void removeTimerJob(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        TimerJobEntityManager timerJobEntityManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobEntityManager();
        List<TimerJobEntity> timerJobsEntities = timerJobEntityManager
            .findJobsByScopeIdAndSubScopeId(planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId());
        for (TimerJobEntity timerJobEntity : timerJobsEntities) {
            timerJobEntityManager.delete(timerJobEntity);
        }
    }

    protected Object resolveTimerExpression(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);
        Expression expression = expressionManager.createExpression(timerEventListener.getTimerExpression());
        return expression.getValue(planItemInstanceEntity);
    }
    
    protected boolean isRepetitionString(String timerString) {
        return timerString != null && timerString.startsWith("R");
    }

    protected boolean isDurationString(String timerString) {
        return timerString != null && timerString.startsWith("P");
    }
    
    public String prepareRepeat(String dueDate, Clock clock) {
        if (dueDate.startsWith("R") && dueDate.split("/").length == 2) {
            return dueDate.replace("/", "/" + clock.getCurrentTime().toInstant().toString() + "/");
        }
        return dueDate;
    }
}
