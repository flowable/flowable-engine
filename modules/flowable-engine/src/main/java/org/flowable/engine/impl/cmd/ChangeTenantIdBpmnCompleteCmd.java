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

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdResult;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.ACTIVITY_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.EXECUTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.CASE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.PLAN_ITEM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.MILESTONE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.EVENT_SUBSCRIPTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.TASKS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.FORM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.EXTERNAL_WORKER_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.CONTENT_ITEM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_ACTIVITY_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_CASE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_DECISION_EXECUTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_MILESTONE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_PLAN_ITEM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_PROCESS_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_TASK_LOG_ENTRIES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORIC_TASK_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.HISTORY_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.SUSPENDED_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.TIMER_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.DEADLETTER_JOBS;

public class ChangeTenantIdBpmnCompleteCmd  implements Command<ChangeTenantIdResult>{

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdBpmnCompleteCmd.class);

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

        Map<String,Long> resultMap = new HashMap<>();
        resultMap.put(EXECUTIONS, engineConfiguration.getExecutionEntityManager()
                        .changeTenantIdExecutions(sourceTenantId, targetTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions));
        resultMap.put(ACTIVITY_INSTANCES, engineConfiguration.getActivityInstanceEntityManager()
                        .changeTenantIdActivityInstances(sourceTenantId, targetTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions));
        resultMap.put(HISTORIC_PROCESS_INSTANCES, engineConfiguration.getHistoricProcessInstanceEntityManager()
                        .changeTenantIdHistoricProcessInstances(sourceTenantId, targetTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions));
        resultMap.put(HISTORIC_ACTIVITY_INSTANCES, engineConfiguration.getHistoricActivityInstanceEntityManager()
                        .changeTenantIdHistoricActivityInstances(sourceTenantId, targetTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions));
        resultMap.put(JOBS, jobServiceConfiguration.getJobEntityManager()
                        .changeTenantIdJobs(sourceTenantId, targetTenantId, defaultTenantId,
                        onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.BPMN));
        resultMap.put(TIMER_JOBS, jobServiceConfiguration.getTimerJobEntityManager()
                        .changeTenantIdTimerJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(SUSPENDED_JOBS, jobServiceConfiguration.getSuspendedJobEntityManager()
                        .changeTenantIdSuspendedJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(DEADLETTER_JOBS, jobServiceConfiguration.getDeadLetterJobEntityManager()
                        .changeTenantIdDeadLetterJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(HISTORY_JOBS,
                        jobServiceConfiguration.getHistoryJobEntityManager()
                                        .changeTenantIdHistoryJobs(sourceTenantId, targetTenantId));
        resultMap.put(EXTERNAL_WORKER_JOBS, jobServiceConfiguration
                        .getExternalWorkerJobEntityManager()
                        .changeTenantIdExternalWorkerJobs(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(EVENT_SUBSCRIPTIONS, engineConfiguration
                        .getEventSubscriptionServiceConfiguration()
                        .getEventSubscriptionEntityManager()
                        .changeTenantIdEventSubscriptions(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(TASKS, taskServiceConfiguration.getTaskEntityManager()
                        .changeTenantIdTasks(sourceTenantId, targetTenantId, defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(HISTORIC_TASK_INSTANCES, taskServiceConfiguration
                        .getHistoricTaskInstanceEntityManager()
                        .changeTenantIdHistoricTaskInstances(sourceTenantId, targetTenantId,
                                        defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        resultMap.put(HISTORIC_TASK_LOG_ENTRIES, taskServiceConfiguration
                        .getHistoricTaskLogEntryEntityManager()
                        .changeTenantIdHistoricTaskLogEntries(sourceTenantId, targetTenantId,
                                        defaultTenantId,
                                        onlyInstancesFromDefaultTenantDefinitions,
                                        ScopeTypes.BPMN));
        return new DefaultChangeTenantIdResult(resultMap);
    }

}