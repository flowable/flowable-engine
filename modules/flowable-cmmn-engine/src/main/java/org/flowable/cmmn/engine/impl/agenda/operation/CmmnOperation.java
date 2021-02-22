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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class CmmnOperation implements Runnable {
    
    protected CommandContext commandContext;
    protected boolean isNoop = false; // flag indicating whether this operation did something. False by default, as all operation should typically do something.
    
    public CmmnOperation() {
    }

    public CmmnOperation(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * @return The id of the case instance related to this operation.
     */
    public abstract String getCaseInstanceId();

    protected Stage getStage(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof Stage) {
            return (Stage) planItemDefinition;
        } else {
            return planItemDefinition.getParentStage();
        }
    }

    public boolean isStage(PlanItemInstanceEntity planItemInstanceEntity) {
        return (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof Stage);
    }

    public Stage getPlanModel(CaseInstanceEntity caseInstanceEntity) {
        return CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
    }
    
    
    protected List<PlanItemInstanceEntity> createPlanItemInstancesForNewStage(CommandContext commandContext, List<PlanItem> planItems, String caseDefinitionId,
            CaseInstanceEntity caseInstanceEntity, PlanItemInstanceEntity stagePlanItemInstanceEntity, String tenantId) {

        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>();
        for (PlanItem planItem : planItems) {

            // In some cases (e.g. cross-border triggering of a sentry, the child plan item instance has been activated already
            // As such, it doesn't need to be created again (this is the if check here, which goes against the cache)

            if (stagePlanItemInstanceEntity == null || !childPlanItemInstanceForPlanItemExists(stagePlanItemInstanceEntity, planItem)) {

                String caseInstanceId = null;
                if (caseInstanceEntity != null) {
                    caseInstanceId = caseInstanceEntity.getId();
                } else if (stagePlanItemInstanceEntity != null) {
                    caseInstanceId = stagePlanItemInstanceEntity.getCaseInstanceId();
                }

                PlanItemInstanceEntity childPlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .createPlanItemInstanceEntityBuilder()
                    .planItem(planItem)
                    .caseDefinitionId(caseDefinitionId)
                    .caseInstanceId(caseInstanceId)
                    .stagePlanItemInstance(stagePlanItemInstanceEntity)
                    .tenantId(tenantId)
                    .addToParent(true)
                    // we silently ignore any exceptions evaluating the name for the new plan item, if it has repetition on a collection, as the item / itemIndex
                    // local variables might not yet be available
                    .silentNameExpressionEvaluation(ExpressionUtil.hasRepetitionOnCollection(planItem))
                    .create();

                planItemInstances.add(childPlanItemInstance);
                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childPlanItemInstance);

            }
        }
        return planItemInstances;
    }

    protected boolean childPlanItemInstanceForPlanItemExists(PlanItemInstanceContainer planItemInstanceContainer, PlanItem planItem) {
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstanceContainer.getChildPlanItemInstances();
        if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity childPlanItemInstanceEntity : childPlanItemInstances) {
                if (childPlanItemInstanceEntity.getPlanItem() != null && planItem.getId().equals(childPlanItemInstanceEntity.getPlanItem().getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEventListenerWithAvailableCondition(PlanItem planItem) {
        if (planItem.getPlanItemDefinition() != null && planItem.getPlanItemDefinition() instanceof EventListener) {
            EventListener eventListener = (EventListener) planItem.getPlanItemDefinition();
            return StringUtils.isNotEmpty(eventListener.getAvailableConditionExpression());
        }
        return false;
    }

    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy,
        boolean addToParent, boolean silentNameExpressionEvaluation) {
        return copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntityToCopy, null, addToParent, silentNameExpressionEvaluation);
    }

    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy,
        Map<String, Object> localVariables, boolean addToParent, boolean silentNameExpressionEvaluation) {

        if (ExpressionUtil.hasRepetitionRule(planItemInstanceEntityToCopy)) {
            int counter = getRepetitionCounter(planItemInstanceEntityToCopy);
            if (localVariables == null) {
                localVariables = new HashMap<>(0);
            }
            localVariables.put(getCounterVariable(planItemInstanceEntityToCopy), counter);
        }

        PlanItemInstance stagePlanItem = planItemInstanceEntityToCopy.getStagePlanItemInstanceEntity();
        if (stagePlanItem == null && planItemInstanceEntityToCopy.getStageInstanceId() != null) {
            stagePlanItem = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceEntityToCopy.getStageInstanceId());
        }

        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
            .createPlanItemInstanceEntityBuilder()
            .planItem(planItemInstanceEntityToCopy.getPlanItem())
            .caseDefinitionId(planItemInstanceEntityToCopy.getCaseDefinitionId())
            .caseInstanceId(planItemInstanceEntityToCopy.getCaseInstanceId())
            .stagePlanItemInstance(stagePlanItem)
            .tenantId(planItemInstanceEntityToCopy.getTenantId())
            .localVariables(localVariables)
            .addToParent(addToParent)
            .silentNameExpressionEvaluation(silentNameExpressionEvaluation)
            .create();

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

    public void markAsNoop() {
        isNoop = true;
    }

    public boolean isNoop() {
        return isNoop;
    }

}