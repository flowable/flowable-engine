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
package org.flowable.cmmn.engine.impl.callback;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.job.api.Job;
import org.flowable.job.service.ScopeAwareInternalJobManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public class DefaultInternalCmmnJobManager extends ScopeAwareInternalJobManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultInternalCmmnJobManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    protected VariableScope resolveVariableScopeInternal(Job job) {
        if (job.getSubScopeId() != null) {
            return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
        }
        return null;
    }

    @Override
    protected boolean handleJobInsertInternal(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).
        return true;
    }

    @Override
    protected void handleJobDeleteInternal(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).        
    }

    @Override
    protected void lockJobScopeInternal(Job job) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        String lockOwner;
        Date lockExpirationTime;

        if (job instanceof JobInfoEntity) {
            lockOwner = ((JobInfoEntity) job).getLockOwner();
            lockExpirationTime = ((JobInfoEntity) job).getLockExpirationTime();
        } else {
            int lockMillis = cmmnEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis();
            GregorianCalendar lockCal = new GregorianCalendar();
            lockCal.setTime(cmmnEngineConfiguration.getClock().getCurrentTime());
            lockCal.add(Calendar.MILLISECOND, lockMillis);

            lockOwner = cmmnEngineConfiguration.getAsyncExecutor().getLockOwner();
            lockExpirationTime = lockCal.getTime();
        }

        caseInstanceEntityManager.updateLockTime(job.getScopeId(), lockOwner, lockExpirationTime);
        
        if (cmmnEngineConfiguration.isLoggingSessionEnabled() && job.getSubScopeId() != null) {
            PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
            if (planItemInstanceEntity != null) {
                CmmnLoggingSessionUtil.addAsyncActivityLoggingData("Locking job for " + planItemInstanceEntity.getPlanItemDefinitionId() + ", with job id " + job.getId(),
                        CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, (JobEntity) job, planItemInstanceEntity.getPlanItemDefinition(), 
                        planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
            }
        }
    }

    @Override
    protected void clearJobScopeLockInternal(Job job) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        caseInstanceEntityManager.clearLockTime(job.getScopeId());
        
        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
            if (planItemInstanceEntity != null) {
                CmmnLoggingSessionUtil.addAsyncActivityLoggingData("Unlocking job for " + planItemInstanceEntity.getPlanItemDefinitionId() + ", with job id " + job.getId(),
                        CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB, (JobEntity) job, planItemInstanceEntity.getPlanItemDefinition(), 
                        planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
            }
        }
    }

    @Override
    protected void preTimerJobDeleteInternal(JobEntity jobEntity, VariableScope variableScope) {
        // Nothing additional needed (no support for endDate for cmmn timer yet)
    }
    
    @Override
    protected void preRepeatedTimerScheduleInternal(TimerJobEntity timerJobEntity, VariableScope variableScope) {

        // In CMMN (and contrary to BPMN), when a timer is repeated a new plan item instance needs to be created
        // as the original one is removed when the timer event has occurred.
        if (variableScope instanceof PlanItemInstanceEntity) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) variableScope;

            PlanItemInstance stagePlanItem = planItemInstanceEntity.getStagePlanItemInstanceEntity();
            if (stagePlanItem == null && planItemInstanceEntity.getStageInstanceId() != null) {
                stagePlanItem = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceEntity.getStageInstanceId());
            }

            // Create new plan item instance based on the data of the original one
            PlanItemInstanceEntity newPlanItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().createPlanItemInstanceEntityBuilder()
                .planItem(planItemInstanceEntity.getPlanItem())
                .caseDefinitionId(planItemInstanceEntity.getCaseDefinitionId())
                .caseInstanceId(planItemInstanceEntity.getCaseInstanceId())
                .stagePlanItemInstance(stagePlanItem)
                .tenantId(planItemInstanceEntity.getTenantId())
                .addToParent(true)
                .create();

            // The plan item instance state needs to be set to available manually. 
            // Leaving it to empty will automatically make it available it and execute the behavior,
            // creating a duplicate timer. The job server logic will take care of scheduling the repeating timer.
            String oldState = newPlanItemInstanceEntity.getState();
            String newState = PlanItemInstanceState.AVAILABLE;
            newPlanItemInstanceEntity.setState(newState);
            CommandContext commandContext = Context.getCommandContext();
            CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
                .executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

            // Plan createOperation, it will also sync planItemInstance history
            CommandContextUtil.getAgenda().planCreatePlanItemInstanceOperation(newPlanItemInstanceEntity);

            // Switch job references to new plan item instance
            timerJobEntity.setSubScopeId(newPlanItemInstanceEntity.getId());
        }
    }
}
