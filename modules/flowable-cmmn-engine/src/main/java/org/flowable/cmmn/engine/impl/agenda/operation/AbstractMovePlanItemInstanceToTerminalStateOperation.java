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

import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.listener.PlanItemLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Operation that moves a given {@link org.flowable.cmmn.api.runtime.PlanItemInstance} to a terminal state (completed, terminated or failed).
 *
 * @author Joram Barrez
 */
public abstract class AbstractMovePlanItemInstanceToTerminalStateOperation extends AbstractChangePlanItemInstanceStateOperation {

    public AbstractMovePlanItemInstanceToTerminalStateOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        String originalState = planItemInstanceEntity.getState();

        super.run();

        String plannedNewState = getNewState();
        
        if (isRepeatingOnDelete(originalState, plannedNewState) && !isWaitingForRepetitionPlanItemInstanceExists(planItemInstanceEntity)) {

            // Create new repeating instance
            PlanItemInstanceEntity newPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, true);

            if (planItemInstanceEntity.getPlanItem() != null && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof EventListener) {
                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(newPlanItemInstanceEntity);

            } else {

                String oldState = newPlanItemInstanceEntity.getState();
                String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
                newPlanItemInstanceEntity.setState(newState);
                PlanItemLifeCycleListenerUtil.callLifecycleListeners(commandContext, newPlanItemInstanceEntity, oldState, newState);

                // Plan item creation "for Repetition"
                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(newPlanItemInstanceEntity);

                // Plan item doesn't have entry criteria (checked in the if condition) and immediately goes to ACTIVE
                if (hasRepetitionRuleAndNoEntryCriteria(planItemInstanceEntity.getPlanItem())) {
                    CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(newPlanItemInstanceEntity, null);
                }
            }
        }
        
        removeSentryRelatedData();
    }

    /**
     * Implementing classes should be aware that unlike extending from AbstractChangePlanItemInstanceStateOperation, this
     * method will be executed just before the deleting the entity
     */
    @Override
    protected abstract void internalExecute();

    protected boolean isRepeatingOnDelete(String originalState, String newState) {
        
        // If there are no entry criteria and the repetition rule evaluates to true: a new instance needs to be created.

        CaseInstanceEntity caseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
        if (CaseInstanceState.isInTerminalState(caseInstance)) {
            return false;
        }
        
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (isEvaluateRepetitionRule() && hasRepetitionRuleAndNoEntryCriteria(planItem)) {
            return ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity, planItemInstanceEntity.getStagePlanItemInstanceEntity());
        }

        // If the plan item instance is in AVAILABLE (and the new state is terminated), and it's repeatable and it gets terminated
        // this means it has never moved away from available.
        // This means there never was a wait_for_repetition instance created (because the plan item instance
        // never goes back to available but to wait_for_repetition).
        // In this specific case, we need to create the wait_for_repetition for future repetitions
        if (PlanItemInstanceState.AVAILABLE.equals(originalState)
                && PlanItemInstanceState.TERMINATED.equals(newState)
                && hasRepetitionRuleEntryCriteria(planItem)
                && !hasRepetitionOnCollection(planItem)
                && isWithoutStageOrParentIsNotTerminated(planItemInstanceEntity)) { // only when the parent is not yet terminated, a new instance should be created
            return true; // the repetition rule doesn't matter, as it can happen on any entry condition that becomes true
        }

        return false;
    }

    protected boolean isWithoutStageOrParentIsNotTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        return planItemInstanceEntity.getStagePlanItemInstanceEntity() == null
                || !PlanItemInstanceState.isInTerminalState(planItemInstanceEntity.getStagePlanItemInstanceEntity());
    }

    /**
     * Returns true, if the given plan item has a repetition rule, but no entry criteria to be satisfied and no collection based repetition. A collection
     * based repetition is similar to entry criteria as it needs to be available in order to repeat the plan item.
     *
     * @param planItem the plan item to test
     * @return true, if the plan item has a repetition rule without any conditions like entry criteria or a collection to be based on for repetition
     */
    protected boolean hasRepetitionRuleAndNoEntryCriteria(PlanItem planItem) {
        return planItem != null
            && planItem.getEntryCriteria().isEmpty()
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null
            && !planItem.getItemControl().getRepetitionRule().hasCollectionVariable();
    }

    protected boolean hasRepetitionOnCollection(PlanItem planItem) {
        return planItem != null
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null
            && planItem.getItemControl().getRepetitionRule().hasCollectionVariable();
    }

    protected boolean hasRepetitionRuleEntryCriteria(PlanItem planItem) {
        return planItem != null
            && !planItem.getEntryCriteria().isEmpty()
            && planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null;
    }

    protected boolean isWaitingForRepetitionPlanItemInstanceExists(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();
        if (stagePlanItemInstanceEntity != null) {
            List<PlanItemInstanceEntity> childPlanItemInstances = stagePlanItemInstanceEntity.getChildPlanItemInstances();
            if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
                return childPlanItemInstances.stream()
                    .anyMatch(p -> Objects.equals(p.getPlanItem().getId(), planItemInstanceEntity.getPlanItem().getId())
                        && PlanItemInstanceState.WAITING_FOR_REPETITION.equals(p.getState()));
            }
        }
        return false;
    }

    protected void completeChildPlanItemInstances() {
        completeChildPlanItemInstances(null);
    }

    protected void completeChildPlanItemInstances(String exitCriterionId) {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildPlanItemInstances()) {
            if (StateTransition.isPossible(child, PlanItemTransition.COMPLETE)) {
                CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(child);
            }
        }
    }
    
    protected void exitChildPlanItemInstances() {
        exitChildPlanItemInstances(null);
    }

    protected void exitChildPlanItemInstances(String exitCriterionId) {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildPlanItemInstances()) {
            if (StateTransition.isPossible(child, PlanItemTransition.EXIT)) {
                // don't propagate the exit event type and exit type to child plan items, it only has an impact where it was set using the exit sentry
                CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(child, exitCriterionId, null, null);
            }
        }
    }
    
    protected abstract boolean isEvaluateRepetitionRule();
    
}
