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

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChangeTenantIdBpmnCompleteCmd  implements Command<ChangeTenantIdResult>{

    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdBpmnCompleteCmd.class);

    private final String sourceTenantId;
    private final String targetTenantId;
    private final boolean onlyInstancesFromDefaultTenantDefinitions;

    public ChangeTenantIdBpmnCompleteCmd(String sourceTenantId, String targetTenantId,
                    boolean onlyInstancesFromDefaultTenantDefinitions) {
            this.sourceTenantId = sourceTenantId;
            this.targetTenantId = targetTenantId;
            this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
    }

    @Override
    public ChangeTenantIdResult execute(CommandContext commandContext) {
        LOGGER.debug("Executing case instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
        onlyInstancesFromDefaultTenantDefinitions
                        ? " but only for instances from the default tenant definitions"
                        : "");
        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                        String defaultTenantId = engineConfiguration.getDefaultTenantProvider()
                .getDefaultTenant(sourceTenantId, ScopeTypes.BPMN, null);
        JobServiceConfiguration jobServiceConfiguration = engineConfiguration.getJobServiceConfiguration();
        TaskServiceConfiguration taskServiceConfiguration = engineConfiguration.getTaskServiceConfiguration();

        return ChangeTenantIdResult.builder()
                .addResult(Key.Executions, engineConfiguration.getExecutionEntityManager()
                                .changeTenantIdExecutions(sourceTenantId, targetTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions))
                .addResult(Key.ActivityInstances, engineConfiguration.getActivityInstanceEntityManager()
                                .changeTenantIdActivityInstances(sourceTenantId, targetTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions))
                .addResult(Key.HistoricProcessInstances, engineConfiguration.getHistoricProcessInstanceEntityManager()
                                .changeTenantIdHistoricProcessInstances(sourceTenantId, targetTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions))
                .addResult(Key.HistoricActivityInstances, engineConfiguration.getHistoricActivityInstanceEntityManager()
                                .changeTenantIdHistoricActivityInstances(sourceTenantId, targetTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions))
                .addResult(Key.Jobs, jobServiceConfiguration.getJobEntityManager()
                                .changeTenantIdJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.BPMN))
                .addResult(Key.TimerJobs, jobServiceConfiguration.getTimerJobEntityManager()
                                .changeTenantIdTimerJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.SuspendedJobs, jobServiceConfiguration.getSuspendedJobEntityManager()
                                .changeTenantIdSuspendedJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.DeadLetterJobs, jobServiceConfiguration.getDeadLetterJobEntityManager()
                                .changeTenantIdDeadLetterJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.HistoryJobs,
                                jobServiceConfiguration.getHistoryJobEntityManager()
                                                .changeTenantIdHistoryJobs(sourceTenantId, targetTenantId))
                .addResult(Key.ExternalWorkerJobs, jobServiceConfiguration
                                .getExternalWorkerJobEntityManager()
                                .changeTenantIdExternalWorkerJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.EventSubscriptions, engineConfiguration
                                .getEventSubscriptionServiceConfiguration()
                                .getEventSubscriptionEntityManager()
                                .changeTenantIdEventSubscriptions(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.Tasks, taskServiceConfiguration.getTaskEntityManager()
                                .changeTenantIdTasks(sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.HistoricTaskInstances, taskServiceConfiguration
                                .getHistoricTaskInstanceEntityManager()
                                .changeTenantIdHistoricTaskInstances(sourceTenantId, targetTenantId,
                                                defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .addResult(Key.HistoricTaskLogEntries, taskServiceConfiguration
                                .getHistoricTaskLogEntryEntityManager()
                                .changeTenantIdHistoricTaskLogEntries(sourceTenantId, targetTenantId,
                                                defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions,
                                                ScopeTypes.BPMN))
                .build();
    }

}