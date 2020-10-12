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

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

public class HandleHistoryCleanupTimerJobCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Object execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ManagementService managementService = processEngineConfiguration.getManagementService();
        TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
        List<Job> cleanupJobs = managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).list();
        
        if (cleanupJobs.isEmpty()) {
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setRevision(1);
            timerJob.setJobHandlerType(BpmnHistoryCleanupJobHandler.TYPE);
            
            BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
            timerJob.setDuedate(businessCalendar.resolveDuedate(processEngineConfiguration.getHistoryCleaningTimeCycleConfig()));
            timerJob.setRepeat(processEngineConfiguration.getHistoryCleaningTimeCycleConfig());
            
            timerJobService.scheduleTimerJob(timerJob);
            
        } else {
            if (cleanupJobs.size() > 1) {
                for (int i = 1; i < cleanupJobs.size(); i++) {
                    managementService.deleteTimerJob(cleanupJobs.get(i).getId());
                }
            }
        }

        return null;
    }

}
