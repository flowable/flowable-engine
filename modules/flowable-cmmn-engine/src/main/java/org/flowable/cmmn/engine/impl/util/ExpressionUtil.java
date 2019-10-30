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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
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

    public static boolean evaluateRepetitionRule(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (hasRepetitionRule(planItemInstanceEntity)) {
            String repetitionCondition = planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getCondition();
            return evaluateRepetitionRule(commandContext, planItemInstanceEntity, repetitionCondition);
        }
        return false;
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
