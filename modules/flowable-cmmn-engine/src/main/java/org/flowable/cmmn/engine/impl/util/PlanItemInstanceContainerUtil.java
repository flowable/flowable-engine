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

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE_STATES;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.END_STATES;
import static org.flowable.cmmn.model.ParentCompletionRule.IGNORE;
import static org.flowable.cmmn.model.ParentCompletionRule.IGNORE_AFTER_FIRST_COMPLETION;
import static org.flowable.cmmn.model.ParentCompletionRule.IGNORE_AFTER_FIRST_COMPLETION_IF_AVAILABLE_OR_ENABLED;
import static org.flowable.cmmn.model.ParentCompletionRule.IGNORE_IF_AVAILABLE;
import static org.flowable.cmmn.model.ParentCompletionRule.IGNORE_IF_AVAILABLE_OR_ENABLED;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Utility methods around a plan item container (most likely a stage or case plan model).
 *
 * @author Micha Kiener
 * @author Joram Barrez
 */
public class PlanItemInstanceContainerUtil {

    public static CompletionEvaluationResult shouldPlanItemContainerComplete(PlanItemInstanceContainer planItemInstanceContainer, boolean containerIsAutocomplete) {
        return shouldPlanItemContainerComplete(CommandContextUtil.getCommandContext(), planItemInstanceContainer, containerIsAutocomplete);
    }

    public static CompletionEvaluationResult shouldPlanItemContainerComplete(CommandContext commandContext, PlanItemInstanceContainer planItemInstanceContainer,
        boolean containerIsAutocomplete) {
        return shouldPlanItemContainerComplete(commandContext, planItemInstanceContainer, null, containerIsAutocomplete);
    }

    public static CompletionEvaluationResult shouldPlanItemContainerComplete(PlanItemInstanceContainer planItemInstanceContainer, Collection<String> planItemInstanceIdsToIgnore,
        boolean containerIsAutocomplete) {
        return shouldPlanItemContainerComplete(CommandContextUtil.getCommandContext(), planItemInstanceContainer, planItemInstanceIdsToIgnore, containerIsAutocomplete);
    }

