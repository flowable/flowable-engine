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

import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.*;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.task.service.TaskServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdCmmnSimulateCmd implements Command<ChangeTenantIdResult> {

        private final static Logger logger = LoggerFactory.getLogger(ChangeTenantIdCmmnSimulateCmd.class);

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
                logger.debug("Simulating case instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
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
                                .addResult(CaseInstances, cmmnEngineConfiguration.getCaseInstanceEntityManager()
                                                .countChangeTenantIdCmmnCaseInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(MilestoneInstances, cmmnEngineConfiguration
                                                .getMilestoneInstanceEntityManager()
                                                .countChangeTenantIdCmmnMilestoneInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(PlanItemInstances, cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                                                .countChangeTenantIdCmmnPlanItemInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricCaseInstances, cmmnEngineConfiguration
                                                .getHistoricCaseInstanceEntityManager()
                                                .countChangeTenantIdCmmnHistoricCaseInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricMilestoneInstances, cmmnEngineConfiguration
                                                .getHistoricMilestoneInstanceEntityManager()
                                                .countChangeTenantIdCmmnHistoricMilestoneInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricPlanItemInstances, cmmnEngineConfiguration
                                                .getHistoricPlanItemInstanceEntityManager()
                                                .countChangeTenantIdCmmnHistoricPlanItemInstances(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Jobs, jobServiceConfiguration.getJobEntityManager().countChangeTenantIdJobs(
                                                sourceTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN))
                                .addResult(TimerJobs, jobServiceConfiguration.getTimerJobEntityManager()
                                                .countChangeTenantIdTimerJobs(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(SuspendedJobs, jobServiceConfiguration.getSuspendedJobEntityManager()
                                                .countChangeTenantIdSuspendedJobs(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(DeadLetterJobs, jobServiceConfiguration.getDeadLetterJobEntityManager()
                                                .countChangeTenantIdDeadLetterJobs(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(HistoryJobs,
                                                jobServiceConfiguration.getHistoryJobEntityManager()
                                                                .countChangeTenantIdHistoryJobs(sourceTenantId))
                                .addResult(ExternalWorkerJobs, jobServiceConfiguration
                                                .getExternalWorkerJobEntityManager()
                                                .countChangeTenantIdExternalWorkerJobs(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(EventSubscriptions, cmmnEngineConfiguration
                                                .getEventSubscriptionServiceConfiguration()
                                                .getEventSubscriptionEntityManager()
                                                .countChangeTenantIdEventSubscriptions(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Tasks, taskServiceConfiguration.getTaskEntityManager()
                                                .countChangeTenantIdTasks(sourceTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(HistoricTaskInstances, taskServiceConfiguration
                                                .getHistoricTaskInstanceEntityManager()
                                                .countChangeTenantIdHistoricTaskInstances(sourceTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(HistoricTaskLogEntries, taskServiceConfiguration
                                                .getHistoricTaskLogEntryEntityManager()
                                                .countChangeTenantIdHistoricTaskLogEntries(sourceTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .build();
        }

}
