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

import static org.flowable.common.engine.impl.util.ExceptionUtil.sneakyThrow;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * ActivityBehavior that evaluates an expression when executed. Optionally, it sets the result of the expression as a variable on the execution.
 *
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class PlanItemExpressionActivityBehavior extends CoreCmmnActivityBehavior {

    protected String expression;
    protected String resultVariable;
    protected boolean storeResultVariableAsTransient;

    public PlanItemExpressionActivityBehavior(String expression, String resultVariable, boolean storeResultVariableAsTransient) {
        this.expression = expression;
        this.resultVariable = resultVariable;
        this.storeResultVariableAsTransient = storeResultVariableAsTransient;
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        Object value = null;
        Expression expressionObject = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager().createExpression(expression);
        value = expressionObject.getValue(planItemInstanceEntity);
        if (value instanceof CompletableFuture) {
            CommandContextUtil.getAgenda(commandContext)
                    .planFutureOperation((CompletableFuture<Object>) value, new FutureExpressionCompleteAction(planItemInstanceEntity));
        } else {
            complete(value, planItemInstanceEntity);
        }

    }

    protected void complete(Object value, PlanItemInstanceEntity planItemInstanceEntity) {
        if (resultVariable != null) {
            if (storeResultVariableAsTransient) {
                planItemInstanceEntity.setTransientVariable(resultVariable, value);
            } else {
                planItemInstanceEntity.setVariable(resultVariable, value);
            }
        }

        CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }

    protected class FutureExpressionCompleteAction implements BiConsumer<Object, Throwable> {

        protected final PlanItemInstanceEntity planItemInstanceEntity;

        public FutureExpressionCompleteAction(PlanItemInstanceEntity planItemInstanceEntity) {
            this.planItemInstanceEntity = planItemInstanceEntity;
        }

        @Override
        public void accept(Object value, Throwable throwable) {
            if (throwable == null) {
                complete(value, planItemInstanceEntity);
            } else {
                sneakyThrow(throwable);
            }
        }
    }
}
