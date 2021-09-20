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

package org.flowable.engine.impl.tenant;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.ACTIVITY_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.DEADLETTER_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.EXECUTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORY_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.SUSPENDED_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.TASKS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.TIMER_JOBS;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.cmd.ChangeTenantIdCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdRequest;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;

public class ChangeTenantIdBuilderBpmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;
    private final String defaultTenantId;
    private final ProcessEngineConfigurationImpl engineConfiguration;
    private final JobServiceConfiguration jobServiceConfiguration;
    private final TaskServiceConfiguration taskServiceConfiguration;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdBuilderBpmnImpl(CommandExecutor commandExecutor,
            ProcessEngineConfigurationImpl engineConfiguration, String fromTenantId, String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
        this.defaultTenantId = engineConfiguration.getDefaultTenantProvider().getDefaultTenant(fromTenantId,
                ScopeTypes.BPMN, null);
        this.engineConfiguration = engineConfiguration;
        this.jobServiceConfiguration = engineConfiguration.getJobServiceConfiguration();
        this.taskServiceConfiguration = engineConfiguration.getTaskServiceConfiguration();
        this.mapOfEntitiesAndFunctions = buildMapOfEntitiesAndFunctions();
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions() {
        this.onlyInstancesFromDefaultTenantDefinitions = true;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.BPMN).dryRun(true) // This is a dril.
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId).defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.BPMN).dryRun(false) // This is NOT a drill!
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    private Map<String, Function<ChangeTenantIdRequest, Long>> buildMapOfEntitiesAndFunctions() {
        Map<String, Function<ChangeTenantIdRequest, Long>> buildMap = new HashMap<>();
        buildMap.put(EXECUTIONS, r -> engineConfiguration.getExecutionEntityManager().changeTenantIdExecutions(r));
        buildMap.put(ACTIVITY_INSTANCES,
                r -> engineConfiguration.getActivityInstanceEntityManager().changeTenantIdActivityInstances(r));
        buildMap.put(HISTORIC_PROCESS_INSTANCES, r -> engineConfiguration.getHistoricProcessInstanceEntityManager()
                .changeTenantIdHistoricProcessInstances(r));
        buildMap.put(HISTORIC_ACTIVITY_INSTANCES, r -> engineConfiguration.getHistoricActivityInstanceEntityManager()
                .changeTenantIdHistoricActivityInstances(r));
        buildMap.put(JOBS, r -> jobServiceConfiguration.getJobEntityManager().changeTenantIdJobs(r));
        buildMap.put(TIMER_JOBS, r -> jobServiceConfiguration.getTimerJobEntityManager().changeTenantIdTimerJobs(r));
        buildMap.put(SUSPENDED_JOBS,
                r -> jobServiceConfiguration.getSuspendedJobEntityManager().changeTenantIdSuspendedJobs(r));
        buildMap.put(DEADLETTER_JOBS,
                r -> jobServiceConfiguration.getDeadLetterJobEntityManager().changeTenantIdDeadLetterJobs(r));
        buildMap.put(HISTORY_JOBS,
                r -> jobServiceConfiguration.getHistoryJobEntityManager().changeTenantIdHistoryJobs(r));
        buildMap.put(EXTERNAL_WORKER_JOBS,
                r -> jobServiceConfiguration.getExternalWorkerJobEntityManager().changeTenantIdExternalWorkerJobs(r));
        buildMap.put(EVENT_SUBSCRIPTIONS, r -> engineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionEntityManager().changeTenantIdEventSubscriptions(r));
        buildMap.put(TASKS, r -> taskServiceConfiguration.getTaskEntityManager().changeTenantIdTasks(r));
        buildMap.put(HISTORIC_TASK_INSTANCES, r -> taskServiceConfiguration.getHistoricTaskInstanceEntityManager()
                .changeTenantIdHistoricTaskInstances(r));
        buildMap.put(HISTORIC_TASK_LOG_ENTRIES, r -> taskServiceConfiguration.getHistoricTaskLogEntryEntityManager()
                .changeTenantIdHistoricTaskLogEntries(r));
        return buildMap;
    }

}
