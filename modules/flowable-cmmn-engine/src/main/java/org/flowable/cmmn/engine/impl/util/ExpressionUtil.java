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
import org.flowable.cmmn.model.PlanItemControl;
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

    public static boolean isCompletionNeutralPlanItemInstance( PlanItemInstanceEntity planItemInstanceEntity) {
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

}
