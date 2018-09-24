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
package org.flowable.engine.impl.bpmn.deployer;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.cmd.CancelJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Manages timers for newly-deployed process definitions and their previous versions.
 */
public class TimerManager {

    protected void removeObsoleteTimers(ProcessDefinitionEntity processDefinition) {
        List<TimerJobEntity> jobsToDelete = null;

        if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
            jobsToDelete = CommandContextUtil.getTimerJobService().findJobsByTypeAndProcessDefinitionKeyAndTenantId(
                    TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());
        } else {
            jobsToDelete = CommandContextUtil.getTimerJobService()
                    .findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());
        }

        if (jobsToDelete != null) {
            for (TimerJobEntity job : jobsToDelete) {
                new CancelJobsCmd(job.getId()).execute(Context.getCommandContext());
            }
        }
    }

    protected void scheduleTimers(ProcessDefinitionEntity processDefinition, Process process) {
        TimerJobService timerJobService = CommandContextUtil.getTimerJobService();
        List<TimerJobEntity> timers = getTimerDeclarations(processDefinition, process);
        for (TimerJobEntity timer : timers) {
            timerJobService.scheduleTimerJob(timer);
        }
    }

    protected List<TimerJobEntity> getTimerDeclarations(ProcessDefinitionEntity processDefinition, Process process) {
        List<TimerJobEntity> timers = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(process.getFlowElements())) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) element;
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
                        EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                        if (eventDefinition instanceof TimerEventDefinition) {
                            TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
                            TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, false, null, TimerStartEventJobHandler.TYPE,
                                    TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));

                            if (timerJob != null) {
                                timerJob.setProcessDefinitionId(processDefinition.getId());

                                if (processDefinition.getTenantId() != null) {
                                    timerJob.setTenantId(processDefinition.getTenantId());
                                }
                                timers.add(timerJob);
                            }

                        }
                    }
                }
            }
        }

        return timers;
    }
}
