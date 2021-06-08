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

public class ChangeTenantIdCmmnCompleteCmd implements Command<ChangeTenantIdResult> {

        private final static Logger logger = LoggerFactory.getLogger(ChangeTenantIdCmmnCompleteCmd.class);

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
                logger.debug("Executing case instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
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
                                                .changeTenantIdCmmnCaseInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(MilestoneInstances, cmmnEngineConfiguration
                                                .getMilestoneInstanceEntityManager()
                                                .changeTenantIdCmmnMilestoneInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(PlanItemInstances, cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                                                .changeTenantIdCmmnPlanItemInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricCaseInstances, cmmnEngineConfiguration
                                                .getHistoricCaseInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricCaseInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricMilestoneInstances, cmmnEngineConfiguration
                                                .getHistoricMilestoneInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricMilestoneInstances(sourceTenantId,
                                                                targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(HistoricPlanItemInstances, cmmnEngineConfiguration
                                                .getHistoricPlanItemInstanceEntityManager()
                                                .changeTenantIdCmmnHistoricPlanItemInstances(sourceTenantId,
                                                                targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .addResult(Jobs, jobServiceConfiguration.getJobEntityManager().changeTenantIdJobs(
                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN))
                                .addResult(TimerJobs, jobServiceConfiguration.getTimerJobEntityManager()
                                                .changeTenantIdTimerJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(SuspendedJobs, jobServiceConfiguration.getSuspendedJobEntityManager()
                                                .changeTenantIdSuspendedJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(DeadLetterJobs, jobServiceConfiguration.getDeadLetterJobEntityManager()
                                                .changeTenantIdDeadLetterJobs(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(HistoryJobs, jobServiceConfiguration.getHistoryJobEntityManager()
                                                .changeTenantIdHistoryJobs(sourceTenantId, targetTenantId))
                                .addResult(ExternalWorkerJobs, jobServiceConfiguration
                                                .getExternalWorkerJobEntityManager().changeTenantIdExternalWorkerJobs(
                                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(EventSubscriptions, cmmnEngineConfiguration
                                                .getEventSubscriptionServiceConfiguration()
                                                .getEventSubscriptionEntityManager().changeTenantIdEventSubscriptions(
                                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(Tasks, taskServiceConfiguration.getTaskEntityManager().changeTenantIdTasks(
                                                sourceTenantId, targetTenantId, defaultTenantId,
                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN))
                                .addResult(HistoricTaskInstances, taskServiceConfiguration
                                                .getHistoricTaskInstanceEntityManager()
                                                .changeTenantIdHistoricTaskInstances(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .addResult(HistoricTaskLogEntries, taskServiceConfiguration
                                                .getHistoricTaskLogEntryEntityManager()
                                                .changeTenantIdHistoricTaskLogEntries(sourceTenantId, targetTenantId,
                                                                defaultTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions,
                                                                ScopeTypes.CMMN))
                                .build();
        }

}
