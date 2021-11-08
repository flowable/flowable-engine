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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Tijs Rademakers
 */
public class HandleHistoryCleanupTimerJobCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    
    @Override
    public Object execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CmmnManagementService managementService = cmmnEngineConfiguration.getCmmnManagementService();
        List<Job> cleanupJobs = managementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).list();
        
        if (cleanupJobs.isEmpty()) {
            scheduleTimerJob(cmmnEngineConfiguration);
            
        } else if (cleanupJobs.size() == 1) {
            TimerJobEntity timerJob = (TimerJobEntity) cleanupJobs.get(0);
            if (!Objects.equals(cmmnEngineConfiguration.getHistoryCleaningTimeCycleConfig(), timerJob.getRepeat())) {
                managementService.deleteTimerJob(timerJob.getId());
                scheduleTimerJob(cmmnEngineConfiguration);
            }
        } else {
            TimerJobEntity timerJob = (TimerJobEntity) cleanupJobs.get(0);
            if (!Objects.equals(cmmnEngineConfiguration.getHistoryCleaningTimeCycleConfig(), timerJob.getRepeat())) {
                // If the cleaning time cycle config has changed we need to create a new timer job
                managementService.deleteTimerJob(timerJob.getId());
                scheduleTimerJob(cmmnEngineConfiguration);
            }

            for (int i = 1; i < cleanupJobs.size(); i++) {
                managementService.deleteTimerJob(cleanupJobs.get(i).getId());
            }
        }

        return null;
    }

    protected void scheduleTimerJob(CmmnEngineConfiguration cmmnEngineConfiguration) {
        TimerJobService timerJobService = cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
        TimerJobEntity timerJob = timerJobService.createTimerJob();
        timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
        timerJob.setRevision(1);
        timerJob.setJobHandlerType(CmmnHistoryCleanupJobHandler.TYPE);
        timerJob.setScopeType(ScopeTypes.CMMN);

        BusinessCalendar businessCalendar = cmmnEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
        timerJob.setDuedate(businessCalendar.resolveDuedate(cmmnEngineConfiguration.getHistoryCleaningTimeCycleConfig()));
        timerJob.setRepeat(cmmnEngineConfiguration.getHistoryCleaningTimeCycleConfig());

        timerJobService.scheduleTimerJob(timerJob);
    }

}
