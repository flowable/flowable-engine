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

package org.flowable.cmmn.engine.impl.tenant;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.CASE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.PLAN_ITEM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.MILESTONE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_CASE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_PLAN_ITEM_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.HISTORIC_MILESTONE_INSTANCES;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.DEADLETTER_JOBS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS;
import static org.flowable.common.engine.api.tenant.ChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS;
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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.cmd.ChangeTenantIdCmd;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdRequest;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;

public class ChangeTenantIdBuilderCmmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final CmmnEngineConfiguration cmmnEngineConfiguration;
    private final JobServiceConfiguration jobServiceConfiguration;
    private final TaskServiceConfiguration taskServiceConfiguration;
    private final String defaultTenantId;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;
    private final Map<String, Function<ChangeTenantIdRequest, Long>> mapOfEntitiesAndFunctions;

    public ChangeTenantIdBuilderCmmnImpl(CommandExecutor commandExecutor,
            CmmnEngineConfiguration cmmnEngineConfiguration, String fromTenantId, String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
        this.taskServiceConfiguration = cmmnEngineConfiguration.getTaskServiceConfiguration();
        this.defaultTenantId = cmmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(fromTenantId, ScopeTypes.CMMN, null);
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
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
                .builder(this.fromTenantId, this.toTenantId)
                .defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.CMMN)
                .dryRun(true) // This is a drill.
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        ChangeTenantIdRequest changeTenantIdRequest = DefaultChangeTenantIdRequest
                .builder(this.fromTenantId, this.toTenantId)
                .defaultTenantId(defaultTenantId)
                .onlyInstancesFromDefaultTenantDefinitions(onlyInstancesFromDefaultTenantDefinitions)
                .scope(ScopeTypes.CMMN)
                .dryRun(false) // This is NOT a drill!
                .build();
        return commandExecutor.execute(new ChangeTenantIdCmd(changeTenantIdRequest, mapOfEntitiesAndFunctions));
    }

    private Map<String, Function<ChangeTenantIdRequest, Long>> buildMapOfEntitiesAndFunctions() {
        Map<String, Function<ChangeTenantIdRequest, Long>> buildMap = new HashMap<>();
        buildMap.put(CASE_INSTANCES,
                r -> cmmnEngineConfiguration.getCaseInstanceEntityManager().changeTenantIdCmmnCaseInstances(r));
        buildMap.put(MILESTONE_INSTANCES, r -> cmmnEngineConfiguration.getMilestoneInstanceEntityManager()
                .changeTenantIdCmmnMilestoneInstances(r));
        buildMap.put(PLAN_ITEM_INSTANCES,
                r -> cmmnEngineConfiguration.getPlanItemInstanceEntityManager().changeTenantIdCmmnPlanItemInstances(r));
        buildMap.put(HISTORIC_CASE_INSTANCES, r -> cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager()
                .changeTenantIdCmmnHistoricCaseInstances(r));
        buildMap.put(HISTORIC_MILESTONE_INSTANCES, r -> cmmnEngineConfiguration
                .getHistoricMilestoneInstanceEntityManager().changeTenantIdCmmnHistoricMilestoneInstances(r));
        buildMap.put(HISTORIC_PLAN_ITEM_INSTANCES, r -> cmmnEngineConfiguration
                .getHistoricPlanItemInstanceEntityManager().changeTenantIdCmmnHistoricPlanItemInstances(r));
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
        buildMap.put(EVENT_SUBSCRIPTIONS, r -> cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionEntityManager().changeTenantIdEventSubscriptions(r));
        buildMap.put(TASKS, r -> taskServiceConfiguration.getTaskEntityManager().changeTenantIdTasks(r));
        buildMap.put(HISTORIC_TASK_INSTANCES, r -> taskServiceConfiguration.getHistoricTaskInstanceEntityManager()
                .changeTenantIdHistoricTaskInstances(r));
        buildMap.put(HISTORIC_TASK_LOG_ENTRIES, r -> taskServiceConfiguration.getHistoricTaskLogEntryEntityManager()
                .changeTenantIdHistoricTaskLogEntries(r));
        return buildMap;
    }

}
