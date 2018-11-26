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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
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
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof Stage) {
            return (Stage) planItemDefinition;
        } else {
            return planItemDefinition.getParentStage();
        }
    }
    
    protected boolean isStage(PlanItemInstanceEntity planItemInstanceEntity) {
        return (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof Stage);
    }
    
    protected Stage getPlanModel(CaseInstanceEntity caseInstanceEntity) {
        return CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
    }
    
    
    protected List<PlanItemInstanceEntity> createPlanItemInstances(CommandContext commandContext, List<PlanItem> planItems, String caseDefinitionId,
            String caseInstanceId, PlanItemInstanceEntity stagePlanItemInstanceEntity, String tenantId) {

        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>();
        for (PlanItem planItem : planItems) {

            // In some cases (e.g. cross-border triggering of a sentry, the child plan item instance has been activated already
            // As such, it doesn't need to be created again (this is the if check here, which goes against the cache)

            if (stagePlanItemInstanceEntity == null || !childPlanItemInstanceDoesnExist(stagePlanItemInstanceEntity, planItem)) {
                PlanItemInstanceEntity childPlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .createChildPlanItemInstance(planItem,
                        caseDefinitionId,
                        caseInstanceId,
                        stagePlanItemInstanceEntity != null ? stagePlanItemInstanceEntity.getId() : null,
                        tenantId,
                        true);
                planItemInstances.add(childPlanItemInstance);
                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childPlanItemInstance);
            }
        }
        return planItemInstances;
    }

    protected boolean childPlanItemInstanceDoesnExist(PlanItemInstanceEntity stagePlanItemInstanceEntity, PlanItem planItem) {
        List<PlanItemInstanceEntity> childPlanItemInstances = stagePlanItemInstanceEntity.getChildPlanItemInstances();
        if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity childPlanItemInstanceEntity : childPlanItemInstances) {
                if (childPlanItemInstanceEntity.getPlanItem() != null && planItem.getId().equals(childPlanItemInstanceEntity.getPlanItem().getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy, boolean addToParent) {
        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).createChildPlanItemInstance(
                planItemInstanceEntityToCopy.getPlanItem(),
                planItemInstanceEntityToCopy.getCaseDefinitionId(),
                planItemInstanceEntityToCopy.getCaseInstanceId(),
                planItemInstanceEntityToCopy.getStageInstanceId(),
                planItemInstanceEntityToCopy.getTenantId(),
                addToParent);

        if (hasRepetitionRule(planItemInstanceEntityToCopy)) {
            int counter = getRepetitionCounter(planItemInstanceEntityToCopy);
            setRepetitionCounter(planItemInstanceEntity, counter);
        }

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

    protected boolean hasRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity != null && planItemInstanceEntity.getPlanItem() != null) {
            return hasRepetitionRule(planItemInstanceEntity.getPlanItem());
        }
        return false;
    }

    protected boolean hasRepetitionRule(PlanItem planItem) {
        return planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null;
    }

    protected boolean evaluateRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity) {
        if (hasRepetitionRule(planItemInstanceEntity)) {
            String repetitionCondition = planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getCondition();
            return evaluateRepetitionRule(planItemInstanceEntity, repetitionCondition);
        }
        return false;
    }

    protected boolean evaluateRepetitionRule(VariableContainer variableContainer, String repetitionCondition) {
        if (StringUtils.isNotEmpty(repetitionCondition)) {
            return evaluateBooleanExpression(commandContext, variableContainer, repetitionCondition);
        } else {
            return true; // no condition set, but a repetition rule defined is assumed to be defaulting to true
        }
    }

    protected boolean evaluateBooleanExpression(CommandContext commandContext, VariableContainer variableContainer, String condition) {
        Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(condition);
        Object evaluationResult = expression.getValue(variableContainer);
        if (evaluationResult instanceof Boolean) {
            return (boolean) evaluationResult;
        } else if (evaluationResult instanceof String) {
            return ((String) evaluationResult).toLowerCase().equals("true");
        } else {
            throw new FlowableException("Expression condition " + condition + " did not evaluate to a boolean value");
        }
    }

}