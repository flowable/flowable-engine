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

package org.flowable.engine.impl.el;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.javax.el.ELContext;
import org.flowable.engine.common.impl.javax.el.ELException;
import org.flowable.engine.common.impl.javax.el.MethodNotFoundException;
import org.flowable.engine.common.impl.javax.el.PropertyNotFoundException;
import org.flowable.engine.common.impl.javax.el.ValueExpression;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.delegate.invocation.ExpressionGetInvocation;
import org.flowable.engine.impl.delegate.invocation.ExpressionSetInvocation;
import org.flowable.engine.impl.interceptor.DelegateInterceptor;
import org.flowable.engine.impl.util.CommandContextUtil;

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
    protected DelegateInterceptor delegateInterceptor;

    public JuelExpression(ExpressionManager expressionManager, DelegateInterceptor delegateInterceptor, ValueExpression valueExpression, String expressionText) {
        this.valueExpression = valueExpression;
        this.expressionText = expressionText;
        this.expressionManager = expressionManager;
        this.delegateInterceptor = delegateInterceptor;
    }

    public Object getValue(VariableScope variableScope) {
        ELContext elContext = expressionManager.getElContext(variableScope);
        try {
            ExpressionGetInvocation invocation = new ExpressionGetInvocation(valueExpression, elContext);
            delegateInterceptor.handleInvocation(invocation);
            return invocation.getInvocationResult();
            
        } catch (PropertyNotFoundException pnfe) {
            throw new FlowableException("Unknown property used in expression: " + expressionText, pnfe);
        } catch (MethodNotFoundException mnfe) {
            throw new FlowableException("Unknown method used in expression: " + expressionText, mnfe);
        } catch (ELException ele) {
            throw new FlowableException("Error while evaluating expression: " + expressionText, ele);
        } catch (Exception e) {
            throw new FlowableException("Error while evaluating expression: " + expressionText, e);
        }
    }

    public void setValue(Object value, VariableScope variableScope) {
        ELContext elContext = expressionManager.getElContext(variableScope);
        try {
            ExpressionSetInvocation invocation = new ExpressionSetInvocation(valueExpression, elContext, value);
            CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(invocation);
        } catch (Exception e) {
            throw new FlowableException("Error while evaluating expression: " + expressionText, e);
        }
    }

    @Override
    public String toString() {
        if (valueExpression != null) {
            return valueExpression.getExpressionString();
        }
        return super.toString();
    }

    public String getExpressionText() {
        return expressionText;
    }
}
