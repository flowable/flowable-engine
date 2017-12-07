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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
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

        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        // Variables
        VariableInstanceEntityManager variableInstanceEntityManager 
            = CommandContextUtil.getVariableServiceConfiguration(commandContext).getVariableInstanceEntityManager();
        List<VariableInstanceEntity> variableInstanceEntities = variableInstanceEntityManager
                .findVariableInstanceByScopeIdAndScopeType(caseInstanceId, VariableScopeType.CMMN);
        for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
            variableInstanceEntityManager.delete(variableInstanceEntity);
        }
        
        // Tasks
        TaskEntityManager taskEntityManager = CommandContextUtil.getTaskServiceConfiguration(commandContext).getTaskEntityManager();
        List<TaskEntity> taskEntities = taskEntityManager.findTasksByScopeIdAndScopeType(caseInstanceId, VariableScopeType.CMMN);
        for (TaskEntity taskEntity : taskEntities) {
            TaskHelper.deleteTask(taskEntity, deleteReason, false, true);
        }
        
        // Sentry part instances
        List<SentryPartInstanceEntity> sentryPartInstances = caseInstanceEntity.getSatisfiedSentryPartInstances();
        for (SentryPartInstanceEntity sentryPartInstanceEntity : sentryPartInstances) {
            CommandContextUtil.getSentryPartInstanceEntityManager(commandContext).delete(sentryPartInstanceEntity);
        }

        // Runtime milestones
        MilestoneInstanceEntityManager milestoneInstanceEntityManager = CommandContextUtil.getMilestoneInstanceEntityManager(commandContext);
        List<MilestoneInstanceEntity> milestoneInstanceEntities = milestoneInstanceEntityManager
                .findMilestoneInstancesByCaseInstanceId(caseInstanceId);
        if (milestoneInstanceEntities != null) {
            for (MilestoneInstanceEntity milestoneInstanceEntity : milestoneInstanceEntities) {
                milestoneInstanceEntityManager.delete(milestoneInstanceEntity);
            }
        }
        
        // Plan item instances
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        List<PlanItemInstanceEntity> planItemInstanceEntities = planItemInstanceEntityManager
                .findChildPlanItemInstancesForCaseInstance(caseInstanceId);
        if (planItemInstanceEntities != null) {
            for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceEntities) {
                planItemInstanceEntityManager.delete(planItemInstanceEntity);
            }
        }

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
