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

import java.util.Date;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.job.TriggerTimerEventJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
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
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN Timer Event Listener.
 * 
 * @author Joram Barrez
 */
public class TimerEventListenerActivityBehaviour extends CoreCmmnActivityBehavior implements PlanItemActivityBehavior {
    
    protected String timerExpression;
    protected String startTriggerSourceRef;
    protected String startTriggerStandardEvent;
    
    public TimerEventListenerActivityBehaviour(TimerEventListener timerEventListener) {
        this.timerExpression = timerEventListener.getTimerExpression();
        this.startTriggerSourceRef = timerEventListener.getTimerStartTriggerSourceRef();
        this.startTriggerStandardEvent = timerEventListener.getTimerStartTriggerStandardEvent();
    }
    
    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.CREATE.equals(transition)) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) planItemInstance;
            Object timerValue = resolveTimerExpression(commandContext, planItemInstanceEntity);
            
            Date timerDueDate = null;
            boolean isRepeating = false;
            if (timerValue != null) {
                if (timerValue instanceof Date) {
                    timerDueDate = (Date) timerValue;
                    
                } else if (timerValue instanceof DateTime) {
                    DateTime timerDateTime = (DateTime) timerValue;
                    timerDueDate = timerDateTime.toDate();
                    
                } else if (timerValue instanceof String) {
                    String timerString = (String) timerValue;
                    
                    BusinessCalendarManager businessCalendarManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getBusinessCalendarManager();
                    if (isDurationString(timerString)) {
                        timerDueDate = businessCalendarManager.getBusinessCalendar(DueDateBusinessCalendar.NAME).resolveDuedate(timerString);
                        
                    } else if (isRepetitionString(timerString)) {
                        timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(timerString);
                        isRepeating = true;
                        
                    } else {
                       
                        // Try to parse as ISO8601 first
                        try {
                            timerDueDate = DateTime.parse(timerString).toDate();
                        } catch (Exception e) { }
                        
                        // Try to parse as cron expression
                        try {
                            timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(timerString);
                            isRepeating = true;
                            
                        } catch (Exception pe) { }
                        
                    }
                    
                }
            }
            
            if (timerDueDate == null) {
                throw new FlowableException("Timer expression '" + timerExpression + "' did not resolve to java.util.Date, org.joda.time.DateTime, "
                        + "an ISO8601 date/duration/repetition string or a cron expression");
            }
            
            scheduleTimerJob(commandContext, planItemInstanceEntity, timerValue, timerDueDate, isRepeating);
        }
    }

    protected void scheduleTimerJob(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, 
            Object timerValue, Date timerDueDate, boolean isRepeating) {
        if (timerDueDate != null) {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration();
            TimerJobEntity timer = jobServiceConfiguration.getTimerJobService().createTimerJob();
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            timer.setJobHandlerType(TriggerTimerEventJobHandler.TYPE);
            timer.setExclusive(true);
            timer.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());
            timer.setDuedate(timerDueDate);
            timer.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            timer.setScopeId(planItemInstanceEntity.getCaseInstanceId());
            timer.setSubScopeId(planItemInstanceEntity.getId());
            timer.setScopeType(ScopeTypes.CMMN);
            timer.setTenantId(planItemInstanceEntity.getTenantId());
            
            if (isRepeating && timerValue instanceof String) {
                timer.setRepeat(prepareRepeat((String) timerValue, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock()));
            }
            
            jobServiceConfiguration.getTimerJobService().scheduleTimerJob(timer);
        }
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
    }

    protected Object resolveTimerExpression(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);
        Expression expression = expressionManager.createExpression(timerExpression);
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
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            return dueDate.replace("/", "/" + fmt.print(new DateTime(clock.getCurrentTime(),DateTimeZone.forTimeZone(clock.getCurrentTimeZone()))) + "/");
        }
        return dueDate;
    }
    
    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        execute(planItemInstance);
    }
    
}
