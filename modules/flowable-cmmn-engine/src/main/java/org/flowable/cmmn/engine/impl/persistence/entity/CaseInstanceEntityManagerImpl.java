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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.impl.ChildTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;

/**
 * @author Joram Barrez
 */
public class CaseInstanceEntityManagerImpl extends AbstractCmmnEntityManager<CaseInstanceEntity> implements CaseInstanceEntityManager {

    protected CaseInstanceDataManager caseInstanceDataManager;

    public CaseInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, CaseInstanceDataManager caseInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.caseInstanceDataManager = caseInstanceDataManager;
    }

    @Override
    protected DataManager<CaseInstanceEntity> getDataManager() {
        return caseInstanceDataManager;
    }

    @Override
    public CaseInstanceQuery createCaseInstanceQuery() {
        return new CaseInstanceQueryImpl(cmmnEngineConfiguration.getCommandExecutor());
    }

    @Override
    public List<CaseInstanceEntity> findCaseInstancesByCaseDefinitionId(String caseDefinitionId) {
        return caseInstanceDataManager.findCaseInstancesByCaseDefinitionId(caseDefinitionId);
    }

    @Override
    public List<CaseInstance> findByCriteria(CaseInstanceQuery query) {
        return caseInstanceDataManager.findByCriteria((CaseInstanceQueryImpl) query);
    }

    @Override
    public List<CaseInstance> findWithVariablesByCriteria(CaseInstanceQuery query) {
        return caseInstanceDataManager.findWithVariablesByCriteria((CaseInstanceQueryImpl) query);
    }

    @Override
    public long countByCriteria(CaseInstanceQuery query) {
        return caseInstanceDataManager.countByCriteria((CaseInstanceQueryImpl) query);
    }

    @Override
    public void delete(String caseInstanceId, boolean cascade, String deleteReason) {
        CaseInstanceEntity caseInstanceEntity = caseInstanceDataManager.findById(caseInstanceId);

        // Variables
        getVariableInstanceEntityManager().deleteByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);
        
        // Identity links
        getIdentityLinkEntityManager().deleteIdentityLinksByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);
        
        // Entity links
        if (cmmnEngineConfiguration.isEnableEntityLinks()) {
            getEntityLinkEntityManager().deleteEntityLinksByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);
        }
        
        // Tasks
        TaskEntityManager taskEntityManager = getTaskEntityManager();
        List<TaskEntity> taskEntities = taskEntityManager.findTasksByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);
        for (TaskEntity taskEntity : taskEntities) {
            TaskHelper.deleteTask(taskEntity, deleteReason, cascade, true);
        }
        
        // Event subscriptions
        EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService();
        eventSubscriptionService.deleteEventSubscriptionsForScopeIdAndType(caseInstanceId, ScopeTypes.CMMN);

        // Sentry part instances
        getSentryPartInstanceEntityManager().deleteByCaseInstanceId(caseInstanceId);

        // Runtime milestones
        getMilestoneInstanceEntityManager().deleteByCaseInstanceId(caseInstanceId);

        // Plan item instances
        PlanItemInstanceEntityManager planItemInstanceEntityManager = getPlanItemInstanceEntityManager();
        
        List<PlanItemInstanceEntity> stagePlanItemInstances = new ArrayList<>();
        List<PlanItemInstanceEntity> childTaskPlanItemInstances = new ArrayList<>();
        collectPlanItemInstances(caseInstanceEntity, stagePlanItemInstances, childTaskPlanItemInstances);

        // Plan item instances are removed per stage, in reversed order
        for (int i = stagePlanItemInstances.size() - 1; i>=0; i--) {
            planItemInstanceEntityManager.deleteByStageInstanceId(stagePlanItemInstances.get(i).getId());
        }
        planItemInstanceEntityManager.deleteByCaseInstanceId(caseInstanceId); // root plan item instances

        // Child task behaviors have potentially associated child entities (case/process instances)
        for (PlanItemInstanceEntity childTaskPlanItemInstance : childTaskPlanItemInstances) {
            if (PlanItemInstanceState.ACTIVE.equals(childTaskPlanItemInstance.getState())) {
                ChildTaskActivityBehavior childTaskActivityBehavior = (ChildTaskActivityBehavior) childTaskPlanItemInstance.getPlanItem().getBehavior();
                childTaskActivityBehavior.deleteChildEntity(CommandContextUtil.getCommandContext(), childTaskPlanItemInstance, cascade);
            }
        }

        // Jobs have dependencies (byte array refs that need to be deleted, so no immediate delete for the moment)
        JobEntityManager jobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getJobEntityManager();
        List<Job> jobs = jobEntityManager.findJobsByQueryCriteria(new JobQueryImpl().scopeId(caseInstanceId).scopeType(ScopeTypes.CMMN));
        for (Job job : jobs) {
            jobEntityManager.delete(job.getId());
        }
        TimerJobEntityManager timerJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager();
        List<Job> timerJobs = timerJobEntityManager.findJobsByQueryCriteria(new TimerJobQueryImpl().scopeId(caseInstanceId).scopeType(ScopeTypes.CMMN));
        for (Job timerJob : timerJobs) {
            timerJobEntityManager.delete(timerJob.getId());
        }
        SuspendedJobEntityManager suspendedJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getSuspendedJobEntityManager();
        List<Job> suspendedJobs = suspendedJobEntityManager.findJobsByQueryCriteria(new SuspendedJobQueryImpl().scopeId(caseInstanceId).scopeType(ScopeTypes.CMMN));
        for (Job suspendedJob : suspendedJobs) {
            suspendedJobEntityManager.delete(suspendedJob.getId());
        }
        DeadLetterJobEntityManager deadLetterJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getDeadLetterJobEntityManager();
        List<Job> deadLetterJobs = deadLetterJobEntityManager.findJobsByQueryCriteria(new DeadLetterJobQueryImpl().scopeId(caseInstanceId).scopeType(ScopeTypes.CMMN));
        for (Job deadLetterJob : deadLetterJobs) {
            deadLetterJobEntityManager.delete(deadLetterJob.getId());
        }

        // Actual case instance
        delete(caseInstanceEntity);
    }

    protected void collectPlanItemInstances(PlanItemInstanceContainer planItemInstanceContainer,
        List<PlanItemInstanceEntity> stagePlanItemInstanceEntities, List<PlanItemInstanceEntity> childTaskPlanItemInstanceEntities) {
        for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceContainer.getChildPlanItemInstances()) {

            if (planItemInstanceEntity.isStage()) {
                stagePlanItemInstanceEntities.add(planItemInstanceEntity);
                collectPlanItemInstances(planItemInstanceEntity, stagePlanItemInstanceEntities, childTaskPlanItemInstanceEntities);

            } else if (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getBehavior() != null
                && planItemInstanceEntity.getPlanItem().getBehavior() instanceof ChildTaskActivityBehavior) {
                    childTaskPlanItemInstanceEntities.add(planItemInstanceEntity);
            }

        }
    }

    @Override
    public void updateLockTime(String caseInstanceId) {
        Date expirationTime = getCmmnEngineConfiguration().getClock().getCurrentTime();
        int lockMillis = getCmmnEngineConfiguration().getAsyncExecutor().getAsyncJobLockTimeInMillis();

        GregorianCalendar lockCal = new GregorianCalendar();
        lockCal.setTime(expirationTime);
        lockCal.add(Calendar.MILLISECOND, lockMillis);
        Date lockDate = lockCal.getTime();

        caseInstanceDataManager.updateLockTime(caseInstanceId, lockDate, expirationTime);
    }

    @Override
    public void clearLockTime(String caseInstanceId) {
        caseInstanceDataManager.clearLockTime(caseInstanceId);
    }

    @Override
    public void updateCaseInstanceBusinessKey(CaseInstanceEntity caseInstanceEntity, String businessKey) {
        if (businessKey != null) {
            caseInstanceEntity.setBusinessKey(businessKey);
            getCmmnEngineConfiguration().getCmmnHistoryManager().recordUpdateBusinessKey(caseInstanceEntity, businessKey);
        }
    }


}
