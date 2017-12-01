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

import java.text.ParseException;
import java.util.Date;

import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.job.TriggerTimerEventJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.calendar.BusinessCalendarManager;
import org.flowable.engine.common.impl.calendar.CronExpression;
import org.flowable.engine.common.impl.calendar.CycleBusinessCalendar;
import org.flowable.engine.common.impl.calendar.DueDateBusinessCalendar;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.type.VariableScopeType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN Timer Event Listener.
 * 
 * @author Joram Barrez
 */
public class TimerEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior {
    
    protected String timerExpression;
    protected String startTriggerSourceRef;
    protected String startTriggerStandardEvent;
    
    public TimerEventListenerActivityBehaviour(TimerEventListener timerEventListener) {
        this.timerExpression = timerEventListener.getTimerExpression();
        this.startTriggerSourceRef = timerEventListener.getTimerStartTriggerSourceRef();
        this.startTriggerStandardEvent = timerEventListener.getTimerStartTriggerStandardEvent();
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        Object timerValue = resolveTimerExpression(commandContext, planItemInstanceEntity);
        Date timerDueDate = resolveTimerDueDate(commandContext, timerValue);
        
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
            timer.setScopeType(VariableScopeType.CMMN);
            timer.setTenantId(planItemInstanceEntity.getTenantId());
            
            if (timerValue instanceof String && isRepetitionString((String) timerValue)) {
                timer.setRepeat(prepareRepeat((String) timerValue, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock()));
            }
            
            jobServiceConfiguration.getTimerJobService().scheduleTimerJob(timer);
        }
        
    }

    protected Object resolveTimerExpression(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);
        Expression expression = expressionManager.createExpression(timerExpression);
        return expression.getValue(planItemInstanceEntity);
    }
    
    protected Date resolveTimerDueDate(CommandContext commandContext, Object timerValue) {
        Date timerDueDate = null;
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
                    
                } else {
                   
                    // Try to parse as ISO8601 first
                    try {
                        timerDueDate = DateTime.parse(timerString).toDate();
                    } catch (Exception e) { }
                    
                    // Try to parse as cron expression
                    try {
                        Clock clock = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock();
                        CronExpression cronExpression = new CronExpression(timerString, clock);
                        if (cronExpression != null) {
                            timerDueDate = cronExpression.getTimeAfter(clock.getCurrentTime());
                        }
                    } catch (ParseException pe) { }
                    
                }
                
            }
        }
        
        if (timerDueDate == null) {
            throw new FlowableException("Timer expression '" + timerExpression + "' did not resolve to java.util.Date, org.joda.time.DateTime, "
                    + "an ISO8601 date/duration/repetition string or a cron expression");
        }
        return timerDueDate;
    }

    protected boolean isRepetitionString(String timerString) {
        return timerString != null && timerString.startsWith("R");
    }

    protected boolean isDurationString(String timerString) {
        return timerString != null && timerString.startsWith("P");
    }
    
    public String prepareRepeat(String dueDate, Clock clock) {
        if (isRepetitionString(dueDate) && dueDate.split("/").length == 2) {
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            return dueDate.replace("/", "/" + fmt.print(new DateTime(clock.getCurrentTime(),DateTimeZone.forTimeZone(clock.getCurrentTimeZone()))) + "/");
        }
        return dueDate;
    }

    
    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstance(planItemInstanceEntity);
    }

}
