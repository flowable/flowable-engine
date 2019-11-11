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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class ExpressionUtil {

    public static boolean evaluateBooleanExpression(CommandContext commandContext, VariableContainer variableContainer, String condition) {
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

    public static boolean isRequiredPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemControl planItemControl = planItemInstanceEntity.getPlanItem().getItemControl();
        if (planItemControl != null && planItemControl.getRequiredRule() != null) {

            boolean isRequired = true; // Having a required rule means required by default, unless the condition says otherwise
            String requiredCondition = planItemControl.getRequiredRule().getCondition();
            if (StringUtils.isNotEmpty(requiredCondition)) {
                isRequired = evaluateBooleanExpression(commandContext, planItemInstanceEntity, requiredCondition);
            }
            return isRequired;
        }
        return false;
    }

    public static boolean hasRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity != null && planItemInstanceEntity.getPlanItem() != null) {
            return hasRepetitionRule(planItemInstanceEntity.getPlanItem());
        }
        return false;
    }

    public static boolean hasRepetitionRule(PlanItem planItem) {
        return planItem.getItemControl() != null
            && planItem.getItemControl().getRepetitionRule() != null;
    }

    /**
     * Returns true, if: the given plan item instance has a repetition rule at all and if so, if it has a condition witch is satisfied and all in combination
     * with the optional max instance count attribute. If the repetition rule evaluates to true, this normally means that there should be an additional
     * instance of the plan item created.
     *
     * @param commandContext the command context in which this evaluation is taking place
     * @param planItemInstanceEntity the plan item instance entity to test for a repetition rule to evaluate to true
     * @param planItemInstanceContainer the container (usually the parent stage of the plan item instance) to get access to child plan items
     * @return true, if there is a repetition rule of the plan item instance currently evaluating to true with all of its conditions and attributes
     */
    public static boolean evaluateRepetitionRule(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
        PlanItemInstanceContainer planItemInstanceContainer) {
        RepetitionRule repetitionRule = null;
        if (planItemInstanceEntity != null && planItemInstanceEntity.getPlanItem() != null && planItemInstanceEntity.getPlanItem().getItemControl() != null) {
            repetitionRule = planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule();
        }

        if (repetitionRule != null) {
            // we first check, if there is a max instance count set and if so, check, if there are enough active instances available already
            if (repetitionRule.hasLimitedInstanceCount() && repetitionRule.getMaxInstanceCount() <=
                searchNonFinishedEqualPlanItemInstances(planItemInstanceEntity, planItemInstanceContainer).size()) {
                // we found enough non-final plan item instances with the same plan item definition, so no need to create a new one
                return false;
            }

            String repetitionCondition = repetitionRule.getCondition();
            return evaluateRepetitionRule(commandContext, planItemInstanceEntity, repetitionCondition);
        }
        return false;
    }

    /**
     * Searches for non-finished plan item instances within the given container to be of the same plan item as the given instance.
     *
     * @param planItemInstanceEntity the plan item instance to search for instances of the same plan item within the container
     * @param planItemInstanceContainer the container to search for child plan item instances of the same plan item
     * @return the list of equal plan item instances, might be empty, but never null
     */
    public static List<PlanItemInstance> searchNonFinishedEqualPlanItemInstances(PlanItemInstanceEntity planItemInstanceEntity,
        PlanItemInstanceContainer planItemInstanceContainer) {

        if (planItemInstanceContainer != null && planItemInstanceContainer.getChildPlanItemInstances() != null) {
            return planItemInstanceContainer.getChildPlanItemInstances()
                .stream()
                .filter(pi -> planItemInstanceEntity.getPlanItem().getId().equals(pi.getPlanItem().getId()))
                .filter(pi -> !PlanItemInstanceState.isInTerminalState(pi))
                .filter(pi -> !pi.getId().equals(planItemInstanceEntity.getId()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public static boolean evaluateRepetitionRule(CommandContext commandContext, VariableContainer variableContainer, String repetitionCondition) {
        if (StringUtils.isNotEmpty(repetitionCondition)) {
            return ExpressionUtil.evaluateBooleanExpression(commandContext, variableContainer, repetitionCondition);
        } else {
            return true; // no condition set, but a repetition rule defined is assumed to be defaulting to true
        }
    }

    public static boolean isCompletionNeutralPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        return isCompletionNeutralPlanItemInstance(CommandContextUtil.getCommandContext(), planItemInstanceEntity);
    }

    public static boolean isCompletionNeutralPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemControl planItemControl = planItemInstanceEntity.getPlanItem().getItemControl();
        if (planItemControl != null && planItemControl.getCompletionNeutralRule() != null) {

            boolean isCompletionNeutral = true; // Having a required rule means required by default, unless the condition says otherwise
            String condition = planItemControl.getCompletionNeutralRule().getCondition();
            if (StringUtils.isNotEmpty(condition)) {
                isCompletionNeutral = ExpressionUtil.evaluateBooleanExpression(commandContext, planItemInstanceEntity, condition);
            }
            return isCompletionNeutral;
        }
        return false;
    }

    /**
     * Returns true, if the given stage has an auto-complete condition which evaluates to true or if not, is in auto-complete mode permanently.
     *
     * @param commandContext the command context in which the method is invoked
     * @param variableContainer the variable container (most likely the plan item representing the stage or case plan model) used to evaluate the condition against
     * @param stage the stage model object to get its auto-complete condition from
     * @return true, if the stage is in auto-complete, either statically or if the condition evaluates to true
     */
    public static boolean evaluateAutoComplete(CommandContext commandContext, VariableContainer variableContainer, Stage stage) {
        if (StringUtils.isNotEmpty(stage.getAutoCompleteCondition())) {
            return evaluateBooleanExpression(commandContext, variableContainer, stage.getAutoCompleteCondition());
        }
        return stage.isAutoComplete();
    }
}
