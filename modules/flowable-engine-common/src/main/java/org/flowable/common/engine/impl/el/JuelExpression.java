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

package org.flowable.common.engine.impl.el;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.MethodNotFoundException;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.common.engine.impl.javax.el.ValueExpression;

/**
 * Expression implementation backed by a JUEL {@link ValueExpression}.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class JuelExpression implements Expression {

    private static final long serialVersionUID = 1L;
    
    protected String expressionText;
    protected ValueExpression valueExpression;
    protected ExpressionManager expressionManager;

    public JuelExpression(ExpressionManager expressionManager, ValueExpression valueExpression, String expressionText) {
        this.valueExpression = valueExpression;
        this.expressionText = expressionText;
        this.expressionManager = expressionManager;
    }
    
    @Override
    public Object getValue(VariableContainer variableContainer) {
        ELContext elContext = expressionManager.getElContext(variableContainer);
        try {
            return resolveGetValueExpression(elContext);
        } catch (PropertyNotFoundException pnfe) {
            throw new FlowableException("Unknown property used in expression: " + expressionText, pnfe);
        } catch (MethodNotFoundException mnfe) {
            throw new FlowableException("Unknown method used in expression: " + expressionText, mnfe);
        } catch (Exception e) {
            throw new FlowableException("Error while evaluating expression: " + expressionText, e);
        }
    }

    protected Object resolveGetValueExpression(ELContext elContext) {
        return valueExpression.getValue(elContext);
    }

    @Override
    public void setValue(Object value, VariableContainer variableContainer) {
        ELContext elContext = expressionManager.getElContext(variableContainer);
        try {
            resolveSetValueExpression(value, elContext);
        } catch (Exception e) {
            throw new FlowableException("Error while evaluating expression: " + expressionText, e);
        }
    }

    protected void resolveSetValueExpression(Object value, ELContext elContext) {
        valueExpression.setValue(elContext, value);
    }

    @Override
    public String toString() {
        if (valueExpression != null) {
            return valueExpression.getExpressionString();
        }
        return super.toString();
    }

    @Override
    public String getExpressionText() {
        return expressionText;
    }

}
