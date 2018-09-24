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

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public class DefaultInternalCmmnJobManager implements InternalJobManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultInternalCmmnJobManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public VariableScope resolveVariableScope(Job job) {
        if (job.getSubScopeId() != null) {
            return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
        }
        return null;
    }

    @Override
    public boolean handleJobInsert(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).
        return true;
    }

    @Override
    public void handleJobDelete(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).        
    }

    @Override
    public void lockJobScope(Job job) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        caseInstanceEntityManager.updateLockTime(job.getScopeId());
    }

    @Override
    public void clearJobScopeLock(Job job) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        caseInstanceEntityManager.clearLockTime(job.getScopeId());
    }

    @Override
    public void preTimerJobDelete(JobEntity jobEntity, VariableScope variableScope) {
        // Nothing additional needed (no support for endDate for cmmn timer yet)
    }
    
    @Override
    public void preRepeatedTimerSchedule(TimerJobEntity timerJobEntity, VariableScope variableScope) {
        // In CMMN (and contrary to BPMN), when a timer is repeated a new plan item instance needs to be created
        // as the original one is removed when the timer event has occurred.
        if (variableScope instanceof PlanItemInstanceEntity) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) variableScope;
            
            // Create new plan item instance based on the data of the original one
            PlanItemInstanceEntity newPlanItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .createChildPlanItemInstance(planItemInstanceEntity.getPlanItem(), 
                            planItemInstanceEntity.getCaseDefinitionId(), 
                            planItemInstanceEntity.getCaseInstanceId(), 
                            planItemInstanceEntity.getStageInstanceId(), 
                            planItemInstanceEntity.getTenantId(), 
                            true);
            
            // The plan item instance state needs to be set to available manually. 
            // Leaving it to empty will automatically make it available it and execute the behavior,
            // creating a duplicate timer. The job server logic will take care of scheduling 
            // the repeating timer.
            newPlanItemInstanceEntity.setState(PlanItemInstanceState.AVAILABLE);
            // Plan createOperation, it will also sync planItemInstance history
            CommandContextUtil.getAgenda().planCreatePlanItemInstanceOperation(newPlanItemInstanceEntity);
            // Switch job references to new plan item instance
            timerJobEntity.setSubScopeId(newPlanItemInstanceEntity.getId());
        }
    }

}
