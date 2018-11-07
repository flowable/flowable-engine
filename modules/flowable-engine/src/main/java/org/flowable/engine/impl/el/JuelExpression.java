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

import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
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
public class JuelExpression extends org.flowable.common.engine.impl.el.JuelExpression {

    private static final long serialVersionUID = 1L;
    
    protected DelegateInterceptor delegateInterceptor;

    public JuelExpression(ExpressionManager expressionManager, DelegateInterceptor delegateInterceptor, ValueExpression valueExpression, String expressionText) {
        super(expressionManager, valueExpression, expressionText);
        this.delegateInterceptor = delegateInterceptor;
    }
    
    @Override
    protected Object resolveGetValueExpression(ELContext elContext) {
        ExpressionGetInvocation invocation = new ExpressionGetInvocation(valueExpression, elContext);
        delegateInterceptor.handleInvocation(invocation);
        return invocation.getInvocationResult();
    }
    
    @Override
    protected void resolveSetValueExpression(Object value, ELContext elContext) {
        ExpressionSetInvocation invocation = new ExpressionSetInvocation(valueExpression, elContext, value);
        CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(invocation);
    }

}
