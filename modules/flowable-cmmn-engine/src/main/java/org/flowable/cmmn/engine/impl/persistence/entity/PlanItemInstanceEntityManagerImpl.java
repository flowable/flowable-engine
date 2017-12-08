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
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.variable.api.type.VariableScopeType;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceEntityManagerImpl extends AbstractCmmnEntityManager<PlanItemInstanceEntity> implements PlanItemInstanceEntityManager {

    protected PlanItemInstanceDataManager planItemInstanceDataManager;

    public PlanItemInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, PlanItemInstanceDataManager planItemInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.planItemInstanceDataManager = planItemInstanceDataManager;
    }
    
    @Override
    protected DataManager<PlanItemInstanceEntity> getDataManager() {
        return planItemInstanceDataManager;
    }
    
    @Override
    public PlanItemInstanceEntity createChildPlanItemInstance(PlanItem planItem, String caseDefinitionId, String caseInstanceId, 
            String stagePlanItemInstanceId, String tenantId, boolean addToParent) {
        
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ExpressionManager expressionManager = cmmnEngineConfiguration.getExpressionManager();
        CaseInstanceEntity caseInstanceEntity = getCaseInstanceEntityManager().findById(caseInstanceId);
        
        PlanItemInstanceEntity planItemInstanceEntity = create();
        planItemInstanceEntity.setCaseDefinitionId(caseDefinitionId);
        planItemInstanceEntity.setCaseInstanceId(caseInstanceId);
        if (planItem.getName() != null) {
            Expression nameExpression = expressionManager.createExpression(planItem.getName());
            planItemInstanceEntity.setName(nameExpression.getValue(caseInstanceEntity).toString());
        }
        planItemInstanceEntity.setStartTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        planItemInstanceEntity.setElementId(planItem.getId());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        if (planItemDefinition != null) {
            planItemInstanceEntity.setPlanItemDefinitionId(planItemDefinition.getId());
            planItemInstanceEntity.setPlanItemDefinitionType(planItemDefinition.getClass().getSimpleName().toLowerCase());
        }
        planItemInstanceEntity.setStage(false);
        planItemInstanceEntity.setStageInstanceId(stagePlanItemInstanceId);
        planItemInstanceEntity.setTenantId(tenantId);
       
        insert(planItemInstanceEntity);
        
        if (addToParent) {
            addPlanItemInstanceToParent(commandContext, planItemInstanceEntity);
        }
        
        return planItemInstanceEntity;
    }
    
    protected void addPlanItemInstanceToParent(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity.getStageInstanceId() != null) {
            PlanItemInstanceEntity stagePlanItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .findById(planItemInstanceEntity.getStageInstanceId());
            stagePlanItemInstanceEntity.getChildPlanItemInstances().add(planItemInstanceEntity);
        } else {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
            caseInstanceEntity.getChildPlanItemInstances().add(planItemInstanceEntity);
        }
    }
    
    @Override
    public List<PlanItemInstanceEntity> findImmediateChildPlanItemInstancesForCaseInstance(String caseInstance) {
        return planItemInstanceDataManager.findChildPlanItemInstancesForCaseInstance(caseInstance);
    }
    
    @Override
    public List<PlanItemInstanceEntity> findAllChildPlanItemInstancesForCaseInstance(String caseInstanceId) {
        List<PlanItemInstanceEntity> allChildPlanItemInstances = new ArrayList<>();
        collectChildPlanItemInstances(getCaseInstanceEntityManager().findById(caseInstanceId), allChildPlanItemInstances);
        return allChildPlanItemInstances;
    }

    protected void collectChildPlanItemInstances(PlanItemInstanceContainer planItemInstanceContainer, List<PlanItemInstanceEntity> allChildPlanItemInstances) {
        for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceContainer.getChildPlanItemInstances()) {
            allChildPlanItemInstances.add(planItemInstanceEntity);
            if (planItemInstanceEntity.getChildPlanItemInstances() != null) {
                collectChildPlanItemInstances(planItemInstanceEntity, allChildPlanItemInstances);
            }
        }
    }
    
    @Override
    public List<PlanItemInstanceEntity> findChildPlanItemInstancesForStage(String stagePlanItemInstanceId) {
        return planItemInstanceDataManager.findChildPlanItemInstancesForStage(stagePlanItemInstanceId);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        planItemInstanceDataManager.deleteByCaseDefinitionId(caseDefinitionId);
    }
    
    public PlanItemInstanceQuery createPlanItemInstanceQuery() {
        return new PlanItemInstanceQueryImpl(cmmnEngineConfiguration.getCommandExecutor());
    }

    @Override
    public long countByCriteria(PlanItemInstanceQuery planItemInstanceQuery) {
        return planItemInstanceDataManager.countByCriteria((PlanItemInstanceQueryImpl) planItemInstanceQuery);
    }

    @Override
    public List<PlanItemInstance> findByCriteria(PlanItemInstanceQuery planItemInstanceQuery) {
        return planItemInstanceDataManager.findByCriteria((PlanItemInstanceQueryImpl) planItemInstanceQuery);
    }

    @Override
    public void delete(PlanItemInstanceEntity planItemInstanceEntity, boolean fireEvent) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        // Variables
        VariableInstanceEntityManager variableInstanceEntityManager 
            = CommandContextUtil.getVariableServiceConfiguration(commandContext).getVariableInstanceEntityManager();
        List<VariableInstanceEntity> variableInstanceEntities = variableInstanceEntityManager
                .findVariableInstanceBySubScopeIdAndScopeType(planItemInstanceEntity.getId(), VariableScopeType.CMMN);
        for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
            variableInstanceEntityManager.delete(variableInstanceEntity);
        }
        
        List<PlanItemInstanceEntity> childPlanItems = findChildPlanItemInstancesForStage(planItemInstanceEntity.getId());
        if (childPlanItems != null && childPlanItems.size() > 0) {
            for (PlanItemInstanceEntity childPlanItem : childPlanItems) {
                delete(childPlanItem, fireEvent);
            }
        }
        
        getDataManager().delete(planItemInstanceEntity);
    }
    
}
