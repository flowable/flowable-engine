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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
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
import org.flowable.variable.api.type.VariableScopeType;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

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
    public long countByCriteria(CaseInstanceQuery query) {
        return caseInstanceDataManager.countByCriteria((CaseInstanceQueryImpl) query);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        caseInstanceDataManager.deleteByCaseDefinitionId(caseDefinitionId);
    }
    
    @Override
    public void deleteCaseInstanceAndRelatedData(String caseInstanceId, String deleteReason) {
        CaseInstanceEntity caseInstanceEntity = caseInstanceDataManager.findById(caseInstanceId);

        // Variables
        VariableInstanceEntityManager variableInstanceEntityManager = getVariableInstanceEntityManager();
        List<VariableInstanceEntity> variableInstanceEntities = variableInstanceEntityManager
                .findVariableInstanceByScopeIdAndScopeType(caseInstanceId, VariableScopeType.CMMN);
        for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
            variableInstanceEntityManager.delete(variableInstanceEntity);
        }
        
        // Tasks
        TaskEntityManager taskEntityManager = getTaskEntityManager();
        List<TaskEntity> taskEntities = taskEntityManager.findTasksByScopeIdAndScopeType(caseInstanceId, VariableScopeType.CMMN);
        for (TaskEntity taskEntity : taskEntities) {
            TaskHelper.deleteTask(taskEntity, deleteReason, false, true);
        }
        
        // Sentry part instances
        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = getSentryPartInstanceEntityManager();
        List<SentryPartInstanceEntity> sentryPartInstances = caseInstanceEntity.getSatisfiedSentryPartInstances();
        for (SentryPartInstanceEntity sentryPartInstanceEntity : sentryPartInstances) {
            sentryPartInstanceEntityManager.delete(sentryPartInstanceEntity);
        }

        // Runtime milestones
        MilestoneInstanceEntityManager milestoneInstanceEntityManager = getMilestoneInstanceEntityManager(); 
        List<MilestoneInstanceEntity> milestoneInstanceEntities = milestoneInstanceEntityManager
                .findMilestoneInstancesByCaseInstanceId(caseInstanceId);
        if (milestoneInstanceEntities != null) {
            for (MilestoneInstanceEntity milestoneInstanceEntity : milestoneInstanceEntities) {
                milestoneInstanceEntityManager.delete(milestoneInstanceEntity);
            }
        }
        
        // Plan item instances
        PlanItemInstanceEntityManager planItemInstanceEntityManager = getPlanItemInstanceEntityManager();
        List<PlanItemInstanceEntity> planItemInstanceEntities = planItemInstanceEntityManager
                .findAllChildPlanItemInstancesForCaseInstance(caseInstanceId);
        Collections.reverse(planItemInstanceEntities); // Need to have them in leaf -> root order
        if (planItemInstanceEntities != null) {
            for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceEntities) {
                if (planItemInstanceEntity.getSatisfiedSentryPartInstances() != null) {
                    for (SentryPartInstanceEntity sentryPartInstanceEntity : planItemInstanceEntity.getSatisfiedSentryPartInstances()) {
                        sentryPartInstanceEntityManager.delete(sentryPartInstanceEntity);
                    }
                }
                planItemInstanceEntityManager.delete(planItemInstanceEntity);
            }
        }
        
        // Jobs
        JobEntityManager jobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getJobEntityManager();
        List<Job> jobs = jobEntityManager.findJobsByQueryCriteria(new JobQueryImpl().scopeId(caseInstanceId).scopeType(VariableScopeType.CMMN));
        for (Job job : jobs) {
            jobEntityManager.delete(job.getId());
        }
        TimerJobEntityManager timerJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager();
        List<Job> timerJobs = timerJobEntityManager.findJobsByQueryCriteria(new TimerJobQueryImpl().scopeId(caseInstanceId).scopeType(VariableScopeType.CMMN));
        for (Job timerJob : timerJobs) {
            timerJobEntityManager.delete(timerJob.getId());
        }
        SuspendedJobEntityManager suspendedJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getSuspendedJobEntityManager();
        List<Job> suspendedJobs = suspendedJobEntityManager.findJobsByQueryCriteria(new SuspendedJobQueryImpl().scopeId(caseInstanceId).scopeType(VariableScopeType.CMMN));
        for (Job suspendedJob : suspendedJobs) {
            suspendedJobEntityManager.delete(suspendedJob.getId());
        }
        DeadLetterJobEntityManager deadLetterJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getDeadLetterJobEntityManager();
        List<Job> deadLetterJobs = deadLetterJobEntityManager.findJobsByQueryCriteria(new DeadLetterJobQueryImpl().scopeId(caseInstanceId).scopeType(VariableScopeType.CMMN));
        for (Job deadLetterJob : deadLetterJobs) {
            deadLetterJobEntityManager.delete(deadLetterJob.getId());
        }

        // Actual case instance
        delete(caseInstanceEntity);
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

}
