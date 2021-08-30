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

import java.util.Map;
import java.util.HashMap;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.tenant.DefaultChangeTenantIdResult;
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

public class ChangeTenantIdCmmnSimulateCmd implements Command<ChangeTenantIdResult> {

        private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdCmmnSimulateCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;
        private final boolean onlyInstancesFromDefaultTenantDefinitions;

        public ChangeTenantIdCmmnSimulateCmd(String sourceTenantId, String targetTenantId,
                        boolean onlyInstancesFromDefaultTenantDefinitions) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
                this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
            LOGGER.debug("Simulating case instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
                            onlyInstancesFromDefaultTenantDefinitions
                                            ? " but only for instances from the default tenant definitions"
                                            : "");
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
            String defaultTenantId = cmmnEngineConfiguration.getDefaultTenantProvider()
                            .getDefaultTenant(sourceTenantId, ScopeTypes.CMMN, null);
            JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
            TaskServiceConfiguration taskServiceConfiguration = cmmnEngineConfiguration
                            .getTaskServiceConfiguration();

            Map<String,Long> resultMap = new HashMap<>();
            resultMap.put(CASE_INSTANCES, cmmnEngineConfiguration.getCaseInstanceEntityManager()
                            .countChangeTenantIdCmmnCaseInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(MILESTONE_INSTANCES, cmmnEngineConfiguration
                            .getMilestoneInstanceEntityManager()
                            .countChangeTenantIdCmmnMilestoneInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(PLAN_ITEM_INSTANCES, cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                            .countChangeTenantIdCmmnPlanItemInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(HISTORIC_CASE_INSTANCES, cmmnEngineConfiguration
                            .getHistoricCaseInstanceEntityManager()
                            .countChangeTenantIdCmmnHistoricCaseInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(HISTORIC_MILESTONE_INSTANCES, cmmnEngineConfiguration
                            .getHistoricMilestoneInstanceEntityManager()
                            .countChangeTenantIdCmmnHistoricMilestoneInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(HISTORIC_PLAN_ITEM_INSTANCES, cmmnEngineConfiguration
                            .getHistoricPlanItemInstanceEntityManager()
                            .countChangeTenantIdCmmnHistoricPlanItemInstances(sourceTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions));
            resultMap.put(JOBS, jobServiceConfiguration.getJobEntityManager().countChangeTenantIdJobs(
                            sourceTenantId, defaultTenantId,
                            onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN));
            resultMap.put(TIMER_JOBS, jobServiceConfiguration.getTimerJobEntityManager()
                            .countChangeTenantIdTimerJobs(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(SUSPENDED_JOBS, jobServiceConfiguration.getSuspendedJobEntityManager()
                            .countChangeTenantIdSuspendedJobs(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(DEADLETTER_JOBS, jobServiceConfiguration.getDeadLetterJobEntityManager()
                            .countChangeTenantIdDeadLetterJobs(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(HISTORY_JOBS,
                            jobServiceConfiguration.getHistoryJobEntityManager()
                                            .countChangeTenantIdHistoryJobs(sourceTenantId));
            resultMap.put(EXTERNAL_WORKER_JOBS, jobServiceConfiguration
                            .getExternalWorkerJobEntityManager()
                            .countChangeTenantIdExternalWorkerJobs(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(EVENT_SUBSCRIPTIONS, cmmnEngineConfiguration
                            .getEventSubscriptionServiceConfiguration()
                            .getEventSubscriptionEntityManager()
                            .countChangeTenantIdEventSubscriptions(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(TASKS, taskServiceConfiguration.getTaskEntityManager()
                            .countChangeTenantIdTasks(sourceTenantId, defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(HISTORIC_TASK_INSTANCES, taskServiceConfiguration
                            .getHistoricTaskInstanceEntityManager()
                            .countChangeTenantIdHistoricTaskInstances(sourceTenantId,
                                            defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            resultMap.put(HISTORIC_TASK_LOG_ENTRIES, taskServiceConfiguration
                            .getHistoricTaskLogEntryEntityManager()
                            .countChangeTenantIdHistoricTaskLogEntries(sourceTenantId,
                                            defaultTenantId,
                                            onlyInstancesFromDefaultTenantDefinitions,
                                            ScopeTypes.CMMN));
            return new DefaultChangeTenantIdResult(resultMap);
        }

}
