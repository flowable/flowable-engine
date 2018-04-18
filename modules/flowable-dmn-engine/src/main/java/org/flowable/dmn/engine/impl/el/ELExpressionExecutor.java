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
package org.flowable.dmn.engine.impl.el;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.dmn.engine.FlowableDmnExpressionException;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.UnaryTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class ELExpressionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ELExpressionExecutor.class);

    public static Boolean executeInputExpression(InputClause inputClause, UnaryTests inputEntry, ExpressionManager expressionManager, ELExecutionContext executionContext) {
        if (inputClause == null) {
            throw new IllegalArgumentException("input clause is required");
        }
        if (inputClause.getInputExpression() == null) {
            throw new IllegalArgumentException("input expression is required");
        }
        if (inputEntry == null) {
            throw new IllegalArgumentException("input entry is required");
        }
        if (executionContext == null) {
            throw new IllegalArgumentException("execution context is required");
        }
        
        String inputExpression = inputClause.getInputExpression().getText();
        executionContext.checkExecutionContext(inputExpression);
        
        // pre parse expression
        String parsedExpression = ELInputEntryExpressionPreParser.parse(inputEntry.getText(), inputExpression, inputClause.getInputExpression().getTypeRef());

        Expression expression = expressionManager.createExpression(parsedExpression);
        RuleExpressionCondition condition = new RuleExpressionCondition(expression);
        
        try {
            return condition.evaluate(executionContext.getStackVariables());
        } catch (Exception ex) {
            LOGGER.warn("Error while executing input entry: {}", parsedExpression, ex);
            throw new FlowableDmnExpressionException("error while executing input entry", parsedExpression, ex);
        }
    }

    public static Object executeOutputExpression(OutputClause outputClause, LiteralExpression outputEntry, ExpressionManager expressionManager, ELExecutionContext executionContext) {
        if (outputClause == null) {
            throw new IllegalArgumentException("output clause is required");
        }
        if (outputEntry == null) {
            throw new IllegalArgumentException("output entry is required");
        }
        if (executionContext == null) {
            throw new IllegalArgumentException("execution context is required");
        }
        
        String parsedExpression = ELOutputEntryExpressionPreParser.parse(outputEntry.getText());
        
        Expression expression = expressionManager.createExpression(parsedExpression);
        RuleExpressionOutput outputExpression = new RuleExpressionOutput(expression);

        try {
            return outputExpression.getValue(executionContext.getStackVariables());
        } catch (Exception ex) {
            LOGGER.warn("Error while executing output entry: {}", outputEntry.getText(), ex);
            throw new FlowableDmnExpressionException("error while executing output entry", outputEntry.getText(), ex);
        }
    }
}
