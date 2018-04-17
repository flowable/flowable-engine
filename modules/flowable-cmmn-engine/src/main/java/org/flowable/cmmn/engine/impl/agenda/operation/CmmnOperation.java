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
package org.flowable.cmmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class CmmnOperation implements Runnable {
    
    protected CommandContext commandContext;
    
    public CmmnOperation() {
    }

    public CmmnOperation(CommandContext commandContext) {
        this.commandContext = commandContext;
    }
    
    protected Stage getStage(PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null) {
            PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
            if (planItemDefinition instanceof Stage) {
                return (Stage) planItemDefinition;
            } else {
                return planItemDefinition.getParentStage();
            }
        } else {
            return getStage(planItemInstanceEntity.getCaseDefinitionId(), planItemInstanceEntity.getElementId());
        }
    }
    
    protected Stage getStage(String caseDefinitionId, String stageId) {
        return CaseDefinitionUtil.getCase(caseDefinitionId).findStage(stageId);
    }
    
    protected boolean isStage(PlanItemInstanceEntity planItemInstanceEntity) {
        return (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof Stage);
    }
    
    protected Stage getPlanModel(CaseInstanceEntity caseInstanceEntity) {
        return CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
    }
    
    
    protected List<PlanItemInstanceEntity> createPlanItemInstances(CommandContext commandContext,
                                                                        List<PlanItem> planItems,
                                                                        String caseDefinitionId,
                                                                        String caseInstanceId, 
                                                                        String stagePlanItemInstanceId, 
                                                                        String tenantId) {
        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>();
        for (PlanItem planItem : planItems) {
            planItemInstances.add(CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .createChildPlanItemInstance(planItem, caseDefinitionId, caseInstanceId, stagePlanItemInstanceId, tenantId, true));
        }
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(planItemInstance);
        }
        return planItemInstances;
    }
    
    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy) {
        return copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntityToCopy, true);
    }
    
    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy, boolean addToParent) {
        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).createChildPlanItemInstance( 
                planItemInstanceEntityToCopy.getPlanItem(), 
                planItemInstanceEntityToCopy.getCaseDefinitionId(), 
                planItemInstanceEntityToCopy.getCaseInstanceId(), 
                planItemInstanceEntityToCopy.getStageInstanceId(), 
                planItemInstanceEntityToCopy.getTenantId(),
                addToParent);
        return planItemInstanceEntity;
    }
    
    protected int getRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        Integer counter = (Integer) repeatingPlanItemInstanceEntity.getVariableLocal(getCounterVariable(repeatingPlanItemInstanceEntity));
        if (counter == null) {
            return 0;
        } else {
            return counter.intValue();
        }
    }
    
    protected void setRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity, int counterValue) {
        repeatingPlanItemInstanceEntity.setVariableLocal(getCounterVariable(repeatingPlanItemInstanceEntity), counterValue);
    }

    protected String getCounterVariable(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        String repetitionCounterVariableName = repeatingPlanItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getRepetitionCounterVariableName();
        return repetitionCounterVariableName;
    }
    
    protected boolean evaluateRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity.getPlanItem() != null) {
            PlanItem planItem = planItemInstanceEntity.getPlanItem();
            if (planItem.getItemControl() != null && planItem.getItemControl().getRepetitionRule() != null) {
                String repetitionCondition = planItem.getItemControl().getRepetitionRule().getCondition();
                boolean isRepeating = false;
                if (StringUtils.isNotEmpty(repetitionCondition)) {
                    isRepeating = evaluateBooleanExpression(commandContext, planItemInstanceEntity, repetitionCondition);
                } else {
                    isRepeating = true; // no condition set, but a repetition rule defined is assumed to be defaulting to true
                }
                return isRepeating;
            }
        }
        return false;
    }

    protected boolean evaluateBooleanExpression(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String condition) {
        Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(condition);
        Object evaluationResult = expression.getValue(planItemInstanceEntity);
        if (evaluationResult instanceof Boolean) {
            return (boolean) evaluationResult;
        } else if (evaluationResult instanceof String) {
            return ((String) evaluationResult).toLowerCase().equals("true");
        } else {
            throw new FlowableException("Expression condition " + condition + " did not evaluate to a boolean value");
        }
    }

}