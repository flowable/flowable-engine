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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class RescheduleTimerJobCmd implements Command<Job> {

    protected String eventListenerInstanceId;
    protected String jobId;
    protected Date newDueDate;
    protected String newDateValue;

    public RescheduleTimerJobCmd(String eventListenerInstanceId, String jobId, Date newDueDate, String newDateValue) {
        this.eventListenerInstanceId = eventListenerInstanceId;
        this.jobId = jobId;
        this.newDueDate = newDueDate;
        this.newDateValue = newDateValue;
    }

    @Override
    public Job execute(CommandContext commandContext) {
        String timerJobId = null;
        PlanItemInstance planItemInstance = null;
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        
        if (eventListenerInstanceId != null) {
            planItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(eventListenerInstanceId);
            if (planItemInstance == null) {
                throw new FlowableObjectNotFoundException("No plan item instance found for id " + eventListenerInstanceId);
            }
            
            List<Job> timerJobs = jobService.createTimerJobQuery().planItemInstanceId(eventListenerInstanceId).list();
            if (timerJobs == null || timerJobs.isEmpty()) {
                throw new FlowableException("No timer jobs found for plan item instance " + eventListenerInstanceId);
            }
            
            if (timerJobs.size() > 1) {
                throw new FlowableException("Multiple timer jobs found for plan item instance " + eventListenerInstanceId);
            }
            
            timerJobId = timerJobs.get(0).getId();
        
        } else {
            timerJobId = jobId;
        }
        
        TimerJobService timerJobService = CommandContextUtil.getTimerJobService(commandContext);
        TimerJobEntity timerJob = timerJobService.findTimerJobById(timerJobId);
        if (timerJob == null) {
            throw new FlowableObjectNotFoundException("Timer job not found for id " + timerJobId);
        }
        
        if (planItemInstance == null) {
            planItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(timerJob.getSubScopeId());
            if (planItemInstance == null) {
                throw new FlowableException("Plan item instance not found for id " + timerJob.getSubScopeId());
            }
        }
        
        Date timerDueDate = null;
        boolean isRepeating = false;
        if (newDueDate != null) {
            timerDueDate = newDueDate;
            
        } else if (newDateValue != null) {
            BusinessCalendarManager businessCalendarManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getBusinessCalendarManager();
            if (isDurationString(newDateValue)) {
                timerDueDate = businessCalendarManager.getBusinessCalendar(DueDateBusinessCalendar.NAME).resolveDuedate(newDateValue);

            } else if (isRepetitionString(newDateValue)) {
                timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(newDateValue);
                isRepeating = true;

            } else {

                // Try to parse as ISO8601 first
                try {
                    timerDueDate = DateTime.parse(newDateValue).toDate();
                } catch (Exception e) { }

                // Try to parse as cron expression
                try {
                    timerDueDate = businessCalendarManager.getBusinessCalendar(CycleBusinessCalendar.NAME).resolveDuedate(newDateValue);
                    isRepeating = true;

                } catch (Exception pe) { }
            }
            
            if (timerDueDate == null) {
                throw new FlowableException("Timer expression '" + newDateValue + "' did not resolve to java.util.Date for " + planItemInstance);
            }
        }
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
        
        TimerJobEntity newTimer = timerJobService.createTimerJob();
        newTimer.setJobType(timerJob.getJobType());
        newTimer.setJobHandlerType(timerJob.getJobHandlerType());
        newTimer.setExclusive(true);
        newTimer.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());
        newTimer.setDuedate(timerDueDate);
        newTimer.setScopeDefinitionId(timerJob.getScopeDefinitionId());
        newTimer.setScopeId(timerJob.getScopeId());
        newTimer.setSubScopeId(timerJob.getSubScopeId());
        newTimer.setCategory(timerJob.getCategory());
        newTimer.setScopeType(timerJob.getScopeType());
        newTimer.setElementId(timerJob.getElementId());
        newTimer.setElementName(timerJob.getElementName());
        newTimer.setTenantId(timerJob.getTenantId());
        
        if (isRepeating) {
            newTimer.setRepeat(prepareRepeat((String) newDateValue, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock()));
        }
        
        timerJobService.deleteTimerJob(timerJob);
        timerJobService.scheduleTimerJob(newTimer);

        return newTimer;
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
}
