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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdCmmnCompleteCmd implements Command<ChangeTenantIdResult> {

        private final static Logger LOGGER = LoggerFactory.getLogger(ChangeTenantIdCmmnCompleteCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;
        private final boolean onlyInstancesFromDefaultTenantDefinitions;

        public ChangeTenantIdCmmnCompleteCmd(String sourceTenantId, String targetTenantId,
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
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
                String defaultTenantId = cmmnEngineConfiguration.getDefaultTenantProvider()
                                .getDefaultTenant(sourceTenantId, ScopeTypes.CMMN, null);
                JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
                TaskServiceConfiguration taskServiceConfiguration = cmmnEngineConfiguration
                                .getTaskServiceConfiguration();
                return ChangeTenantIdResult.builder()
                                .addResult(Key.CaseInstances, cmmnEngineConfiguration.getCaseInstanceEntityManager()
                                                .changeTenantIdCmmnCaseInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.MilestoneInstances, cmmnEngineConfiguration
                                                .getMilestoneInstanceEntityManager()
                                                .changeTenantIdCmmnMilestoneInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.PlanItemInstances, cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                                                .changeTenantIdCmmnPlanItemInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.HistoricCaseInstances, cmmnEngineConfiguration
                                                .getHistoricCaseInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricCaseInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.HistoricMilestoneInstances, cmmnEngineConfiguration
                                                .getHistoricMilestoneInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricMilestoneInstances(sourceTenantId,
                                                                targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.HistoricPlanItemInstances, cmmnEngineConfiguration
                                                .getHistoricPlanItemInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricPlanItemInstances(sourceTenantId,
                                                                targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Key.Jobs, jobServiceConfiguration.getJobEntityManager().changeTenantIdJobs(
                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN))
                                .addResult(Key.TimerJobs, jobServiceConfiguration.getTimerJobEntityManager()
                                                .changeTenantIdTimerJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.SuspendedJobs, jobServiceConfiguration.getSuspendedJobEntityManager()
                                                .changeTenantIdSuspendedJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.DeadLetterJobs, jobServiceConfiguration.getDeadLetterJobEntityManager()
                                                .changeTenantIdDeadLetterJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.HistoryJobs, jobServiceConfiguration.getHistoryJobEntityManager()
                                                .changeTenantIdHistoryJobs(sourceTenantId, targetTenantId))
                                .addResult(Key.ExternalWorkerJobs, jobServiceConfiguration
                                                .getExternalWorkerJobEntityManager().changeTenantIdExternalWorkerJobs(
                                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.EventSubscriptions, cmmnEngineConfiguration
                                                .getEventSubscriptionServiceConfiguration()
                                                .getEventSubscriptionEntityManager().changeTenantIdEventSubscriptions(
                                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.Tasks, taskServiceConfiguration.getTaskEntityManager().changeTenantIdTasks(
                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN))
                                .addResult(Key.HistoricTaskInstances, taskServiceConfiguration
                                                .getHistoricTaskInstanceEntityManager()
                                                .changeTenantIdHistoricTaskInstances(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Key.HistoricTaskLogEntries, taskServiceConfiguration
                                                .getHistoricTaskLogEntryEntityManager()
                                                .changeTenantIdHistoricTaskLogEntries(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .build();
        }

}
