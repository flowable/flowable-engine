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
package org.flowable.cmmn.engine.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.agenda.PlanItemEvaluationResult;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.impl.interceptor.CommandContext;

public class PlanItemInstanceUtil {
    
    public static PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy,
            boolean addToParent, boolean silentNameExpressionEvaluation) {
        
        return copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntityToCopy, null, addToParent, silentNameExpressionEvaluation);
    }

    public static PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy,
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
    
    /**
     * Evaluates an optional repetition rule on the given plan item and handles it. This might also include handling of a repetition condition or repetition
     * based on a collection variable with optional local item and item index variables to be set on the newly created plan item instances for repetition.
     *
     * @param planItemInstanceEntity the plan item instance to test for a repetition rule
     * @param satisfiedEntryCriterion the optional, satisfied entry criterion activating the plan item, might be null
     * @param planItemInstanceContainer the parent container of the given plan item
     * @param evaluationResult the evaluation result used to collect information during the evaluation of a list of plan items, will be modified inside this
     *          method to reflect gained information about further evaluation as well as any newly created plan item instances for repetition
     * @return true, if the plan item must be activated, false otherwise
     */
    public static boolean evaluateRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity, Criterion satisfiedEntryCriterion,
            PlanItemInstanceContainer planItemInstanceContainer, PlanItemEvaluationResult evaluationResult, CommandContext commandContext) {

        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        boolean activatePlanItemInstance = true;

        // check for a repetition rule on the plan item
        if (ExpressionUtil.hasRepetitionRule(planItemInstanceEntity)) {
            boolean noEntryCriteria = planItem.getEntryCriteria().isEmpty();

            // first check, if we run on a collection variable for repetition and if so, we ignore the max instance count and any other repetition
            // condition and just use the collection to create plan item instances accordingly
            if (ExpressionUtil.hasRepetitionOnCollection(planItemInstanceEntity)) {
                // the plan item should be repeated based on a collection variable
                // evaluate the variable content and check, if we need to start creating instances accordingly
                Iterable<Object> collection = ExpressionUtil.evaluateRepetitionCollectionVariableValue(commandContext, planItemInstanceEntity);

                // if the collection is null (meaning it is not yet available) and we don't have any on-part criteria (e.g an on-part or even combined with
                // an if-part), we don't handle the repetition yet, but wait for its collection to become available
                // but if we have an on-part, we always handle the collection, even if it is null or empty
                if (collection == null && !ExpressionUtil.hasOnParts(planItem)) {
                    // keep this plan item in its current state and don't activate it or handle the repetition collection yet as it is not available yet
                    activatePlanItemInstance = false;
                    
                } else {

                    if (collection != null) {
                        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
                        int index = 0;
                        for (Object item : collection) {
                            // create and activate a new plan item instance for each item in the collection
                            PlanItemInstanceEntity childPlanItemInstanceEntity = createPlanItemInstanceDuplicateForCollectionRepetition(
                                repetitionRule, planItemInstanceEntity, null, item, index++, commandContext);

                            evaluationResult.addChildPlanItemInstance(childPlanItemInstanceEntity);
                        }
                    }

                    // we handled the collection, now we need to make sure that evaluation does not trigger again as it might get evaluated again
                    // before it is terminated or made available again, so we remove the sentry related data of the plan item
                    CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).deleteSentryRelatedData(planItemInstanceEntity.getId());

                    // don't activate this plan item instance, but keep it in available or waiting for repetition state for the next on-part triggering,
                    // if there is an on-part, otherwise we will directly terminate it without having activated it, it was only used to wait in available
                    // state until all criteria was satisfied, including having the collection variable
                    activatePlanItemInstance = false;

                    // if there is an on-part, we keep the current plan item instance for further triggering the on-part and evaluating the collection again
                    // otherwise we terminate the plan item
                    if (!ExpressionUtil.hasOnParts(planItem)) {
                        // if there is no on-part, we don't need this plan item instance anymore, so terminate it
                        CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
                    }
                }
                
            } else if (!noEntryCriteria) {
                // check the plan item to be repeating by evaluating its repetition rule
                if (ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity, planItemInstanceContainer)) {
                    // create a new duplicated plan item instance in waiting for repetition as this one is becoming active
                    evaluationResult.addChildPlanItemInstance(createPlanItemInstanceDuplicateForRepetition(planItemInstanceEntity, commandContext));
                } else {
                    // the repetition rule does not evaluate to true, so we keep this instance in its current state and don't activate it
                    activatePlanItemInstance = false;
                }
            }
        }

        return activatePlanItemInstance;
    }
    
    public static PlanItemInstanceEntity createPlanItemInstanceDuplicateForRepetition(PlanItemInstanceEntity planItemInstanceEntity, CommandContext commandContext) {
        PlanItemInstanceEntity childPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false, false);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
        childPlanItemInstanceEntity.setState(newState);
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, childPlanItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);
        return childPlanItemInstanceEntity;
    }
    
    public static int getRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        Integer counter = (Integer) repeatingPlanItemInstanceEntity.getVariableLocal(getCounterVariable(repeatingPlanItemInstanceEntity));
        if (counter == null) {
            return 0;
        } else {
            return counter.intValue();
        }
    }

    public static String getCounterVariable(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        String repetitionCounterVariableName = repeatingPlanItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getRepetitionCounterVariableName();
        return repetitionCounterVariableName;
    }

    protected static PlanItemInstanceEntity createPlanItemInstanceDuplicateForCollectionRepetition(RepetitionRule repetitionRule,
            PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId, Object item, int index, CommandContext commandContext) {

        // check, if we need to set local variables as the item or item index
        Map<String, Object> localVariables = new HashMap<>(2);
        if (repetitionRule.hasElementVariable()) {
            localVariables.put(repetitionRule.getElementVariableName(), item);
        }
        if (repetitionRule.hasElementIndexVariable()) {
            localVariables.put(repetitionRule.getElementIndexVariableName(), index);
        }

        PlanItemInstanceEntity childPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, localVariables, false, false);

        // record the plan item being created based on the collection, so it gets synchronized to the history as well
        CommandContextUtil.getAgenda(commandContext).planCreateRepeatedPlanItemInstanceOperation(childPlanItemInstanceEntity);

        // The repetition counter is 1 based
        childPlanItemInstanceEntity.setVariableLocal(PlanItemInstanceUtil.getCounterVariable(childPlanItemInstanceEntity), index + 1);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.ACTIVE;
        childPlanItemInstanceEntity.setState(newState);
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(childPlanItemInstanceEntity, entryCriterionId);
        return childPlanItemInstanceEntity;
    }
}