    /**
     * Method to check a plan item container (most likely a stage or case plan model) if it should be completed according its child plan item states and their
     * combined behavior rules (e.g. repetition, if-part, manual activation, required, etc). The method returns two results: whether the plan item itself
     * is completable, which is the case when there is no more active or required work to be done, but it might still have optional work to do and a second
     * one which represents whether the plan item should in fact be completed (the difference being the state of the autocomplete mode, where if turned off,
     * only returns true, if there is no more work to be done and no more further options to activate some optional work).
     *
     * @param commandContext the command context in which the method is to be executed
     * @param planItemInstanceContainer the plan item container to evaluate is completable state (most likely a stage or case plan model)
     * @param planItemInstanceIdsToIgnore an optional list of plan item ids to be ignored for evaluating the parent completing state, might be null or empty
     * @param containerIsAutocomplete true, if the plan item container is in autocomplete mode, false, if not
     * @return two flags representing whether the plan item might be completable and whether it should actually be completed
     */
    public static CompletionEvaluationResult shouldPlanItemContainerComplete(CommandContext commandContext, PlanItemInstanceContainer planItemInstanceContainer,
        Collection<String> planItemInstanceIdsToIgnore, boolean containerIsAutocomplete) {
        // this might become false as we go through the plan items
        boolean shouldBeCompleted = true;

        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity planItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {

                // check, if the plan item should be ignored as its id is part of the list of plan items to be ignored
                if (planItemInstanceIdsToIgnore == null || !planItemInstanceIdsToIgnore.contains(planItemInstance.getId())) {
                    Boolean alreadyCompleted = null;

                    // continue, if the plan item is in one of the end states or it is configured to be ignored for parent completion
                    if (END_STATES.contains(planItemInstance.getState()) || isParentCompletionRuleForPlanItemEqualToType(planItemInstance, IGNORE)) {
                        continue;
                    }

                    // if the plan item is active and not to be ignored, we can directly stop to look any further as it prevents the parent from being completed
                    if (ACTIVE_STATES.contains(planItemInstance.getState())) {
                        // if the plan item is active, but was already completed and should be ignored after first completion, we can skip it for further investigation
                        alreadyCompleted = isPlanItemAlreadyCompleted(commandContext, planItemInstance);
                        if (shouldIgnorePlanItemForCompletion(commandContext, planItemInstance, alreadyCompleted)) {
                            continue;
                        }
                        return new CompletionEvaluationResult(false, false, planItemInstance);
                    }

                    // if the plan item is required and not yet in an end state or active, we need to check the special parent completion rule to determine
                    // if we need to prevent completion
                    if (ExpressionUtil.isRequiredPlanItemInstance(commandContext, planItemInstance)) {
                        // if the plan item is repeatable, we need to further investigate, as a required plan item might have a special rule set to be ignored
                        // after first completion
                        if (ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstance, (PlanItemInstanceContainer) null)) {
                            alreadyCompleted = isPlanItemAlreadyCompleted(commandContext, planItemInstance);
                            if (shouldIgnorePlanItemForCompletion(commandContext, planItemInstance, alreadyCompleted)) {
                                continue;
                            }
                            if (!alreadyCompleted) {
                                // was never completed before and is required, evaluation can stop here as we found required work still to be done
                                return new CompletionEvaluationResult(false, false, planItemInstance);
                            }
                            // we don't ignore it for completion, but if it was completed before, the parent is still completable (depends then on its autocompletion)
                            shouldBeCompleted = shouldBeCompleted && containerIsAutocomplete;
                        } else {
                            return new CompletionEvaluationResult(false, false, planItemInstance);
                        }
                    }

                    // same thing, if we're not in autocomplete mode, but the parent completion mode of the plan item says to ignore if available or enabled
                    if (isParentCompletionRuleForPlanItemEqualToType(planItemInstance, IGNORE_IF_AVAILABLE_OR_ENABLED) &&
                        (ENABLED.equals(planItemInstance.getState()) || AVAILABLE.equals(planItemInstance.getState()))) {
                        continue;
                    }

                    // same for the available state
                    if ((isParentCompletionRuleForPlanItemEqualToType(planItemInstance, IGNORE_IF_AVAILABLE) || ExpressionUtil.isCompletionNeutralPlanItemInstance(commandContext, planItemInstance))
                        && AVAILABLE.equals(planItemInstance.getState())) {
                        continue;
                    }

                    // special care if the plan item is repeatable
                    if (ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstance, (PlanItemInstanceContainer) null)) {
                        if (alreadyCompleted == null) {
                            alreadyCompleted = isPlanItemAlreadyCompleted(commandContext, planItemInstance);
                        }
                        if (shouldIgnorePlanItemForCompletion(commandContext, planItemInstance, alreadyCompleted)) {
                            continue;
                        }
                    }

                    // if the plan item is in available or enabled state, we ignore it, if we look at it with autocompletion in mind
                    if (AVAILABLE.equals(planItemInstance.getState()) || ENABLED.equals(planItemInstance.getState())) {
                        shouldBeCompleted = shouldBeCompleted && containerIsAutocomplete;
                    }

                    // recursively invoke this method again with the current child plan item to check its children
                    if (planItemInstance.getChildPlanItemInstances() != null) {

                        boolean childContainerIsAutocomplete = false;
                        if (PlanItemDefinitionType.STAGE.equals(planItemInstance.getPlanItemDefinitionType())) {
                            Stage stage = (Stage) planItemInstance.getPlanItem().getPlanItemDefinition();

                            childContainerIsAutocomplete = ExpressionUtil.evaluateAutoComplete(commandContext, planItemInstance, stage);
                        }

                        CompletionEvaluationResult childPlanItemInstanceCompletionEvaluationResult =
                            shouldPlanItemContainerComplete(commandContext, planItemInstance, null, childContainerIsAutocomplete);
                        if (!childPlanItemInstanceCompletionEvaluationResult.isCompletable) {
                            return childPlanItemInstanceCompletionEvaluationResult;
                        }
                        shouldBeCompleted = shouldBeCompleted && childPlanItemInstanceCompletionEvaluationResult.shouldBeCompleted;
                    }
                }
            }
        }
        return new CompletionEvaluationResult(true, shouldBeCompleted, null);
    }

    /**
     * Evaluates the plan item for being ignored for completion, if it was at least completed once before.
     *
     * @param commandContext the command context under which this method is invoked
     * @param planItemInstance the plan item to evaluate its completed state
     * @param alreadyCompleted true, if the plan item has been completed before already
     * @return true, if the plan item should be ignored for completion according the parent completion rule and if it was completed before
     */
    public static boolean shouldIgnorePlanItemForCompletion(CommandContext commandContext, PlanItemInstanceEntity planItemInstance, boolean alreadyCompleted) {
        // a required plan item with repetition might need special treatment
        if (isParentCompletionRuleForPlanItemEqualToType(planItemInstance, IGNORE_AFTER_FIRST_COMPLETION)) {
            // we're not (yet) in active state here and have repetition, so we need to check, whether that plan item was completed at least
            // once already in the past
            return alreadyCompleted;
        } else if (isParentCompletionRuleForPlanItemEqualToType(planItemInstance, IGNORE_AFTER_FIRST_COMPLETION_IF_AVAILABLE_OR_ENABLED) &&
            (AVAILABLE.equals(planItemInstance.getState()) || ENABLED.equals(planItemInstance.getState()))) {
            return alreadyCompleted;
        }
        return false;
    }

    /**
     * Searches for completed plan items with the same plan item id as the given one.
     *
     * @param commandContext the command context under which this method is invoked
     * @param planItemInstance the plan item instance to search for already completed instances
     * @return true, if there is at least one completed instance found, false otherwise
     */
    public static boolean isPlanItemAlreadyCompleted(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        List<PlanItemInstanceEntity> planItemInstances = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
            .findByCaseInstanceIdAndPlanItemId(planItemInstance.getCaseInstanceId(), planItemInstance.getPlanItem().getId());
        if (planItemInstances != null && planItemInstances.size() > 0) {
            for (PlanItemInstanceEntity item : planItemInstances) {
                if (Objects.equals(planItemInstance.getStageInstanceId(), item.getStageInstanceId()) && COMPLETED.equals(item.getState())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks the plan items parent completion mode to be equal to a given type and returns true if so.
     *
     * @param planItemInstance the plan item to check for a parent completion mode
     * @param parentCompletionRuleType the parent completion type to check against
     * @return true, if there is a parent completion mode set on the plan item equal to the given one
     */
    public static boolean isParentCompletionRuleForPlanItemEqualToType(PlanItemInstanceEntity planItemInstance, String parentCompletionRuleType) {
        if (planItemInstance.getPlanItem().getItemControl() != null && planItemInstance.getPlanItem().getItemControl().getParentCompletionRule() != null) {
            ParentCompletionRule parentCompletionRule = planItemInstance.getPlanItem().getItemControl().getParentCompletionRule();
            if (parentCompletionRuleType.equals(parentCompletionRule.getType())) {
                return true;
            }
        }
        return false;
    }

}
