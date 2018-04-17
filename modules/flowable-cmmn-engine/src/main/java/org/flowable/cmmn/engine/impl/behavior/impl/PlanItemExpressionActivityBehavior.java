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

package org.flowable.cmmn.engine.impl.behavior.impl;

import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * ActivityBehavior that evaluates an expression when executed. Optionally, it sets the result of the expression as a variable on the execution.
 *
 * @author Tijs Rademakers
 */
public class PlanItemExpressionActivityBehavior extends CoreCmmnActivityBehavior {

    protected String expression;
    protected String resultVariable;

    public PlanItemExpressionActivityBehavior(String expression, String resultVariable) {
        this.expression = expression;
        this.resultVariable = resultVariable;
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        Object value = null;
        try {
            Expression expressionObject = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager().createExpression(expression);
            value = expressionObject.getValue(planItemInstanceEntity);
            if (resultVariable != null) {
                planItemInstanceEntity.setVariable(resultVariable, value);
            }

            CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation(planItemInstanceEntity);
            
        } catch (Exception exc) {
            throw new FlowableException(exc.getMessage(), exc);
        }
    }
}
